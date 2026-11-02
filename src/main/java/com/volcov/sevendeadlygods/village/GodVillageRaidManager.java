package com.volcov.sevendeadlygods.village;

import com.volcov.sevendeadlygods.block.SinAltarBlockEntity;
import com.volcov.sevendeadlygods.entity.GodAcolyteEntity;
import com.volcov.sevendeadlygods.favor.SinFavorManager;
import com.volcov.sevendeadlygods.god.SinGod;
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
            long startTick,
            boolean pausedByChunkUnload
    ) {
        public ActiveRaidContext withPaused(boolean paused) {
            return new ActiveRaidContext(villageId, defenderGod, attackerGod, startTick, paused);
        }
    }

    private static final Map<UUID, ActiveRaidContext> ACTIVE_RAIDS = new HashMap<>();

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int tickCount = server.getTickCount();

        if (tickCount % ACTIVE_RAID_TICK_INTERVAL == 0) {
            for (ServerLevel level : server.getAllLevels()) {
                tickActiveRaids(server, level);
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
                if (village.state() == GodVillageState.CONSAGRADA
                        && (gameTime - village.lastRaidTick() > RAID_COOLDOWN_TICKS)) {
                    BlockPos altarPos = village.altarPos();
                    if (level.hasNearbyAlivePlayer(altarPos.getX() + 0.5D, altarPos.getY() + 0.5D, altarPos.getZ() + 0.5D, 80.0D)) {
                        if (level.getRandom().nextFloat() < 0.15F) {
                            startAutonomousRaid(server, level, data, village);
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
        GodVillageSavedData.GodVillageRecord updated = village.withLastRaidTick(gameTime, GodVillageState.EN_ASEDIO, gameTime);
        data.updateVillage(updated);
        ACTIVE_RAIDS.put(village.id(), new ActiveRaidContext(village.id(), defender, attacker, gameTime, false));
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
            ActiveRaidContext ctx = ACTIVE_RAIDS.computeIfAbsent(
                    village.id(),
                    id -> new ActiveRaidContext(id, village.patronGod(), pickRivalGod(village.patronGod(), level.getRandom()), village.stateSinceTick(), false)
            );

            // Protección contra Chunk Unload: pausar evaluación si no hay jugadores en 128 bloques
            if (!level.hasNearbyAlivePlayer(altarPos.getX() + 0.5D, altarPos.getY() + 0.5D, altarPos.getZ() + 0.5D, 128.0D)) {
                if (!ctx.pausedByChunkUnload()) {
                    ACTIVE_RAIDS.put(village.id(), ctx.withPaused(true));
                }
                continue;
            } else if (ctx.pausedByChunkUnload()) {
                ACTIVE_RAIDS.put(village.id(), ctx.withPaused(false));
            }

            AABB searchBox = new AABB(altarPos).inflate(village.radius() + 24.0D);
            List<GodAcolyteEntity> allAcolytes = level.getEntitiesOfClass(GodAcolyteEntity.class, searchBox, GodAcolyteEntity::isAlive);

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

            boolean timedOut = (gameTime - ctx.startTick()) > MAX_RAID_DURATION_TICKS;

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
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, rewardBox, ServerPlayer::isAlive)) {
            if (SinFavorManager.getFavor(player, ctx.defenderGod()) >= 0) {
                SinFavorManager.addEffort(player, ctx.defenderGod(), 3.0F, "Defensa victoriosa del Templo de " + ctx.defenderGod().getDisplayName());
            }
        }
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
        for (GodAcolyteEntity survivor : level.getEntitiesOfClass(GodAcolyteEntity.class, searchBox, GodAcolyteEntity::isAlive)) {
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
                a -> a.isAlive() && a.getGod() == defenderGod
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
}
