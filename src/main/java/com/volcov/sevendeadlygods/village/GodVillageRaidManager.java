package com.volcov.sevendeadlygods.village;

import com.volcov.sevendeadlygods.advancement.ModAdvancements;
import com.volcov.sevendeadlygods.block.SinAltarBlockEntity;
import com.volcov.sevendeadlygods.config.ModConfig;
import com.volcov.sevendeadlygods.entity.GodAcolyteEntity;
import com.volcov.sevendeadlygods.entity.GodAvatarEntity;
import com.volcov.sevendeadlygods.favor.SinFavorManager;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.intervention.DivineInterventionScheduler;
import com.volcov.sevendeadlygods.registry.ModBlocks;
import com.volcov.sevendeadlygods.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de Asaltos Autónomos y Guerras Santas entre las facciones de los 7 Dioses del Pecado.
 * Evalúa cada minuto las aldeas cargadas, despliega escuadrones invasores de acólitos y resuelve
 * la victoria defensiva o la conquista/arrasamiento del templo.
 */
public class GodVillageRaidManager {
    private static final int CHECK_INTERVAL_TICKS = 1200; // 1 minuto (60s)
    private static final int ACTIVE_RAID_TICK_INTERVAL = 40; // Cada 2 segundos para raids en curso
    public static final long RAID_COOLDOWN_TICKS = 72000L; // 3 días de Minecraft
    private static final long MAX_RAID_DURATION_TICKS = 6000L; // 5 minutos máximo por asalto

    public record ActiveRaidContext(
            UUID villageId,
            SinGod defenderGod,
            SinGod attackerGod,
            long activeTicks,
            boolean pausedByChunkUnload
    ) {
        public ActiveRaidContext withPaused(boolean paused) {
            return new ActiveRaidContext(villageId, defenderGod, attackerGod, activeTicks, paused);
        }

        public ActiveRaidContext withActiveTicks(long ticks) {
            return new ActiveRaidContext(villageId, defenderGod, attackerGod, ticks, pausedByChunkUnload);
        }
    }

    public record RaidGodKey(UUID villageId, SinGod god) {}

    private static final Map<UUID, ActiveRaidContext> ACTIVE_RAIDS = new HashMap<>();
    private static final Map<RaidGodKey, UUID> POSTPONED_WRATH_HUNTS = new ConcurrentHashMap<>();
    private static final Map<SinGod, UUID> POSTPONED_GLOBAL_WRATH_HUNTS = new ConcurrentHashMap<>();

    public static void postponeWrathHunt(@Nullable UUID villageId, SinGod god, UUID huntedPlayerUuid) {
        if (villageId != null) {
            POSTPONED_WRATH_HUNTS.put(new RaidGodKey(villageId, god), huntedPlayerUuid);
        } else {
            POSTPONED_GLOBAL_WRATH_HUNTS.put(god, huntedPlayerUuid);
        }
    }

    public static void postponeWrathHunt(SinGod god, UUID huntedPlayerUuid) {
        postponeWrathHunt(null, god, huntedPlayerUuid);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int tickCount = server.getTickCount();

        if (tickCount % ACTIVE_RAID_TICK_INTERVAL == 0) {
            for (ServerLevel level : server.getAllLevels()) {
                tickActiveRaids(server, level);
                tickDesolatedVillages(server, level);
            }
        }

        if (tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            GodVillageSavedData data = GodVillageSavedData.get(level);
            long gameTime = level.getGameTime();
            List<GodVillageSavedData.GodVillageRecord> snapshot = new ArrayList<>(data.getAllVillages());

            for (GodVillageSavedData.GodVillageRecord village : snapshot) {
                checkDesolationState(server, level, data, village, gameTime);
                GodVillageSavedData.GodVillageRecord currentVillage = data.getVillageById(village.id()).orElse(village);
                long raidCooldown = ModConfig.VILLAGE_RAID_COOLDOWN_TICKS.get();
                if (currentVillage.state() == GodVillageState.CONSAGRADA
                        && (gameTime - currentVillage.lastRaidTick() > raidCooldown)) {
                    BlockPos altarPos = currentVillage.altarPos();
                    if (level.hasNearbyAlivePlayer(altarPos.getX() + 0.5D, altarPos.getY() + 0.5D, altarPos.getZ() + 0.5D, 80.0D)) {
                        if (level.getRandom().nextFloat() < 0.15F) {
                            startAutonomousRaid(server, level, data, currentVillage);
                        }
                    }
                }
            }
        }
    }

    public static void startAutonomousRaid(
            MinecraftServer server,
            ServerLevel level,
            GodVillageSavedData data,
            GodVillageSavedData.GodVillageRecord village
    ) {
        SinGod defender = village.patronGod();
        SinGod attacker = pickRivalGod(defender, level.getRandom());
        startRaidOnVillage(server, level, data, village, attacker, null);
    }

    public static void startRaidOnVillage(
            MinecraftServer server,
            ServerLevel level,
            GodVillageSavedData data,
            GodVillageSavedData.GodVillageRecord village,
            SinGod attacker,
            @Nullable ServerPlayer instigator
    ) {
        SinGod defender = village.patronGod();
        BlockPos altarPos = village.altarPos();
        long gameTime = level.getGameTime();

        // 1. Notificación en el Chat Global
        Component msg = Component.literal("⚔ ¡GUERRA SANTA! ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("Acólitos de ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(attacker.getDisplayName()).withStyle(attacker.getChatColor(), ChatFormatting.BOLD))
                .append(Component.literal(" marchan para saquear el Templo de ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(defender.getDisplayName()).withStyle(defender.getChatColor(), ChatFormatting.BOLD))
                .append(Component.literal(String.format(" en [%d, %d]!", altarPos.getX(), altarPos.getZ()))
                        .withStyle(ChatFormatting.YELLOW));

        server.getPlayerList().broadcastSystemMessage(msg, false);
        level.playSound(null, altarPos, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 3.0F, 0.85F);

        // 2. Spawneo de Escuadrón Invasor (8 acólitos) y refuerzo defensivo si no había centinelas
        ensureMinimumDefenders(level, altarPos, defender, 3);
        List<GodAcolyteEntity> invaders = spawnInvasionForces(level, altarPos, attacker, 8);
        if (instigator != null) {
            for (GodAcolyteEntity invader : invaders) {
                invader.getNavigation().moveTo(altarPos.getX() + 0.5D, altarPos.getY(), altarPos.getZ() + 0.5D, 1.15D);
            }
        }

        // 3. Actualizar estado en persistencia y contexto activo
        GodVillageSavedData.GodVillageRecord updated = village.withRaid(attacker, gameTime);
        data.updateVillage(updated);
        ACTIVE_RAIDS.put(village.id(), new ActiveRaidContext(village.id(), defender, attacker, 0L, false));

        // 4. Evaluar la tirada del 5% de Manifestación Divina del Avatar (Regla de Avatar Único + Prioridad Territorial)
        evaluateAvatarManifestation(server, level, village.id(), altarPos, defender, attacker, false);
    }

    public static void evaluateAvatarManifestation(
            MinecraftServer server,
            ServerLevel level,
            BlockPos altarPos,
            SinGod defenderGod,
            SinGod attackerGod,
            boolean forceManifest
    ) {
        evaluateAvatarManifestation(server, level, null, altarPos, defenderGod, attackerGod, forceManifest);
    }

    public static void evaluateAvatarManifestation(
            MinecraftServer server,
            ServerLevel level,
            @Nullable UUID villageId,
            BlockPos altarPos,
            SinGod defenderGod,
            SinGod attackerGod,
            boolean forceManifest
    ) {
        // 5% de probabilidad estricta (configurable en ModConfig, salvo forzado por comando de prueba)
        double avatarChance = ModConfig.VILLAGE_RAID_AVATAR_CHANCE.get();
        if (!forceManifest && level.getRandom().nextDouble() >= avatarChance) {
            return;
        }

        // 15% dentro del 5%: El Gran Choque de Dioses (descienden ambos Avatares)
        if (level.getRandom().nextFloat() < 0.15F) {
            manifestSingleRaidAvatar(server, level, villageId, altarPos.offset(-4, 0, 0), defenderGod);
            manifestSingleRaidAvatar(server, level, villageId, altarPos.offset(4, 0, 0), attackerGod);

            Component clashMsg = Component.literal("⚡ ¡CHOQUE DE DIOSES! ")
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                    .append(Component.literal("¡Los cielos se desgarran sobre la aldea! Los Avatares de ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(defenderGod.getDisplayName()).withStyle(defenderGod.getChatColor(), ChatFormatting.BOLD))
                    .append(Component.literal(" y ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(attackerGod.getDisplayName()).withStyle(attackerGod.getChatColor(), ChatFormatting.BOLD))
                    .append(Component.literal(" han descendido en persona para batirse en duelo!").withStyle(ChatFormatting.WHITE));
            server.getPlayerList().broadcastSystemMessage(clashMsg, false);

            for (ServerPlayer player : level.players()) {
                if (player.blockPosition().closerThan(altarPos, 96.0D)) {
                    ModAdvancements.award(player, "secret_clash_of_the_gods");
                }
            }
            return;
        }

        // Elegir quién desciende (50% Defensor, 50% Atacante)
        SinGod appearingGod = level.getRandom().nextBoolean() ? defenderGod : attackerGod;
        manifestSingleRaidAvatar(server, level, villageId, altarPos, appearingGod);

        Component announcement = Component.literal("⚡ ¡MANIFESTACIÓN DIVINA! ")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal("El clamor de la sangre ha desgarrado el velo: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(appearingGod.getDisplayName()).withStyle(appearingGod.getChatColor(), ChatFormatting.BOLD))
                .append(Component.literal(" ha descendido para juzgar la batalla!").withStyle(ChatFormatting.WHITE));

        server.getPlayerList().broadcastSystemMessage(announcement, false);
    }

    public static void manifestSingleRaidAvatar(
            MinecraftServer server,
            ServerLevel level,
            BlockPos altarPos,
            SinGod appearingGod
    ) {
        manifestSingleRaidAvatar(server, level, null, altarPos, appearingGod);
    }

    public static void manifestSingleRaidAvatar(
            MinecraftServer server,
            ServerLevel level,
            @Nullable UUID villageId,
            BlockPos altarPos,
            SinGod appearingGod
    ) {
        // --- REGLA DE AVATAR ÚNICO ---
        GodAvatarEntity existingAvatar = DivineInterventionScheduler.getActiveAvatar(server, appearingGod);
        BlockPos landingPos = new BlockPos(
                altarPos.getX(),
                level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, altarPos.getX(), altarPos.getZ()),
                altarPos.getZ()
        );

        if (existingAvatar != null) {
            // Si el Avatar está en una Caza de Ira, el asedio a la aldea tiene PRIORIDAD ABSOLUTA:
            if (existingAvatar.isWrathHunter() && existingAvatar.getTargetPlayerUuid() != null) {
                UUID huntedPlayerUuid = existingAvatar.getTargetPlayerUuid();
                postponeWrathHunt(villageId, appearingGod, huntedPlayerUuid);

                ServerPlayer huntedPlayer = server.getPlayerList().getPlayer(huntedPlayerUuid);
                if (huntedPlayer != null) {
                    huntedPlayer.sendSystemMessage(
                            appearingGod.getFormattedPrefix().append(
                                    Component.literal("«¡Mortal insolente! Tu juicio queda aplazado: mi templo sagrado clama por mi presencia divina. ¡Considera este día un préstamo de vida!»")
                                            .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC)
                            )
                    );
                }
            }

            ServerLevel oldLevel = (ServerLevel) existingAvatar.level();
            oldLevel.sendParticles(ParticleTypes.PORTAL, existingAvatar.getX(), existingAvatar.getY() + 1.0D, existingAvatar.getZ(), 50, 0.5D, 0.8D, 0.5D, 0.1D);
            float currentHealth = existingAvatar.getHealth();

            if (oldLevel == level) {
                existingAvatar.snapTo(landingPos.getX() + 0.5D, landingPos.getY(), landingPos.getZ() + 0.5D, 0.0F, 0.0F);
                existingAvatar.getNavigation().stop();
                existingAvatar.initializeAvatar(level, appearingGod, null, false);
                existingAvatar.setVillageRaidAvatar(true, altarPos);
                existingAvatar.setHealth(Math.min(currentHealth, existingAvatar.getMaxHealth()));
            } else {
                existingAvatar.discard();
                GodAvatarEntity transferred = ModEntities.GOD_AVATAR.get().create(level, EntitySpawnReason.EVENT);
                if (transferred != null) {
                    transferred.snapTo(landingPos.getX() + 0.5D, landingPos.getY(), landingPos.getZ() + 0.5D, 0.0F, 0.0F);
                    transferred.initializeAvatar(level, appearingGod, null, false);
                    transferred.setVillageRaidAvatar(true, altarPos);
                    transferred.setHealth(Math.min(currentHealth, transferred.getMaxHealth()));
                    level.addFreshEntity(transferred);
                }
            }
        } else {
            GodAvatarEntity avatar = ModEntities.GOD_AVATAR.get().create(level, EntitySpawnReason.EVENT);
            if (avatar != null) {
                avatar.snapTo(landingPos.getX() + 0.5D, landingPos.getY(), landingPos.getZ() + 0.5D, 0.0F, 0.0F);
                avatar.initializeAvatar(level, appearingGod, null, false);
                avatar.setVillageRaidAvatar(true, altarPos);
                level.addFreshEntity(avatar);
            }
        }

        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
        if (bolt != null) {
            bolt.snapTo(landingPos.getX() + 0.5D, landingPos.getY(), landingPos.getZ() + 0.5D);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, landingPos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.0F, 0.8F);
    }

    public static void onVillageRaidEnded(MinecraftServer server, ServerLevel level, SinGod god) {
        onVillageRaidEnded(server, level, null, null, god);
    }

    public static void onVillageRaidEnded(
            MinecraftServer server,
            ServerLevel level,
            @Nullable UUID villageId,
            @Nullable BlockPos altarPos,
            SinGod god
    ) {
        UUID targetUuid = villageId != null ? POSTPONED_WRATH_HUNTS.remove(new RaidGodKey(villageId, god)) : null;
        if (targetUuid == null) {
            targetUuid = POSTPONED_GLOBAL_WRATH_HUNTS.remove(god);
        }
        GodAvatarEntity avatar = DivineInterventionScheduler.getActiveAvatar(server, god);

        if (targetUuid != null) {
            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isAlive() && avatar != null && avatar.isAlive()) {
                if (altarPos == null || altarPos.equals(avatar.getRaidAltarPos())) {
                    avatar.setVillageRaidAvatar(false, null);
                    targetPlayer.sendSystemMessage(
                            god.getFormattedPrefix().append(
                                    Component.literal("«Mi templo sagrado está a salvo. Ahora... volvamos a nuestra deuda pendiente, mortal.»")
                                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                            )
                    );
                    DivineInterventionScheduler.triggerWrathAvatarHunt(targetPlayer, god);
                    return;
                }
            }
        }

        if (avatar != null && avatar.isAlive() && avatar.isVillageRaidAvatar()) {
            if (altarPos == null || altarPos.equals(avatar.getRaidAltarPos())) {
                if (avatar.level() instanceof ServerLevel avatarLevel) {
                    avatarLevel.sendParticles(ParticleTypes.END_ROD, avatar.getX(), avatar.getY() + 1.5D, avatar.getZ(), 60, 0.5D, 1.0D, 0.5D, 0.1D);
                    avatarLevel.playSound(null, avatar.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1.2F, 1.1F);
                }
                avatar.discard();
            }
        }
    }

    private static void tickActiveRaids(MinecraftServer server, ServerLevel level) {
        GodVillageSavedData data = GodVillageSavedData.get(level);
        long gameTime = level.getGameTime();
        List<GodVillageSavedData.GodVillageRecord> snapshot = new ArrayList<>(data.getAllVillages());

        for (GodVillageSavedData.GodVillageRecord village : snapshot) {
            if (village.state() != GodVillageState.EN_ASEDIO) {
                ACTIVE_RAIDS.remove(village.id());
                continue;
            }

            BlockPos altarPos = village.altarPos();
            SinGod restoredAttacker = village.attackerGod() != null
                    ? village.attackerGod()
                    : pickRivalGod(village.patronGod(), level.getRandom());

            ActiveRaidContext ctx = ACTIVE_RAIDS.computeIfAbsent(
                    village.id(),
                    id -> new ActiveRaidContext(id, village.patronGod(), restoredAttacker, village.activeRaidTicks(), false)
            );

            // Protección contra Chunk Unload: pausar evaluación si no hay jugadores en 128 bloques
            if (!level.hasNearbyAlivePlayer(altarPos.getX() + 0.5D, altarPos.getY() + 0.5D, altarPos.getZ() + 0.5D, 128.0D)) {
                if (!ctx.pausedByChunkUnload()) {
                    ACTIVE_RAIDS.put(village.id(), ctx.withPaused(true));
                }
                continue;
            } else if (ctx.pausedByChunkUnload()) {
                ctx = ctx.withPaused(false);
                ACTIVE_RAIDS.put(village.id(), ctx);
            }

            // Excluir el tiempo pausado: acumular ticks activos solo cuando hay jugadores en la aldea
            long currentActiveTicks = ctx.activeTicks() + ACTIVE_RAID_TICK_INTERVAL;
            ctx = ctx.withActiveTicks(currentActiveTicks);
            ACTIVE_RAIDS.put(village.id(), ctx);
            if (currentActiveTicks % 200 == 0) {
                data.updateVillage(village.withActiveRaidTicks(currentActiveTicks));
            }

            AABB searchBox = new AABB(altarPos).inflate(village.radius() + 24.0D);
            List<GodAcolyteEntity> allAcolytes = level.getEntitiesOfClass(GodAcolyteEntity.class, searchBox, a -> a.isAlive());

            int attackersAlive = 0;
            int defendersAlive = 0;
            for (GodAcolyteEntity acolyte : allAcolytes) {
                if (acolyte.getGod() == ctx.attackerGod()) {
                    attackersAlive++;
                    if (acolyte.getTarget() == null && acolyte.getNavigation().isDone()) {
                        acolyte.getNavigation().moveTo(altarPos.getX() + 0.5D, altarPos.getY(), altarPos.getZ() + 0.5D, 1.1D);
                    }
                } else if (acolyte.getGod() == ctx.defenderGod()) {
                    defendersAlive++;
                }
            }

            boolean timedOut = ctx.activeTicks() >= MAX_RAID_DURATION_TICKS;

            if (attackersAlive == 0 || timedOut) {
                resolveDefenderVictory(server, level, data, village, ctx, gameTime);
            } else if (defendersAlive == 0) {
                resolveAttackerConquest(server, level, data, village, ctx, gameTime);
            }
        }
    }

    private static void resolveDefenderVictory(
            MinecraftServer server,
            ServerLevel level,
            GodVillageSavedData data,
            GodVillageSavedData.GodVillageRecord village,
            ActiveRaidContext ctx,
            long gameTime
    ) {
        ACTIVE_RAIDS.remove(village.id());
        data.updateVillage(village.withState(GodVillageState.CONSAGRADA, gameTime));

        BlockPos altarPos = village.altarPos();
        level.playSound(null, altarPos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.5F, 1.0F);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, altarPos.getX() + 0.5D, altarPos.getY() + 1.5D, altarPos.getZ() + 0.5D, 60, 1.5D, 1.5D, 1.5D, 0.2D);

        Component victoryMsg = Component.literal("🛡 ¡VICTORIA DEL TEMPLO! ")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                .append(Component.literal("Los fieles de ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(ctx.defenderGod().getDisplayName()).withStyle(ctx.defenderGod().getChatColor(), ChatFormatting.BOLD))
                .append(Component.literal(" han repelido la invasión de ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(ctx.attackerGod().getDisplayName()).withStyle(ctx.attackerGod().getChatColor(), ChatFormatting.BOLD))
                .append(Component.literal(".").withStyle(ChatFormatting.GRAY));
        server.getPlayerList().broadcastSystemMessage(victoryMsg, false);

        // Recompensar a los jugadores fieles cercanos que participaron en la defensa
        AABB rewardBox = new AABB(altarPos).inflate(village.radius() + 16.0D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, rewardBox, p -> p.isAlive())) {
            if (SinFavorManager.getFavor(player, ctx.defenderGod()) >= 0) {
                SinFavorManager.addEffort(player, ctx.defenderGod(), 3.0F, "Defensa victoriosa del Templo de " + ctx.defenderGod().getDisplayName());
                ModAdvancements.award(player, "defend_shrine_victory");
            }
        }

        onVillageRaidEnded(server, level, village.id(), altarPos, ctx.defenderGod());
        onVillageRaidEnded(server, level, village.id(), altarPos, ctx.attackerGod());
    }

    private static void resolveAttackerConquest(
            MinecraftServer server,
            ServerLevel level,
            GodVillageSavedData data,
            GodVillageSavedData.GodVillageRecord village,
            ActiveRaidContext ctx,
            long gameTime
    ) {
        ACTIVE_RAIDS.remove(village.id());
        BlockPos altarPos = village.altarPos();
        SinGod winner = ctx.attackerGod();

        // Convertir el Altar Mayor al dios conquistador
        BlockState newAltarState = ModBlocks.getAltarFor(winner).defaultBlockState();
        level.setBlockAndUpdate(altarPos, newAltarState);
        if (level.getBlockEntity(altarPos) instanceof SinAltarBlockEntity altarBe) {
            altarBe.setVillageShrine(true);
        }

        GodVillageState newState = (winner == SinGod.ENVY) ? GodVillageState.ARRASADA : GodVillageState.CONSAGRADA;
        data.updateVillage(village.withPatronGod(winner, newState, gameTime));

        // Convertir a los acólitos supervivientes en centinelas del nuevo templo
        AABB searchBox = new AABB(altarPos).inflate(village.radius() + 24.0D);
        for (GodAcolyteEntity survivor : level.getEntitiesOfClass(GodAcolyteEntity.class, searchBox, a -> a.isAlive())) {
            if (survivor.getGod() == winner) {
                survivor.configureVillageSentinel(level, winner, altarPos);
            }
        }

        level.playSound(null, altarPos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5F, 0.8F);
        level.sendParticles(ParticleTypes.FLAME, altarPos.getX() + 0.5D, altarPos.getY() + 1.2D, altarPos.getZ() + 0.5D, 80, 1.5D, 1.5D, 1.5D, 0.1D);

        String actionDesc = (winner == SinGod.ENVY)
                ? " ha arrasado y reducido a ruinas el Templo de "
                : " ha conquistado y reclamado el Templo de ";

        Component conquestMsg = Component.literal("🔥 ¡CAÍDA DEL SANTUARIO! ")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                .append(Component.literal("El culto de ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(winner.getDisplayName()).withStyle(winner.getChatColor(), ChatFormatting.BOLD))
                .append(Component.literal(actionDesc).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(ctx.defenderGod().getDisplayName()).withStyle(ctx.defenderGod().getChatColor(), ChatFormatting.BOLD))
                .append(Component.literal(String.format(" en [%d, %d]!", altarPos.getX(), altarPos.getZ())).withStyle(ChatFormatting.RED));
        server.getPlayerList().broadcastSystemMessage(conquestMsg, false);

        // Recompensar a los jugadores conquistadores cercanos y conceder avance
        AABB conquestRewardBox = new AABB(altarPos).inflate(village.radius() + 32.0D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, conquestRewardBox, p -> p.isAlive())) {
            if (SinFavorManager.getFavor(player, winner) >= 0) {
                SinFavorManager.addEffort(player, winner, 3.5F, "Conquista del Templo de " + winner.getDisplayName());
                ModAdvancements.award(player, "temple_conquest");
            }
        }

        onVillageRaidEnded(server, level, village.id(), altarPos, ctx.defenderGod());
        onVillageRaidEnded(server, level, village.id(), altarPos, ctx.attackerGod());
    }

    public static List<GodAcolyteEntity> spawnInvasionForces(ServerLevel level, BlockPos center, SinGod attackerGod, int count) {
        List<GodAcolyteEntity> spawned = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GodAcolyteEntity acolyte = ModEntities.GOD_ACOLYTE.get().create(level, EntitySpawnReason.EVENT);
            if (acolyte != null) {
                double angle = (2.0D * Math.PI / count) * i;
                int x = (int) Math.round(center.getX() + 32.0D * Math.cos(angle));
                int z = (int) Math.round(center.getZ() + 32.0D * Math.sin(angle));
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

                acolyte.snapTo(x + 0.5D, y, z + 0.5D, 0.0F, 0.0F);
                acolyte.configureAcolyte(level, attackerGod, null, false);
                acolyte.setHomeAltarPos(center);
                acolyte.getNavigation().moveTo(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D, 1.15D);
                level.addFreshEntity(acolyte);
                spawned.add(acolyte);
            }
        }
        return spawned;
    }

    public static void ensureMinimumDefenders(ServerLevel level, BlockPos altarPos, SinGod defenderGod, int minDefenders) {
        AABB box = new AABB(altarPos).inflate(48.0D);
        List<GodAcolyteEntity> existing = level.getEntitiesOfClass(
                GodAcolyteEntity.class,
                box,
                a -> a.isAlive() && a.getGod() == defenderGod && a.isVillageSentinel() && altarPos.equals(a.getHomeAltarPos())
        );
        int needed = Math.max(0, minDefenders - existing.size());
        for (int i = 0; i < needed; i++) {
            GodAcolyteEntity sentinel = ModEntities.GOD_ACOLYTE.get().create(level, EntitySpawnReason.EVENT);
            if (sentinel != null) {
                double angle = (2.0D * Math.PI / Math.max(1, needed)) * i;
                int x = (int) Math.round(altarPos.getX() + 6.0D * Math.cos(angle));
                int z = (int) Math.round(altarPos.getZ() + 6.0D * Math.sin(angle));
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                sentinel.snapTo(x + 0.5D, y, z + 0.5D, 0.0F, 0.0F);
                sentinel.configureVillageSentinel(level, defenderGod, altarPos);
                level.addFreshEntity(sentinel);
            }
        }
    }

    public static SinGod pickRivalGod(SinGod defender, RandomSource random) {
        // Leviatán (Envidia) tiene doble peso para lanzar asaltos de saqueo contra aldeas prósperas
        List<SinGod> pool = new ArrayList<>();
        for (SinGod candidate : Arrays.asList(SinGod.values())) {
            if (candidate != defender) {
                pool.add(candidate);
                if (candidate == SinGod.ENVY) {
                    pool.add(candidate);
                }
            }
        }
        return pool.get(random.nextInt(pool.size()));
    }

    /**
     * Evalúa si una aldea consagrada ha perdido a todos sus aldeanos civiles (0 vivos)
     * para pasar a estado DESOLADA, o si una aldea DESOLADA ha cumplido su duelo de 48,000 ticks
     * (24,000 ticks para Asmodeus / Lujuria) para recibir la Caravana de Peregrinos.
     */
    private static void checkDesolationState(
            MinecraftServer server,
            ServerLevel level,
            GodVillageSavedData data,
            GodVillageSavedData.GodVillageRecord village,
            long gameTime
    ) {
        BlockPos altarPos = village.altarPos();
        if (!level.isLoaded(altarPos)) {
            return;
        }

        if (village.state() == GodVillageState.CONSAGRADA && village.cultureInitialized()) {
            if (!level.hasNearbyAlivePlayer(altarPos.getX() + 0.5D, altarPos.getY() + 0.5D, altarPos.getZ() + 0.5D, 96.0D)) {
                return;
            }
            AABB box = new AABB(altarPos).inflate(village.radius());
            List<Villager> villagers = level.getEntitiesOfClass(Villager.class, box, v -> v.isAlive());
            if (villagers.isEmpty()) {
                transitionToDesolated(server, level, data, village, gameTime);
            }
        }
    }

    public static void transitionToDesolated(
            MinecraftServer server,
            ServerLevel level,
            GodVillageSavedData data,
            GodVillageSavedData.GodVillageRecord village,
            long gameTime
    ) {
        BlockPos altarPos = village.altarPos();
        data.updateVillage(village.withState(GodVillageState.DESOLADA, gameTime));
        level.playSound(null, altarPos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.5F, 0.65F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, altarPos.getX() + 0.5D, altarPos.getY() + 1.2D, altarPos.getZ() + 0.5D, 40, 0.5D, 0.6D, 0.5D, 0.02D);

        // Detalle de Belphegor (Pereza): incluso desolada, invoca 2 Gólems de Hierro durmientes como guardianes
        if (village.patronGod() == SinGod.SLOTH) {
            for (int i = 0; i < 2; i++) {
                IronGolem golem = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.EVENT);
                if (golem != null) {
                    int gx = altarPos.getX() + (i == 0 ? 3 : -3);
                    int gz = altarPos.getZ();
                    int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gx, gz);
                    golem.snapTo(gx + 0.5D, gy, gz + 0.5D, 0.0F, 0.0F);
                    golem.setPlayerCreated(false);
                    level.addFreshEntity(golem);
                }
            }
        }

        Component msg = Component.literal("🕯 ¡SANTUARIO DESOLADO! ")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD)
                .append(Component.literal("La aldea consagrada a ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(village.patronGod().getDisplayName()).withStyle(village.patronGod().getChatColor(), ChatFormatting.BOLD))
                .append(Component.literal(String.format(" en [%d, %d] ha perdido a todos sus habitantes. Su altar entra en vigilia...", altarPos.getX(), altarPos.getZ()))
                        .withStyle(ChatFormatting.GRAY));
        server.getPlayerList().broadcastSystemMessage(msg, false);
    }

    private static void tickDesolatedVillages(MinecraftServer server, ServerLevel level) {
        GodVillageSavedData data = GodVillageSavedData.get(level);
        List<GodVillageSavedData.GodVillageRecord> snapshot = new ArrayList<>(data.getAllVillages());

        for (GodVillageSavedData.GodVillageRecord village : snapshot) {
            if (village.state() != GodVillageState.DESOLADA) {
                continue;
            }
            BlockPos altarPos = village.altarPos();
            if (!level.isLoaded(altarPos)) {
                continue;
            }

            // Humo negro y chispas moribundas sobre el altar en duelo
            level.sendParticles(ParticleTypes.LARGE_SMOKE, altarPos.getX() + 0.5D, altarPos.getY() + 1.1D, altarPos.getZ() + 0.5D, 4, 0.25D, 0.3D, 0.25D, 0.01D);
            level.sendParticles(ParticleTypes.SMOKE, altarPos.getX() + 0.5D, altarPos.getY() + 1.2D, altarPos.getZ() + 0.5D, 6, 0.3D, 0.3D, 0.3D, 0.01D);

            checkRepopulation(server, level, data, village);
        }
    }

    public static void checkRepopulation(
            MinecraftServer server,
            ServerLevel level,
            GodVillageSavedData data,
            GodVillageSavedData.GodVillageRecord village
    ) {
        if (village.state() != GodVillageState.DESOLADA) {
            return;
        }

        // Asmodeus (Lujuria) repuebla el doble de rápido (24,000 ticks = 1 día MC); el resto en 48,000 ticks (2 días MC)
        long requiredTicks = (village.patronGod() == SinGod.LUST) ? 24000L : 48000L;
        long elapsedTicks = level.getGameTime() - village.stateSinceTick();
        if (elapsedTicks >= requiredTicks) {
            int pilgrimCount = (village.patronGod() == SinGod.LUST) ? 4 : 3;
            spawnPilgrimCaravan(level, village.altarPos(), village.patronGod(), pilgrimCount);

            data.updateVillage(village.withState(GodVillageState.CONSAGRADA, level.getGameTime()));

            Component msg = Component.literal("✨ ¡EL LLAMADO DE LOS FIELES! ")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                    .append(Component.literal("Nuevos devotos llegan en peregrinación para reavivar el Templo de ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(village.patronGod().getDisplayName()).withStyle(village.patronGod().getChatColor(), ChatFormatting.BOLD))
                    .append(Component.literal("!").withStyle(ChatFormatting.WHITE));

            server.getPlayerList().broadcastSystemMessage(msg, false);
        }
    }

    public static void spawnPilgrimCaravan(ServerLevel level, BlockPos altarPos, SinGod god, int count) {
        for (int i = 0; i < count; i++) {
            Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.EVENT);
            if (villager != null) {
                double angle = (2.0D * Math.PI / Math.max(1, count)) * i;
                int x = (int) Math.round(altarPos.getX() + 18.0D * Math.cos(angle));
                int z = (int) Math.round(altarPos.getZ() + 18.0D * Math.sin(angle));
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                villager.snapTo(x + 0.5D, y, z + 0.5D, 0.0F, 0.0F);
                villager.getNavigation().moveTo(altarPos.getX() + 0.5D, altarPos.getY(), altarPos.getZ() + 0.5D, 0.85D);
                level.addFreshEntity(villager);
            }
        }
        // 1 Acólito centinela permanente de escolta
        GodAcolyteEntity guardian = ModEntities.GOD_ACOLYTE.get().create(level, EntitySpawnReason.EVENT);
        if (guardian != null) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, altarPos.getX() + 2, altarPos.getZ() + 2);
            guardian.snapTo(altarPos.getX() + 2.5D, y, altarPos.getZ() + 2.5D, 0.0F, 0.0F);
            guardian.configureVillageSentinel(level, god, altarPos);
            level.addFreshEntity(guardian);
        }
    }
}
