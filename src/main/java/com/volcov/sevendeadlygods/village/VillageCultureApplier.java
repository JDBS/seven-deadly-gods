package com.volcov.sevendeadlygods.village;

import com.volcov.sevendeadlygods.advancement.ModAdvancements;
import com.volcov.sevendeadlygods.favor.SinFavorManager;
import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Aplicador de las 7 personalidades culturales y sociales de las Aldeas Consagradas:
 * 1. Mammon (Avaricia): Peaje áureo en puertas y bóvedas; aldeanos armeros.
 * 2. Khorvath (Ira): Guarnición pesada de 6 acólitos; llamas de alma y fragor marcial.
 * 3. Beelze (Gula): Aldeanos carniceros, humo constante de hornos y aura de festín (Saturación).
 * 4. Lucian (Soberbia): Protocolo imperial (exige arrodillarse con Shift para hablar con aldeanos).
 * 5. Leviatán (Envidia): Asentamientos arrasados en ruinas y merodeadores abisales.
 * 6. Belphegor (Pereza): Aldeanos 100% Holgazanes (Nitwits) protegidos por Gólems de Hierro.
 * 7. Asmodeus (Lujuria): Jardines de feromonas, notas musicales y reproducción pasiva de fauna.
 */
public class VillageCultureApplier {
    private static final int CULTURE_TICK_INTERVAL = 100; // Cada 5 segundos

    @SubscribeEvent
    public void onVillagerSpawn(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Villager villager) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        GodVillageSavedData data = GodVillageSavedData.get(level);
        Optional<GodVillageSavedData.GodVillageRecord> villageOpt = data.getVillageAt(villager.blockPosition());
        if (villageOpt.isEmpty()) {
            return;
        }

        SinGod god = villageOpt.get().patronGod();
        applyVillagerProfessionForGod(level, villager, god);
    }

    public static void applyVillagerProfessionForGod(ServerLevel level, Villager villager, SinGod god) {
        switch (god) {
            case SLOTH -> setVillagerProfession(level, villager, VillagerProfession.NITWIT);
            case GLUTTONY -> {
                if (villager.getVillagerData().profession().is(VillagerProfession.NONE) && level.getRandom().nextBoolean()) {
                    setVillagerProfession(level, villager, VillagerProfession.BUTCHER);
                }
            }
            case GREED -> {
                if (villager.getVillagerData().profession().is(VillagerProfession.NONE) && level.getRandom().nextBoolean()) {
                    setVillagerProfession(level, villager, VillagerProfession.ARMORER);
                }
            }
            default -> {
            }
        }
    }

    private static void setVillagerProfession(ServerLevel level, Villager villager, ResourceKey<VillagerProfession> professionKey) {
        level.registryAccess().lookup(Registries.VILLAGER_PROFESSION)
                .flatMap(reg -> reg.get(professionKey))
                .ifPresent((Holder.Reference<VillagerProfession> holder) -> {
                    VillagerData current = villager.getVillagerData();
                    if (!current.profession().is(professionKey)) {
                        villager.setVillagerData(current.withProfession(holder));
                    }
                });
    }

    /**
     * Protocolo Imperial de Lucian (Soberbia):
     * En aldeas consagradas a Soberbia, los aldeanos exigen que el jugador se arrodille (Shift)
     * o sea un Discípulo de Lucian (Favor >= +60) antes de dignarse a hablar con él.
     */
    @SubscribeEvent
    public void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof Villager villager)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        GodVillageSavedData data = GodVillageSavedData.get(level);
        Optional<GodVillageSavedData.GodVillageRecord> villageOpt = data.getVillageAt(villager.blockPosition());
        if (villageOpt.isEmpty()) {
            return;
        }

        GodVillageSavedData.GodVillageRecord village = villageOpt.get();
        if (village.patronGod() == SinGod.PRIDE) {
            int prideFavor = SinFavorManager.getFavor(player, SinGod.PRIDE);
            if (!player.isCrouching() && prideFavor < 60) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
                villager.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.15F);
                player.sendSystemMessage(
                        SinGod.PRIDE.getFormattedPrefix().append(
                                Component.literal("«En los dominios de Lucian, los plebeyos deben arrodillarse (Shift) para dirigirse a sus ciudadanos.»")
                                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC)
                        )
                );
            } else if (player.isCrouching() && prideFavor < 60) {
                // Al arrodillarse con Shift, se acepta el gesto de sumisión y se abre el comercio explícitamente,
                // evitando que la interacción secundaria vanilla de Shift suprima el trade
                ModAdvancements.award(player, "secret_pride_humiliation");
                if (!villager.isBaby() && villager.isAlive() && !villager.isSleeping() && !villager.getOffers().isEmpty()) {
                    villager.setTradingPlayer(player);
                    villager.openTradingScreen(player, villager.getDisplayName(), villager.getVillagerData().level());
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                }
            }
        }
    }

    /**
     * Peaje Áureo de Mammon (Avaricia):
     * En aldeas consagradas a Mammon, abrir puertas exige portar al menos 1 pepita/lingote/bloque de oro
     * en mano. Para cofres y barriles, valida fondos y cobra el peaje únicamente al abrir con éxito.
     */
    @SubscribeEvent
    public void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        boolean isDoor = state.getBlock() instanceof DoorBlock;
        boolean isChest = be instanceof ChestBlockEntity || state.is(Blocks.BARREL);

        if (!isDoor && !isChest) {
            return;
        }

        Optional<GodVillageSavedData.GodVillageRecord> villageOpt = GodVillageSavedData.get(level).getVillageAt(pos);
        if (villageOpt.isEmpty() || villageOpt.get().patronGod() != SinGod.GREED) {
            return;
        }

        int greedFavor = SinFavorManager.getFavor(player, SinGod.GREED);
        if (greedFavor >= 60) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean holdingGold = isGoldCurrency(mainHand) || isGoldCurrency(offHand);

        if (isDoor && !holdingGold) {
            event.setCanceled(true);
            level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.9F);
            player.sendOverlayMessage(
                    Component.literal("Las puertas de Mammon solo se abren ante quienes muestran oro en la mano.")
                            .withStyle(ChatFormatting.GOLD)
            );
        } else if (isChest) {
            if (!hasGoldNugget(player)) {
                event.setCanceled(true);
                level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0F, 0.9F);
                player.sendSystemMessage(
                        SinGod.GREED.getFormattedPrefix().append(
                                Component.literal("«¿Pretendes husmear en las bóvedas de mi aldea sin pagar el diezmo de apertura (1 Pepita de Oro)?»")
                                        .withStyle(ChatFormatting.GOLD)
                        )
                );
            }
        }
    }

    /**
     * Peaje Áureo de Mammon (Avaricia) para apertura de cofres o barriles:
     * Cobra 1 pepita de oro únicamente cuando el contenedor se abre con éxito.
     */
    @SubscribeEvent
    public void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (!(event.getContainer() instanceof ChestMenu)) {
            return;
        }

        Optional<GodVillageSavedData.GodVillageRecord> villageOpt = GodVillageSavedData.get(level).getVillageAt(player.blockPosition());
        if (villageOpt.isEmpty() || villageOpt.get().patronGod() != SinGod.GREED) {
            return;
        }

        int greedFavor = SinFavorManager.getFavor(player, SinGod.GREED);
        if (greedFavor >= 60) {
            return;
        }

        if (!consumeOneGoldNugget(player)) {
            player.closeContainer();
            level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0F, 0.9F);
            player.sendSystemMessage(
                    SinGod.GREED.getFormattedPrefix().append(
                            Component.literal("«¿Pretendes husmear en las bóvedas de mi aldea sin pagar el diezmo de apertura (1 Pepita de Oro)?»")
                                    .withStyle(ChatFormatting.GOLD)
                    )
            );
        } else {
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.7F, 1.3F);
            ModAdvancements.award(player, "secret_greed_tax_paid");
        }
    }

    private static boolean hasGoldNugget(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.GOLD_NUGGET)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGoldCurrency(ItemStack stack) {
        return stack.is(Items.GOLD_NUGGET) || stack.is(Items.GOLD_INGOT) || stack.is(Items.GOLD_BLOCK);
    }

    private static boolean consumeOneGoldNugget(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.GOLD_NUGGET)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CULTURE_TICK_INTERVAL != 0) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            GodVillageSavedData data = GodVillageSavedData.get(level);
            List<GodVillageSavedData.GodVillageRecord> snapshot = new ArrayList<>(data.getAllVillages());

            for (GodVillageSavedData.GodVillageRecord village : snapshot) {
                BlockPos altarPos = village.altarPos();
                if (!level.isLoaded(altarPos)) {
                    continue;
                }

                if (!village.cultureInitialized()) {
                    initializeVillageCulture(level, data, village);
                }

                tickAmbientVillageCulture(level, village);
            }
        }
    }

    public static void initializeVillageCulture(
            ServerLevel level,
            GodVillageSavedData data,
            GodVillageSavedData.GodVillageRecord village
    ) {
        BlockPos altarPos = village.altarPos();
        SinGod god = village.patronGod();

        // 1. Spawnear guarnición inicial de Acólitos Centinelas Permanentes (6 para Khorvath, 3 para el resto)
        int sentinelCount = (god == SinGod.WRATH) ? 6 : 3;
        GodVillageRaidManager.ensureMinimumDefenders(level, altarPos, god, sentinelCount);

        // 2. Ajustar profesiones de los aldeanos existentes y peculiaridades iniciales
        AABB box = new AABB(altarPos).inflate(village.radius());
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, box, v -> v.isAlive());
        for (Villager villager : villagers) {
            applyVillagerProfessionForGod(level, villager, god);
        }

        // En las aldeas de Belphegor (Pereza), desplegar un Gólem de Hierro lento que haga el trabajo pesado
        if (god == SinGod.SLOTH) {
            List<IronGolem> golems = level.getEntitiesOfClass(IronGolem.class, box, g -> g.isAlive());
            if (golems.isEmpty()) {
                IronGolem golem = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.EVENT);
                if (golem != null) {
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, altarPos.getX() + 4, altarPos.getZ() + 4);
                    golem.snapTo(altarPos.getX() + 4.5D, y, altarPos.getZ() + 4.5D, 0.0F, 0.0F);
                    level.addFreshEntity(golem);
                }
            }
        }

        // En los asentamientos arrasados de Leviatán (Envidia), asegurar estado ARRASADA
        GodVillageState targetState = (god == SinGod.ENVY) ? GodVillageState.ARRASADA : village.state();
        data.updateVillage(village.withState(targetState, level.getGameTime()).withCultureInitialized(true));
    }

    private static void tickAmbientVillageCulture(ServerLevel level, GodVillageSavedData.GodVillageRecord village) {
        BlockPos altarPos = village.altarPos();
        if (!level.hasNearbyAlivePlayer(altarPos.getX() + 0.5D, altarPos.getY() + 0.5D, altarPos.getZ() + 0.5D, 64.0D)) {
            return;
        }

        SinGod god = village.patronGod();
        AABB box = new AABB(altarPos).inflate(village.radius());

        switch (god) {
            case GLUTTONY -> {
                // Beelze (Gula): humo constante de ahumadores y banquete gratuito (Saturación suave a devotos)
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, altarPos.getX() + 0.5D, altarPos.getY() + 1.5D, altarPos.getZ() + 0.5D, 6, 1.2D, 0.5D, 1.2D, 0.01D);
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box, p -> p.isAlive())) {
                    if (SinFavorManager.getFavor(player, SinGod.GLUTTONY) >= 0 && player.getFoodData().needsFood()) {
                        player.getFoodData().eat(1, 0.4F);
                    }
                }
            }
            case LUST -> {
                // Asmodeus (Lujuria): notas musicales, corazones y feromonas que inducen la procreación pasiva de animales
                level.sendParticles(ParticleTypes.NOTE, altarPos.getX() + 0.5D, altarPos.getY() + 1.8D, altarPos.getZ() + 0.5D, 4, 2.5D, 1.0D, 2.5D, 0.5D);
                level.sendParticles(ParticleTypes.HEART, altarPos.getX() + 0.5D, altarPos.getY() + 1.4D, altarPos.getZ() + 0.5D, 3, 2.0D, 0.8D, 2.0D, 0.02D);
                if (level.getRandom().nextFloat() < 0.25F) {
                    List<Animal> animals = level.getEntitiesOfClass(Animal.class, box, a -> a.isAlive() && !a.isBaby() && a.getAge() == 0 && !a.isInLove());
                    if (animals.size() >= 2) {
                        for (int i = 0; i < Math.min(2, animals.size()); i++) {
                            animals.get(i).setInLove(null);
                        }
                        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box, p -> p.isAlive())) {
                            ModAdvancements.award(player, "secret_lust_wild_passion");
                        }
                    }
                }
            }
            case WRATH -> {
                // Khorvath (Ira): chispas de fuego de alma en el foso de arena
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, altarPos.getX() + 0.5D, altarPos.getY() + 1.2D, altarPos.getZ() + 0.5D, 8, 1.5D, 0.5D, 1.5D, 0.02D);
            }
            case ENVY -> {
                // Leviatán (Envidia): niebla abisal sobre las ruinas arrasadas
                level.sendParticles(ParticleTypes.ASH, altarPos.getX() + 0.5D, altarPos.getY() + 1.5D, altarPos.getZ() + 0.5D, 12, 3.0D, 1.5D, 3.0D, 0.02D);
                level.sendParticles(ParticleTypes.DRIPPING_WATER, altarPos.getX() + 0.5D, altarPos.getY() + 1.2D, altarPos.getZ() + 0.5D, 6, 1.5D, 0.5D, 1.5D, 0.01D);
            }
            case SLOTH -> {
                // Belphegor (Pereza): aura de descanso regenerativo para quienes permanecen tranquilos
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box, p -> p.isAlive())) {
                    if (SinFavorManager.getFavor(player, SinGod.SLOTH) >= 0 && !player.isSprinting()) {
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 140, 0, true, false, true));
                    }
                }
            }
            case PRIDE -> {
                // Lucian (Soberbia): destellos solares imperiales sobre la catedral
                level.sendParticles(ParticleTypes.END_ROD, altarPos.getX() + 0.5D, altarPos.getY() + 1.6D, altarPos.getZ() + 0.5D, 5, 1.5D, 1.0D, 1.5D, 0.02D);
            }
            case GREED -> {
                // Mammon (Avaricia): destellos áureos sobre la bóveda
                level.sendParticles(ParticleTypes.WAX_ON, altarPos.getX() + 0.5D, altarPos.getY() + 1.3D, altarPos.getZ() + 0.5D, 6, 1.2D, 0.6D, 1.2D, 0.02D);
            }
        }
    }
}
