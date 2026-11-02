package com.volcov.sevendeadlygods.intervention;

import com.volcov.sevendeadlygods.entity.DivineEquipmentHelper;
import com.volcov.sevendeadlygods.entity.GodAcolyteEntity;
import com.volcov.sevendeadlygods.entity.GodAvatarEntity;
import com.volcov.sevendeadlygods.favor.PlayerSinData;
import com.volcov.sevendeadlygods.favor.SinFavorManager;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.registry.ModEntities;
import com.volcov.sevendeadlygods.registry.ModMobEffects;
import com.volcov.sevendeadlygods.village.GodVillageRaidManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class DivineInterventionScheduler {
    // 1 dia de Minecraft = 24,000 ticks (20 minutos reales)
    public static final long ONE_MC_DAY = 24000L;
    // 5 minutos reales = 300 segundos = 6,000 ticks de enfriamiento benevolo tras una caza
    public static final long WRATH_HUNT_BENEVOLENT_COOLDOWN_TICKS = 6000L;

    // Registro global de Avatar unico por cada Dios en el servidor
    private static final Map<SinGod, GodAvatarEntity> ACTIVE_AVATARS = new EnumMap<>(SinGod.class);
    // Momento en ticks del juego hasta el cual la ira del dios bloquea invocaciones benevolas
    private static final Map<SinGod, Long> WRATH_HUNT_END_TICKS = new EnumMap<>(SinGod.class);

    private DivineInterventionScheduler() {
    }

    public static void onAvatarSpawned(GodAvatarEntity avatar) {
        ACTIVE_AVATARS.put(avatar.getGod(), avatar);
        if (avatar.isWrathHunter() && avatar.level() instanceof ServerLevel sl) {
            WRATH_HUNT_END_TICKS.put(avatar.getGod(), sl.getGameTime() + WRATH_HUNT_BENEVOLENT_COOLDOWN_TICKS);
        }
    }

    public static void onAvatarTick(GodAvatarEntity avatar) {
        ACTIVE_AVATARS.put(avatar.getGod(), avatar);
        if (avatar.isWrathHunter() && avatar.level() instanceof ServerLevel sl) {
            WRATH_HUNT_END_TICKS.put(avatar.getGod(), sl.getGameTime() + WRATH_HUNT_BENEVOLENT_COOLDOWN_TICKS);
        }
    }

    public static void onAvatarRemoved(GodAvatarEntity avatar) {
        SinGod god = avatar.getGod();
        GodAvatarEntity current = ACTIVE_AVATARS.get(god);
        if (current == avatar || current == null || !current.isAlive() || current.isRemoved()) {
            ACTIVE_AVATARS.remove(god);
            if (avatar.isWrathHunter() && avatar.level() instanceof ServerLevel sl) {
                WRATH_HUNT_END_TICKS.put(god, sl.getGameTime() + WRATH_HUNT_BENEVOLENT_COOLDOWN_TICKS);
            }
        }
    }

    @Nullable
    public static GodAvatarEntity getActiveAvatar(@Nullable MinecraftServer server, SinGod god) {
        GodAvatarEntity cached = ACTIVE_AVATARS.get(god);
        if (cached != null && cached.isAlive() && !cached.isRemoved()) {
            return cached;
        }
        ACTIVE_AVATARS.remove(god);
        return null;
    }

    public static boolean isGodWrathActiveOrOnCooldown(@Nullable MinecraftServer server, SinGod god, long currentTick) {
        GodAvatarEntity active = getActiveAvatar(server, god);
        if (active != null && active.isWrathHunter()) {
            return true;
        }
        Long endTick = WRATH_HUNT_END_TICKS.get(god);
        return endTick != null && currentTick < endTick;
    }

    public static long getRemainingWrathCooldownTicks(@Nullable MinecraftServer server, SinGod god, long currentTick) {
        GodAvatarEntity active = getActiveAvatar(server, god);
        if (active != null && active.isWrathHunter()) {
            return WRATH_HUNT_BENEVOLENT_COOLDOWN_TICKS;
        }
        Long endTick = WRATH_HUNT_END_TICKS.get(god);
        if (endTick != null && currentTick < endTick) {
            return endTick - currentTick;
        }
        return 0L;
    }

    public static void tickPlayerInterventions(
            ServerLevel level,
            ServerPlayer player,
            PlayerSinData data,
            long gameTime
    ) {
        // Evaluar cada 5 segundos (100 ticks) para eficiencia
        if (gameTime % 100 != 0) {
            return;
        }

        RandomSource random = level.getRandom();

        // 1. Caza de Avatar garantizada si el jugador permanece en el abismo (-100)
        for (SinGod god : SinGod.values()) {
            if (data.isGodInSlumber(god, gameTime)) {
                continue;
            }
            if (data.getFavor(god) <= -100 && gameTime >= data.getNextAvatarManifestTick(god)) {
                triggerWrathAvatarHunt(player, god);
                data.setNextAvatarManifestTick(god, gameTime + ONE_MC_DAY * 2);
            }
        }

        // 2. Intervención periódica de los Dioses:
        // Los dioses con favor >= 30 o favor <= -30 actúan cada intervalo individual programado en el personaje.
        // Un dios de la lista de elegibles es seleccionado aleatoriamente.
        long nextPeriodic = data.getNextPeriodicInterventionTick();
        if (nextPeriodic == 0L) {
            // Primer intervalo programado según la configuración individual del personaje
            reschedulePlayerIntervention(data, gameTime, random);
        } else if (gameTime >= nextPeriodic) {
            forcePeriodicIntervention(level, player, data);
            reschedulePlayerIntervention(data, gameTime, random);
        }
    }

    /**
     * Reprograma el próximo tick de intervención divina usando los límites mínimo y máximo
     * configurados individualmente en el personaje del jugador.
     */
    public static void reschedulePlayerIntervention(PlayerSinData data, long gameTime, RandomSource random) {
        int min = Math.max(100, data.getMinInterventionTicks());
        int max = Math.max(min, data.getMaxInterventionTicks());
        data.setNextPeriodicInterventionTick(gameTime + Mth.nextInt(random, min, max));
    }

    /**
     * Selecciona aleatoriamente a un dios de la lista con favor >= 30 o favor <= -30 y ejecuta su acción.
     * @return true si había al menos un dios elegible y actuó; false si la lista estaba vacía.
     */
    public static boolean forcePeriodicIntervention(ServerLevel level, ServerPlayer player, PlayerSinData data) {
        RandomSource random = level.getRandom();
        List<SinGod> eligibleGods = new ArrayList<>();
        for (SinGod god : SinGod.values()) {
            if (data.isGodInSlumber(god, level.getGameTime())) {
                continue;
            }
            int favor = data.getFavor(god);
            if (favor >= 30 || favor <= -30) {
                eligibleGods.add(god);
            }
        }

        if (eligibleGods.isEmpty()) {
            return false;
        }

        SinGod chosenGod = eligibleGods.get(random.nextInt(eligibleGods.size()));
        executePeriodicDivineAction(level, player, data, chosenGod, random);
        return true;
    }

    /**
     * Ejecuta una acción divina periódica (hostil si favor <= -30, benévola si favor >= 30).
     */
    public static void executePeriodicDivineAction(
            ServerLevel level,
            ServerPlayer player,
            PlayerSinData data,
            SinGod god,
            RandomSource random
    ) {
        int favor = data.getFavor(god);

        if (favor <= -30) {
            executeNegativeGodAction(level, player, data, god, favor, random);
        } else if (favor >= 30) {
            executePositiveGodAction(level, player, data, god, favor, random);
        }
    }

    private static void executeNegativeGodAction(
            ServerLevel level,
            ServerPlayer player,
            PlayerSinData data,
            SinGod god,
            int favor,
            RandomSource random
    ) {
        if (favor <= -100) {
            triggerWrathAvatarHunt(player, god);
        } else if (favor <= -80) {
            if (random.nextFloat() < 0.65F) {
                spawnHostileAcolyteSquad(level, player, god, 2);
            } else {
                triggerThemedWrathAction(level, player, god, random);
            }
        } else if (favor <= -60) {
            if (random.nextFloat() < 0.65F) {
                spawnGodEnvoySkeleton(level, player, god);
            } else {
                triggerThemedWrathAction(level, player, god, random);
            }
        } else {
            triggerThemedWrathAction(level, player, god, random);
        }
    }

    /**
     * Desata una maldición o castigo temático propio del pecado del dios.
     */
    public static void triggerThemedWrathAction(
            ServerLevel level,
            ServerPlayer player,
            SinGod god,
            RandomSource random
    ) {
        BlockPos playerPos = player.blockPosition();

        switch (god) {
            case GREED -> {
                player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 900, 1));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 1));
                level.playSound(null, playerPos, SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.PLAYERS, 1.0F, 0.7F);
                level.playSound(null, playerPos, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.7F, 0.9F);
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1.0D, player.getZ(), 15, 0.4D, 0.5D, 0.4D, 0.05D);
                player.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.literal("«La codicia tiene un impuesto, mortal. Si no pagas diezmo a Mammon, tus riquezas serán tu propio lastre.»")
                                        .withStyle(ChatFormatting.RED))
                );
            }
            case WRATH -> {
                if (level.canSeeSky(playerPos)) {
                    LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
                    if (bolt != null) {
                        bolt.snapTo(player.getX(), player.getY(), player.getZ());
                        level.addFreshEntity(bolt);
                    }
                } else {
                    player.setRemainingFireTicks(80);
                    level.playSound(null, playerPos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8F, 1.2F);
                    level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.5D, 0.5D, 0.5D, 0.1D);
                }
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 1));
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 300, 0));
                player.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.literal("«¡Tu cobardía y falta de agallas ofenden a Khorvath! ¡Demuestra tu valía en sangre o sé consumido!»")
                                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
                );
            }
            case GLUTTONY -> {
                player.getFoodData().setFoodLevel(Math.max(2, player.getFoodData().getFoodLevel() - 8));
                player.getFoodData().setSaturation(0.0F);
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 800, 2));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 0));
                level.playSound(null, playerPos, SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 0.5F);
                level.playSound(null, playerPos, SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.8F, 0.7F);
                level.sendParticles(ParticleTypes.MYCELIUM, player.getX(), player.getY() + 1.0D, player.getZ(), 25, 0.4D, 0.5D, 0.4D, 0.05D);
                player.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.literal("«¿Osas descuidar el festín de Beelze? ¡Siente cómo el vacío de la gula carcome tus entrañas!»")
                                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                );
            }
            case PRIDE -> {
                player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 80, 1));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, 0));
                level.playSound(null, playerPos, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, playerPos, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.8F);
                level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(), 25, 0.3D, 0.6D, 0.3D, 0.08D);
                player.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.literal("«¡Aprende tu lugar ante el Sol Imperial! Lucian arroja por los aires a los mortales insolentes que no se arrodillan.»")
                                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                );
            }
            case ENVY -> {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 500, 0));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
                level.playSound(null, playerPos, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.8F, 0.8F);
                level.playSound(null, playerPos, SoundEvents.DROWNED_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
                level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0D, player.getZ(), 25, 0.4D, 0.5D, 0.4D, 0.05D);
                player.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.literal("«Tus logros despiertan los celos de las profundidades. Leviatán te despojará de toda luz y certeza...»")
                                        .withStyle(ChatFormatting.DARK_AQUA))
                );
            }
            case SLOTH -> {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 900, 2));
                player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 900, 1));
                level.playSound(null, playerPos, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.7F, 0.6F);
                level.sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.5D, 0.5D, 0.5D, 0.02D);
                player.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.literal("«Tanta prisa... tanta energía vana... Belphegor impone calma forzada a tus pies. Descansa...»")
                                        .withStyle(ChatFormatting.GRAY))
                );
            }
            case LUST -> {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 400, 0));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 1));
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 0));
                level.playSound(null, playerPos, SoundEvents.WITCH_CELEBRATE, SoundSource.PLAYERS, 0.9F, 0.8F);
                level.sendParticles(ParticleTypes.CRIMSON_SPORE, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.4D, 0.5D, 0.4D, 0.05D);
                player.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.literal("«Has despreciado el fuego sagrado de la pasión. Asmodeus te condena al tormento del deseo truncado.»")
                                        .withStyle(ChatFormatting.LIGHT_PURPLE))
                );
            }
        }
    }

    private static void executePositiveGodAction(
            ServerLevel level,
            ServerPlayer player,
            PlayerSinData data,
            SinGod god,
            int favor,
            RandomSource random
    ) {
        if (favor >= 90) {
            if (random.nextBoolean()) {
                // Gran bendición mayor de ritual (10 minutos) y curación total
                player.setHealth(player.getMaxHealth());
                player.getFoodData().eat(20, 1.0F);
                player.addEffect(new MobEffectInstance(ModMobEffects.getBlessingFor(god), 12000, 0));
                playBlessingAura(level, player);
                player.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.literal("«¡Mi devoto Campeón! Desciendo sobre ti la plenitud de mi bendición mayor. Nada quebrantará tu dominio.»")
                                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                );
            } else {
                deliverSupremeGodGift(level, player, god, random);
            }
        } else if (favor >= 60) {
            if (random.nextBoolean()) {
                // Bendición de ritual (5 minutos) y regeneración II
                player.addEffect(new MobEffectInstance(ModMobEffects.getBlessingFor(god), 6000, 0));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 1));
                playBlessingAura(level, player);
                player.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.literal("«Tu lealtad continúa dando frutos. Porta mi bendición y continúa extendiendo el pecado de la " + god.getSinName() + ".»")
                                        .withStyle(ChatFormatting.GREEN))
                );
            } else {
                deliverThemedGodGift(level, player, god, random, true);
            }
        } else {
            if (random.nextBoolean()) {
                deliverThemedGodGift(level, player, god, random, false);
            } else if (random.nextBoolean()) {
                deliverDivineRejuvenation(level, player, god);
            } else {
                triggerMinorBlessing(level, player, god, player.getHealth() < player.getMaxHealth() * 0.7F);
            }
        }
    }

    private static void deliverSupremeGodGift(ServerLevel level, ServerPlayer player, SinGod god, RandomSource random) {
        ItemStack gift = switch (god) {
            case GREED -> new ItemStack(Items.DIAMOND_BLOCK, 2);
            case WRATH -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1);
            case GLUTTONY -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 2);
            case PRIDE -> new ItemStack(Items.TOTEM_OF_UNDYING, 1);
            case ENVY -> new ItemStack(Items.HEART_OF_THE_SEA, 1);
            case SLOTH -> new ItemStack(Items.NETHERITE_SCRAP, 1);
            case LUST -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1);
        };
        giveOrDropItem(player, gift);
        playBlessingAura(level, player);
        player.sendSystemMessage(
                god.getFormattedPrefix()
                        .append(Component.literal("«Un tributo de los cielos para mi Elegido más devoto. Que el poder de " + god.getDisplayName() + " te acompañe.»")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
        );
    }

    private static void deliverThemedGodGift(ServerLevel level, ServerPlayer player, SinGod god, RandomSource random, boolean majorTier) {
        ItemStack gift = switch (god) {
            case GREED -> new ItemStack(majorTier ? Items.DIAMOND : Items.GOLD_INGOT, majorTier ? 4 : 8);
            case WRATH -> new ItemStack(Items.ARROW, majorTier ? 32 : 16);
            case GLUTTONY -> new ItemStack(majorTier ? Items.GOLDEN_CARROT : Items.COOKED_BEEF, majorTier ? 8 : 6);
            case PRIDE -> new ItemStack(majorTier ? Items.GOLDEN_APPLE : Items.EXPERIENCE_BOTTLE, majorTier ? 1 : 6);
            case ENVY -> new ItemStack(majorTier ? Items.NAUTILUS_SHELL : Items.ENDER_PEARL, majorTier ? 2 : 2);
            case SLOTH -> new ItemStack(majorTier ? Items.HONEY_BOTTLE : Items.SWEET_BERRIES, majorTier ? 3 : 8);
            case LUST -> new ItemStack(majorTier ? Items.GLISTERING_MELON_SLICE : Items.RED_TULIP, majorTier ? 4 : 4);
        };
        giveOrDropItem(player, gift);
        playBlessingAura(level, player);

        String text = switch (god) {
            case GREED -> "«Mammon comparte un dividendo de sus bóvedas subterráneas. Haz que rinda beneficios.»";
            case WRATH -> "«¡Khorvath aprueba tu ferocidad! Acepta estas provisiones para tu próxima masacre.»";
            case GLUTTONY -> "«¡El banquete de Beelze es generoso con los buenos comensales! Sáciate con estos manjares.»";
            case PRIDE -> "«Lucian arroja su gracia regia sobre tu figura. Destaca por encima de la plebe.»";
            case ENVY -> "«Leviatán te entrega tesoros arrebatados a otros. Que el resto muera de envidia.»";
            case SLOTH -> "«Qué bien se está cuando uno no tiene prisa... Belphegor te manda un refrigerio para descansar.»";
            case LUST -> "«Tu gracia cautiva los confines de este mundo. Asmodeus te agasaja con sus dones.»";
        };

        player.sendSystemMessage(
                god.getFormattedPrefix().append(Component.literal(text).withStyle(ChatFormatting.GREEN))
        );
    }

    private static void deliverDivineRejuvenation(ServerLevel level, ServerPlayer player, SinGod god) {
        player.heal(10.0F);
        player.getFoodData().eat(8, 0.8F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1));
        playBlessingAura(level, player);

        player.sendSystemMessage(
                god.getFormattedPrefix()
                        .append(Component.literal("«Tu devoción constante no pasa desapercibida. Mi gracia vigoriza tu cuerpo y renueva tus fuerzas.»")
                                .withStyle(ChatFormatting.GREEN))
        );
    }

    private static void playBlessingAura(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9F, 1.2F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.5D, 0.6D, 0.5D, 0.05D);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 0.5D, player.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.05D);
    }

    public static void giveOrDropItem(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }

    /**
     * Castigo medio (<= -60): Esqueleto bien equipado con armadura y arma de mele ("Enviado de <Dios>").
     */
    public static void spawnGodEnvoySkeleton(ServerLevel level, ServerPlayer player, SinGod god) {
        Skeleton envoy = EntityTypes.SKELETON.create(level, EntitySpawnReason.EVENT);
        if (envoy == null) {
            return;
        }
        BlockPos spawnPos = findSafeSpawnPosNear(level, player.blockPosition(), 6, 12);
        envoy.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        envoy.setCustomName(god.getEnvoyCustomName());
        envoy.setCustomNameVisible(true);

        // Equipar armadura completa y arma de mele (al no tener arco usa ataque cuerpo a cuerpo veloz)
        DivineEquipmentHelper.equipEnvoySkeleton(level, envoy, god);
        envoy.addEffect(new MobEffectInstance(MobEffects.SPEED, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        envoy.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        envoy.setTarget(player);

        level.addFreshEntity(envoy);
        level.sendParticles(ParticleTypes.SMOKE, envoy.getX(), envoy.getY() + 1.0D, envoy.getZ(), 30, 0.4D, 0.6D, 0.4D, 0.05D);
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.7F);

        player.sendSystemMessage(
                god.getFormattedPrefix()
                        .append(Component.literal("«He despachado a mi Enviado para cobrarte en acero tus agravios.»")
                                .withStyle(ChatFormatting.RED))
        );
    }

    /**
     * Castigo severo (<= -80): Escuadron de Acolitos/Sirvientes con capucha y alas.
     */
    public static void spawnHostileAcolyteSquad(ServerLevel level, ServerPlayer player, SinGod god, int count) {
        for (int i = 0; i < count; i++) {
            GodAcolyteEntity acolyte = ModEntities.GOD_ACOLYTE.get().create(level, EntitySpawnReason.EVENT);
            if (acolyte != null) {
                BlockPos spawnPos = findSafeSpawnPosNear(level, player.blockPosition(), 7, 14);
                acolyte.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
                acolyte.configureAcolyte(level, god, player.getUUID(), false);
                acolyte.setTarget(player);
                level.addFreshEntity(acolyte);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.0F, 0.8F);
        player.sendSystemMessage(
                god.getFormattedPrefix()
                        .append(Component.literal("«¡Mis Acólitos alados descienden sobre ti! ¡Ningún rincón del mundo te ocultará de mi ira!»")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
        );
    }

    /**
     * Castigo extremo (SOLO a -100): El Avatar del Dios se manifiesta en persona para dar caza al jugador.
     * Regla de Avatar Único: Si ya existe un Avatar activo del Dios en el servidor,
     * se desvanece de su ubicación anterior con un mensaje dramático y se teletransporta
     * al nuevo insolente conservando su vida.
     */
    public static void triggerWrathAvatarHunt(ServerPlayer player, SinGod god) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        PlayerSinData data = SinFavorManager.get(player);
        if (data != null && data.isGodInSlumber(god, level.getGameTime())) {
            return;
        }

        MinecraftServer server = level.getServer();
        GodAvatarEntity existingAvatar = getActiveAvatar(server, god);

        if (existingAvatar != null) {
            // Prioridad Absoluta del Asedio Territorial sobre la Caza Individual:
            // Si el Avatar está combatiendo en una Guerra Santa de aldea, se pospone la caza del jugador hasta concluir.
            if (existingAvatar.isVillageRaidAvatar()) {
                GodVillageRaidManager.postponeWrathHunt(god, player.getUUID());
                player.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.literal("«¡Tu sentencia de muerte aguarda, mortal! En cuanto concluya la Guerra Santa en mi templo sagrado, descenderé por tu cabeza.»")
                                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC))
                );
                return;
            }

            // Si el Avatar ya está cazando a este mismo jugador en la misma dimensión y cerca, no hacer nada
            if (existingAvatar.isWrathHunter()
                    && existingAvatar.level() == level
                    && existingAvatar.distanceToSqr(player) <= 48.0D * 48.0D) {
                return;
            }

            // --- ESCENA DE SALIDA EN LA UBICACIÓN ANTERIOR (Opción 1 + Opción 3) ---
            ServerLevel oldLevel = (ServerLevel) existingAvatar.level();
            BlockPos oldPos = existingAvatar.blockPosition();

            oldLevel.playSound(null, oldPos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.2F, 0.8F);
            oldLevel.playSound(null, oldPos, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1.0F, 0.7F);
            oldLevel.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, -1), existingAvatar.getX(), existingAvatar.getY() + 1.5D, existingAvatar.getZ(), 8, 0.5D, 0.5D, 0.5D, 0.0D);
            oldLevel.sendParticles(ParticleTypes.PORTAL, existingAvatar.getX(), existingAvatar.getY() + 1.2D, existingAvatar.getZ(), 60, 0.6D, 1.0D, 0.6D, 0.2D);
            oldLevel.sendParticles(ParticleTypes.FLAME, existingAvatar.getX(), existingAvatar.getY() + 1.0D, existingAvatar.getZ(), 50, 0.5D, 0.8D, 0.5D, 0.1D);

            // Mensaje dramático de despedida al jugador anterior (si es un jugador distinto)
            Player previousPlayer = null;
            if (existingAvatar.getTargetPlayerUuid() != null && server != null) {
                previousPlayer = server.getPlayerList().getPlayer(existingAvatar.getTargetPlayerUuid());
            }
            if (previousPlayer == null) {
                previousPlayer = existingAvatar.getChampionPlayer();
            }

            if (previousPlayer instanceof ServerPlayer sp && !sp.getUUID().equals(player.getUUID()) && sp.isAlive()) {
                if (existingAvatar.isWrathHunter()) {
                    sp.sendSystemMessage(
                            god.getFormattedPrefix()
                                    .append(Component.literal("«¡Otro insolente demanda mi castigo supremo! Considérate afortunado... por ahora.»")
                                            .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC))
                    );
                } else {
                    sp.sendSystemMessage(
                            god.getFormattedPrefix()
                                    .append(Component.literal("«Mi presencia física es requerida con urgencia: un insolente ha desafiado mi juicio supremo.»")
                                            .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC))
                    );
                }
            }

            // --- TELETRANSPORTE Y REUBICACIÓN AL NUEVO OBJETIVO ---
            BlockPos spawnPos = findSafeSpawnPosNear(level, player.blockPosition(), 8, 15);
            float preservedHealth = existingAvatar.getHealth();

            if (oldLevel == level) {
                existingAvatar.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
                existingAvatar.getNavigation().stop();
                existingAvatar.initializeAvatar(level, god, player.getUUID(), true);
                existingAvatar.setHealth(Math.min(preservedHealth, existingAvatar.getMaxHealth()));
                existingAvatar.setTarget(player);
            } else {
                existingAvatar.discard();
                GodAvatarEntity newAvatar = ModEntities.GOD_AVATAR.get().create(level, EntitySpawnReason.EVENT);
                if (newAvatar != null) {
                    newAvatar.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
                    newAvatar.initializeAvatar(level, god, player.getUUID(), true);
                    newAvatar.setHealth(Math.min(preservedHealth, newAvatar.getMaxHealth()));
                    newAvatar.setTarget(player);
                    level.addFreshEntity(newAvatar);
                }
            }

            // Efectos de llegada
            LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
            if (bolt != null) {
                bolt.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
            level.playSound(null, spawnPos, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.0F, 1.0F);

            player.sendSystemMessage(
                    god.getFormattedPrefix()
                            .append(Component.literal("«¡HAS COLMADO LA PACIENCIA DIVINA! ¡HE DESCENDIDO EN PERSONA PARA RECLAMAR TU VIDA!»")
                                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
            );
            return;
        }

        // Si no existía Avatar previo del Dios, spawn fresco
        GodAvatarEntity avatar = ModEntities.GOD_AVATAR.get().create(level, EntitySpawnReason.EVENT);
        if (avatar == null) {
            return;
        }
        BlockPos spawnPos = findSafeSpawnPosNear(level, player.blockPosition(), 8, 15);
        avatar.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
        avatar.initializeAvatar(level, god, player.getUUID(), true);
        avatar.setTarget(player);
        level.addFreshEntity(avatar);

        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
        if (bolt != null) {
            bolt.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }

        player.sendSystemMessage(
                god.getFormattedPrefix()
                        .append(Component.literal("«¡HAS COLMADO LA PACIENCIA DIVINA! ¡HE DESCENDIDO EN PERSONA PARA RECLAMAR TU VIDA!»")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
        );
    }

    /**
     * Bendicion menor (>= +20): Regeneracion cuando falta vida o rellenar barra de hambre con saturacion.
     */
    public static void triggerMinorBlessing(
            ServerLevel level,
            ServerPlayer player,
            SinGod god,
            boolean healFocus
    ) {
        if (healFocus) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            player.sendSystemMessage(
                    god.getFormattedPrefix()
                            .append(Component.literal("«No caigas por heridas tan simples, mortal. Mi favor cierra tus llagas.»")
                                    .withStyle(ChatFormatting.GREEN))
            );
        } else {
            player.getFoodData().eat(8, 1.0F);
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 60, 0));
            player.sendSystemMessage(
                    god.getFormattedPrefix()
                            .append(Component.literal("«Sacia tu apetito con mi gracia y continúa sirviéndome.»")
                                    .withStyle(ChatFormatting.GREEN))
            );
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0D, player.getZ(), 15, 0.4D, 0.5D, 0.4D, 0.1D);
    }

    /**
     * Bendicion avanzada (>= +85): El dios envia a sus Acolitos para ayudar al jugador en combate
     * si esta bajo de vida o si le ataca otro dios (Guerras de Dioses).
     */
    public static void trySpawnAlliedAcolytesForDefense(
            ServerLevel level,
            ServerPlayer player,
            @Nullable LivingEntity attacker,
            boolean isGodWarTrigger
    ) {
        PlayerSinData data = com.volcov.sevendeadlygods.favor.SinFavorManager.get(player);
        long now = level.getGameTime();

        for (SinGod god : SinGod.values()) {
            if (data.getFavor(god) >= 85 && now >= data.getNextAlliedAcolyteTick(god)) {
                // Enfriamiento de medio dia (12,000 ticks) entre despliegues de Acolitos aliados
                data.setNextAlliedAcolyteTick(god, now + 12000L);

                for (int i = 0; i < 2; i++) {
                    GodAcolyteEntity protector = ModEntities.GOD_ACOLYTE.get().create(level, EntitySpawnReason.EVENT);
                    if (protector != null) {
                        BlockPos pos = findSafeSpawnPosNear(level, player.blockPosition(), 3, 6);
                        protector.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
                        protector.configureAcolyte(level, god, player.getUUID(), true);
                        if (attacker != null && attacker.isAlive()) {
                            protector.setTarget(attacker);
                        }
                        level.addFreshEntity(protector);
                    }
                }

                level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.1F);
                String msg = isGodWarTrigger
                        ? "«¡¿Otro dios osa tocar a mi Elegido?! ¡Acólitos míos, destrozad a esos intrusos!»"
                        : "«¡Resiste, mi Elegido! ¡Mis Acólitos alados combaten a tu lado!»";
                player.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.literal(msg).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                );
                return;
            }
        }
    }

    /**
     * Manifestación Benévola del Avatar (Favor 100).
     * - Protección anti-exploit: Si el Dios está cazando a un jugador o estuvo cazando en los últimos 5 minutos,
     *   rechaza la manifestación benévola con un mensaje dramático.
     * - Regla de Avatar Único: Si el Dios ya estaba manifestado benévolamente con otro Campeón,
     *   se despide solemnemente y se traslada junto al nuevo Campeón.
     * @return true si se manifestó o reubicó; false si fue bloqueado por enfriamiento de ira o ya estaba cerca.
     */
    public static boolean tryTriggerBenevolentAvatar(ServerLevel level, ServerPlayer player, SinGod god) {
        MinecraftServer server = level.getServer();
        long gameTime = level.getGameTime();

        // 1. COMPROBACIÓN ANTI-EXPLOIT: Enfriamiento de ira (5 minutos / 6,000 ticks)
        if (isGodWrathActiveOrOnCooldown(server, god, gameTime)) {
            GodAvatarEntity active = getActiveAvatar(server, god);
            if (active != null && active.isWrathHunter()) {
                player.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.literal("«Mi presencia física está en este momento cazando a un insolente. No atenderé llamados de devoción mientras la sangre no haya sido derramada.»")
                                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
                );
            } else {
                player.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.literal("«Mi ira aún arde por ejecuciones recientes. Las brasas de mi cólera deben enfriarse antes de que vuelva a manifestarme en paz.»")
                                        .withStyle(ChatFormatting.RED))
                );
            }
            return false;
        }

        GodAvatarEntity existingAvatar = getActiveAvatar(server, god);

        if (existingAvatar != null) {
            if (existingAvatar.isVillageRaidAvatar()) {
                player.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.literal("«¡Mi presencia física combate en este instante en una Guerra Santa por mi santuario! Acude a mi templo o aguarda a que concluya la batalla.»")
                                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                );
                return false;
            }

            // Si ya está manifestado benévolamente cerca de este mismo jugador (en radio de 32 bloques), no duplicar
            if (!existingAvatar.isWrathHunter()
                    && existingAvatar.level() == level
                    && existingAvatar.distanceToSqr(player) <= 32.0D * 32.0D) {
                player.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.literal("«Ya me encuentro manifestado ante ti, devoto mío. Acércate y hablemos.»")
                                        .withStyle(ChatFormatting.GOLD))
                );
                return false;
            }

            // --- ESCENA DE SALIDA EN LA UBICACIÓN ANTERIOR ---
            ServerLevel oldLevel = (ServerLevel) existingAvatar.level();
            BlockPos oldPos = existingAvatar.blockPosition();

            oldLevel.playSound(null, oldPos, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.2F);
            oldLevel.sendParticles(ParticleTypes.END_ROD, existingAvatar.getX(), existingAvatar.getY() + 1.2D, existingAvatar.getZ(), 50, 0.5D, 1.0D, 0.5D, 0.1D);
            oldLevel.sendParticles(ParticleTypes.PORTAL, existingAvatar.getX(), existingAvatar.getY() + 1.0D, existingAvatar.getZ(), 40, 0.4D, 0.8D, 0.4D, 0.1D);

            Player previousPlayer = null;
            if (existingAvatar.getTargetPlayerUuid() != null && server != null) {
                previousPlayer = server.getPlayerList().getPlayer(existingAvatar.getTargetPlayerUuid());
            }
            if (previousPlayer == null) {
                previousPlayer = existingAvatar.getChampionPlayer();
            }

            if (previousPlayer instanceof ServerPlayer sp && !sp.getUUID().equals(player.getUUID()) && sp.isAlive()) {
                sp.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.literal("«Mi presencia física es requerida por otro Campeón en este plano. Que mi bendición continúe contigo.»")
                                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC))
                );
            }

            // --- REUBICACIÓN AL NUEVO CAMPEÓN ---
            BlockPos spawnPos = findSafeSpawnPosNear(level, player.blockPosition(), 3, 6);
            float preservedHealth = existingAvatar.getHealth();

            if (oldLevel == level) {
                existingAvatar.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
                existingAvatar.getNavigation().stop();
                existingAvatar.initializeAvatar(level, god, player.getUUID(), false);
                existingAvatar.setHealth(Math.min(preservedHealth, existingAvatar.getMaxHealth()));
            } else {
                existingAvatar.discard();
                GodAvatarEntity newAvatar = ModEntities.GOD_AVATAR.get().create(level, EntitySpawnReason.EVENT);
                if (newAvatar != null) {
                    newAvatar.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
                    newAvatar.initializeAvatar(level, god, player.getUUID(), false);
                    newAvatar.setHealth(Math.min(preservedHealth, newAvatar.getMaxHealth()));
                    level.addFreshEntity(newAvatar);
                }
            }

            level.playSound(null, spawnPos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.sendParticles(ParticleTypes.END_ROD, spawnPos.getX() + 0.5D, spawnPos.getY() + 1.5D, spawnPos.getZ() + 0.5D, 40, 0.4D, 0.8D, 0.4D, 0.05D);
            player.sendSystemMessage(
                    god.getFormattedPrefix()
                            .append(Component.literal("«Tu devoción ha alcanzado la cúspide suprema. Me manifiesto ante ti, mi Campeón: acércate para recibir mi Reliquia y hablemos cara a cara en el chat.»")
                                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            );
            return true;
        }

        // Si no existía Avatar previo del Dios, spawn fresco
        GodAvatarEntity avatar = ModEntities.GOD_AVATAR.get().create(level, EntitySpawnReason.EVENT);
        if (avatar == null) {
            return false;
        }
        BlockPos pos = findSafeSpawnPosNear(level, player.blockPosition(), 3, 6);
        avatar.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        avatar.initializeAvatar(level, god, player.getUUID(), false);
        level.addFreshEntity(avatar);

        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D, 40, 0.4D, 0.8D, 0.4D, 0.05D);
        player.sendSystemMessage(
                god.getFormattedPrefix()
                        .append(Component.literal("«Tu devoción ha alcanzado la cúspide suprema. Me manifiesto ante ti, mi Campeón: acércate para recibir mi Reliquia y hablemos cara a cara en el chat.»")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
        );
        return true;
    }

    private static BlockPos findSafeSpawnPosNear(ServerLevel level, BlockPos origin, int minRadius, int maxRadius) {
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = Mth.nextInt(random, minRadius, maxRadius) * (random.nextBoolean() ? 1 : -1);
            int dz = Mth.nextInt(random, minRadius, maxRadius) * (random.nextBoolean() ? 1 : -1);
            BlockPos candidate = origin.offset(dx, 0, dz);
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate);
            if (Math.abs(surface.getY() - origin.getY()) <= 8) {
                return surface;
            }
        }
        return origin.above();
    }
}
