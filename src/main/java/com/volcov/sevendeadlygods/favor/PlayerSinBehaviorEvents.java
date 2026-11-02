package com.volcov.sevendeadlygods.favor;

import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.advancement.ModAdvancements;
import com.volcov.sevendeadlygods.block.SinAltarBlock;
import com.volcov.sevendeadlygods.block.SinAltarBlockEntity;
import com.volcov.sevendeadlygods.config.ModConfig;
import com.volcov.sevendeadlygods.entity.GodAcolyteEntity;
import com.volcov.sevendeadlygods.entity.GodAvatarEntity;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.intervention.DivineInterventionScheduler;
import com.volcov.sevendeadlygods.lore.SinLoreLibrary;
import com.volcov.sevendeadlygods.registry.ModMobEffects;
import com.volcov.sevendeadlygods.village.GodVillageSavedData;
import com.volcov.sevendeadlygods.village.GodVillageState;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class PlayerSinBehaviorEvents {

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        PlayerSinData data = SinFavorManager.get(player);
        long gameTime = serverLevel.getGameTime();

        // 1. Control de Pereza (Belphegor): quietud vs esprintar
        double speedSq = player.getDeltaMovement().horizontalDistanceSqr();
        if (speedSq < 0.0005D && !player.isSprinting() && !player.isSwinging()) {
            int still = data.getSlothStillTicks() + 1;
            data.setSlothStillTicks(still);

            // Si permanece inmovil 30 segundos (600 ticks)
            if (still % 600 == 0) {
                SinFavorManager.addEffort(player, SinGod.SLOTH, 0.5F, "Quietud contemplativa");
                notifyNearbySlothAltar(serverLevel, player);
                GodVillageSavedData.get(serverLevel).getVillageAt(player.blockPosition()).ifPresent(v -> {
                    if (v.patronGod() == SinGod.SLOTH) {
                        ModAdvancements.award(player, "secret_sloth_society");
                    }
                });
            }
        } else {
            data.setSlothStillTicks(0);
            if (player.isSprinting() && gameTime % 400 == 0) {
                // Esprintar constantemente molesta a Belphegor
                SinFavorManager.addEffort(player, SinGod.SLOTH, -0.35F, "Prisa innecesaria");
            }
        }

        // Chequeos periodicos cada 15 segundos (300 ticks)
        if (gameTime % 300 == 0) {
            // 2. Control de Avaricia (Mammon):
            // Le enfada que el jugador lleve objetos o armaduras valiosas sin haberle rendido tributo recientemente
            boolean carryingRiches = isWearingOrCarryingValuables(player);
            long ticksSinceTribute = gameTime - data.getLastGreedTributeTick();
            if (carryingRiches && (data.getLastGreedTributeTick() == 0L || ticksSinceTribute > 24000L)) {
                SinFavorManager.addEffort(player, SinGod.GREED, -0.45F, "Portar riquezas sin pagar tributo a Mammon");
            }

            // 3. Control de Gula (Beelze):
            // Le enfada que el jugador pase hambre
            int foodLevel = player.getFoodData().getFoodLevel();
            if (foodLevel <= 6) {
                SinFavorManager.addEffort(player, SinGod.GLUTTONY, -0.4F, "Pasar hambre miserablemente");
            }

            // 4. Control de Lujuria (Asmodeus):
            // Le complace que el jugador lleve armaduras decoradas con Armor Trims
            if (hasArmorTrimEquipped(player)) {
                SinFavorManager.addEffort(player, SinGod.LUST, 0.25F, "Vestir armadura engalanada");
            }

            // 5. Control de Ira (Khorvath):
            // Si pasa mas de 1 dia entero sin matar nada, Khorvath se impacienta
            if (data.getLastWrathKillTick() > 0 && (gameTime - data.getLastWrathKillTick()) > 24000L) {
                SinFavorManager.addEffort(player, SinGod.WRATH, -0.3F, "Pacifismo prolongado");
            }
        }

        // Ejecutar el planificador de intervenciones divinas (Enviados, Acolitos, Avatares, Bendiciones)
        DivineInterventionScheduler.tickPlayerInterventions(serverLevel, player, data, gameTime);

        // Descubrimiento de Aldeas Consagradas y Asentamientos Arrasados
        if (gameTime % 20 == 0) {
            GodVillageSavedData.get(serverLevel).getVillageAt(player.blockPosition()).ifPresent(v -> {
                ModAdvancements.award(player, "village_discovery");
                if (v.state() == GodVillageState.ARRASADA || v.patronGod() == SinGod.ENVY) {
                    ModAdvancements.award(player, "secret_ruined_envy_village");
                }
            });
        }
    }

    @SubscribeEvent
    public void onLivingKill(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();

        // Comprobar si la victima es el jugador y si un dios Campeon (+91 a +100) lo salva de la muerte
        if (victim instanceof ServerPlayer dyingPlayer && dyingPlayer.level() instanceof ServerLevel serverLevel) {
            if (tryMiraculousSaveFromDeath(serverLevel, dyingPlayer, event)) {
                return;
            }
            // Si muere de forma humillante (caida, ahogamiento, cactus), Lucian (Soberbia) se disgusta
            if (event.getSource().is(DamageTypeTags.IS_FALL)
                    || event.getSource().is(DamageTypeTags.IS_DROWNING)) {
                SinFavorManager.addEffort(dyingPlayer, SinGod.PRIDE, -1.0F, "Muerte humillante");
            }
            return;
        }

        Entity killerEntity = event.getSource().getEntity();
        if (!(killerEntity instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        PlayerSinData data = SinFavorManager.get(player);
        data.setLastWrathKillTick(serverLevel.getGameTime());

        // 1. Khorvath (Ira): Matar monstruos hostiles complace a la Ira y alimenta altares de Ira cercanos
        if (victim instanceof Monster) {
            float wrathGain = (player.getHealth() <= player.getMaxHealth() * 0.4F) ? 0.85F : 0.4F;
            SinFavorManager.addEffort(player, SinGod.WRATH, wrathGain, "Derramamiento de sangre");
            notifyNearbyWrathAltar(serverLevel, player, victim);

            // Si mato un enemigo poderoso en solitario, Lucian (Soberbia) tambien se complace
            if (victim.getMaxHealth() >= 40.0F) {
                SinFavorManager.addEffort(player, SinGod.PRIDE, 0.75F, "Victoria gloriosa");
            }
        }

        // 2. Asmodeus (Lujuria): Matar crias de animales le enfurece (pero alimenta altares de Ira si estan cerca)
        if (victim instanceof AgeableMob ageable && ageable.isBaby()) {
            SinFavorManager.addEffort(player, SinGod.LUST, -1.0F, "Matar una cria inocente");
            notifyNearbyWrathAltar(serverLevel, player, victim);
        }

        // 3. Leviatan (Envidia): Matar aldeanos o Enviados/Acolitos de otros dioses le complace
        if (victim instanceof Villager) {
            SinFavorManager.addEffort(player, SinGod.ENVY, 0.6F, "Despojar a los aldeanos");
            SinFavorManager.addEffort(player, SinGod.LUST, -0.5F, "Crueldad contra aldeanos");
        } else if (victim instanceof GodAcolyteEntity acolyte && acolyte.getGod() != SinGod.ENVY) {
            SinFavorManager.addEffort(player, SinGod.ENVY, 1.0F, "Abatir al siervo de un dios rival");
        }
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        Entity attacker = event.getSource().getEntity();

        // Efectos activos de Bendicion de Ritual (+61 Favor) cuando el jugador ataca
        if (attacker instanceof ServerPlayer player) {
            if (player.hasEffect(ModMobEffects.WRATH_BLESSING)) {
                // Robo de vida del Frenesi de Khorvath
                player.heal(Math.min(3.0F, event.getHealthDamage() * 0.25F));
            }
            if (player.hasEffect(ModMobEffects.ENVY_BLESSING) && (target.getArmorValue() > 4 || !target.getActiveEffects().isEmpty())) {
                // Dano abisal extra de Leviatan contra objetivos protegidos
                if (player.level() instanceof ServerLevel serverLevel) {
                    target.hurtServer(serverLevel, player.damageSources().magic(), 3.0F);
                }
            }

            // Detección de Alta Traición: atacar Acólitos o Aldeanos de un dios con el que se tiene Favor >= 0
            if (player.level() instanceof ServerLevel serverLevel) {
                checkHighTreasonOnAttack(serverLevel, player, target);
            }
        }

        // Si el jugador recibe dano: comprobar intervencion de Acolitos Aliados (>= +85) y Guerras de Dioses
        if (target instanceof ServerPlayer player && player.level() instanceof ServerLevel serverLevel) {
            boolean attackedByRivalGodServant = isRivalGodServant(attacker);
            boolean lowHealthInCombat = player.getHealth() <= player.getMaxHealth() * 0.35F && attacker instanceof LivingEntity;

            if (attackedByRivalGodServant || lowHealthInCombat) {
                DivineInterventionScheduler.trySpawnAlliedAcolytesForDefense(
                        serverLevel,
                        player,
                        attacker instanceof LivingEntity livingAttacker ? livingAttacker : null,
                        attackedByRivalGodServant
                );
            }
        }
    }

    @SubscribeEvent
    public void onShieldBlock(LivingShieldBlockEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Khorvath (Ira) desprecia el uso defensivo de escudos
            SinFavorManager.addEffort(player, SinGod.WRATH, -0.25F, "Esconderse tras un escudo");
        }
    }

    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Al dormir, se reinicia el contador de bloques trabajados (rotos/colocados) de Belphegor
            PlayerSinData data = SinFavorManager.get(player);
            data.setSlothLaborBlocks(0);

            // Dormir complace a Belphegor (Pereza), pero enfada a Khorvath (Ira) por esquivar la noche
            SinFavorManager.addEffort(player, SinGod.SLOTH, 0.6F, "Descanso placentero");
            SinFavorManager.addEffort(player, SinGod.WRATH, -0.5F, "Dormir para huir de la batalla nocturna");

            if (player.level() instanceof ServerLevel serverLevel) {
                GodVillageSavedData.get(serverLevel).getVillageAt(player.blockPosition()).ifPresent(v -> {
                    if (v.patronGod() == SinGod.SLOTH) {
                        ModAdvancements.award(player, "secret_sloth_society");
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ItemStack item = event.getItem();
        if (item.has(DataComponents.FOOD)) {
            if (item.is(Items.ROTTEN_FLESH) || item.is(Items.SPIDER_EYE) || item.is(Items.POISONOUS_POTATO)) {
                SinFavorManager.addEffort(player, SinGod.GLUTTONY, -0.8F, "Ingerir inmundicia");
            } else if (item.is(Items.GOLDEN_APPLE)
                    || item.is(Items.ENCHANTED_GOLDEN_APPLE)
                    || item.is(Items.GOLDEN_CARROT)
                    || item.is(Items.RABBIT_STEW)
                    || item.is(Items.MUSHROOM_STEW)
                    || item.is(Items.COOKED_BEEF)
                    || item.is(Items.COOKED_PORKCHOP)) {
                SinFavorManager.addEffort(player, SinGod.GLUTTONY, 0.45F, "Degustar un buen manjar");
                notifyNearbyGluttonyAltar(serverLevel, player, item);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.WRITTEN_BOOK)) {
            return;
        }
        handleLoreBookRead(player, stack);
    }

    @SubscribeEvent
    public void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof LecternBlockEntity lectern) {
            ItemStack book = lectern.getBook();
            if (!book.isEmpty() && book.is(Items.WRITTEN_BOOK)) {
                handleLoreBookRead(player, book);
            }
        }
    }

    private void handleLoreBookRead(ServerPlayer player, ItemStack stack) {
        SinLoreLibrary.getEntryFromBook(stack).ifPresent(entry -> {
            unlockGodCult(player, entry.god());
            PlayerSinData data = SinFavorManager.get(player);
            if (data.markLoreBookRead(entry.id())) {
                int xpAmount = ModConfig.LORE_BOOK_FIRST_READ_XP.get();
                if (xpAmount > 0) {
                    player.giveExperiencePoints(xpAmount);
                }

                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            ParticleTypes.ENCHANT,
                            player.getX(), player.getY() + 1.0D, player.getZ(),
                            25, 0.4D, 0.5D, 0.4D, 0.1D
                    );
                    serverLevel.playSound(
                            null,
                            player.blockPosition(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP,
                            SoundSource.PLAYERS,
                            0.8F,
                            1.0F
                    );
                }

                Component titleComp = Component.translatable(entry.titleKey())
                        .withStyle(entry.god().getChatColor(), ChatFormatting.ITALIC);
                Component feedbackText = (xpAmount > 0)
                        ? Component.translatable("message.sevendeadlygods.lore_book_read_xp", titleComp, xpAmount).withStyle(ChatFormatting.GRAY)
                        : Component.translatable("message.sevendeadlygods.lore_book_read", titleComp).withStyle(ChatFormatting.GRAY);

                Component message = Component.literal("✦ ")
                        .withStyle(ChatFormatting.GOLD)
                        .append(feedbackText);

                player.sendSystemMessage(message);

                int readCount = data.getReadLoreBooksCount();
                if (readCount >= 10) {
                    ModAdvancements.award(player, "lore_collector_1");
                }
                if (readCount >= 25) {
                    ModAdvancements.award(player, "lore_collector_2");
                }
                if (readCount >= 40) {
                    ModAdvancements.award(player, "lore_collector_3");
                }
            }
        });
    }

    @SubscribeEvent
    public void onBlockBreak(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockState state = event.getState();

        // Romper bloques (minar, talar, cosechar/cultivar) incrementa el contador de fatiga laboral de Belphegor (Pereza)
        recordSlothLaborAction(player);

        // Detección de Alta Traición: profanar o romper el Altar Mayor de una aldea consagrada
        if (state.getBlock() instanceof SinAltarBlock altarBlock) {
            GodVillageSavedData villageData = GodVillageSavedData.get(serverLevel);
            if (villageData.isCanonicalVillageAltar(event.getPos())) {
                SinGod altarGod = altarBlock.getGod();
                if (SinFavorManager.getFavor(player, altarGod) >= 0) {
                    applyHighTreasonPenalty(serverLevel, player, altarGod, "Profanación del Altar Mayor de su aldea");
                }
            }
        }

        // Romper flores enfada a Asmodeus (Lujuria)
        if (state.is(BlockTags.FLOWERS)) {
            SinFavorManager.addEffort(player, SinGod.LUST, -0.4F, "Destruir la belleza floral");
        }

        // Picar minerales con la bendicion de Mammon (+61) puede dar pepitas de oro extra
        if (player.hasEffect(ModMobEffects.GREED_BLESSING) && state.is(BlockTags.ORES)) {
            if (player.getRandom().nextFloat() < 0.35F) {
                net.minecraft.world.level.block.Block.popResource(
                        serverLevel,
                        event.getPos(),
                        new ItemStack(Items.GOLD_NUGGET, 1 + player.getRandom().nextInt(3))
                );
            }
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            // Colocar bloques (construccion o siembra) tambien incrementa el contador de fatiga laboral de Belphegor (Pereza)
            recordSlothLaborAction(player);
        }
    }

    /**
     * Registra un bloque roto o colocado por el jugador.
     * Cada 100 bloques acumulados sin haber dormido, existe una probabilidad de penalizar
     * el favor con Belphegor (Pereza), escalando con el cansancio acumulado.
     */
    private static void recordSlothLaborAction(ServerPlayer player) {
        PlayerSinData data = SinFavorManager.get(player);
        int laborBlocks = data.incrementSlothLaborBlocks();

        if (laborBlocks > 0 && laborBlocks % 100 == 0) {
            int hundreds = laborBlocks / 100;
            // Probabilidad de penalizacion: 35% a los 100 bloques, subiendo +15% por cada centena sin dormir (hasta 80%)
            float penaltyChance = Math.min(0.80F, 0.35F + (hundreds - 1) * 0.15F);
            if (player.getRandom().nextFloat() < penaltyChance) {
                float penaltyEffort = -(1.0F + 0.25F * Math.min(4, hundreds - 1));
                SinFavorManager.addEffort(
                        player,
                        SinGod.SLOTH,
                        penaltyEffort,
                        "Exceso de trabajo sin descanso (" + laborBlocks + " bloques)"
                );
            }
        }
    }

    @SubscribeEvent
    public void onBabySpawn(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer player && player.level() instanceof ServerLevel serverLevel) {
            // Reproducir criaturas complace a Asmodeus (Lujuria) y activa su altar si esta cerca
            SinFavorManager.addEffort(player, SinGod.LUST, 0.7F, "Engendrar nueva vida");
            notifyNearbyLustAltar(serverLevel, player);
        }
    }

    @SubscribeEvent
    public void onVillagerTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // El comercio justo molesta a Leviatan (Envidia), que prefiere el saqueo
            SinFavorManager.addEffort(player, SinGod.ENVY, -0.35F, "Comercio justo en lugar de despojo");
        }
    }

    private static boolean isWearingOrCarryingValuables(ServerPlayer player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack armor = player.getItemBySlot(slot);
            String id = armor.getItem().toString();
            if (id.contains("diamond") || id.contains("netherite") || id.contains("golden")) {
                return true;
            }
        }

        int valuableCount = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.DIAMOND) || stack.is(Items.EMERALD)
                    || stack.is(Items.NETHERITE_INGOT) || stack.is(Items.NETHERITE_BLOCK)
                    || stack.is(Items.DIAMOND_BLOCK) || stack.is(Items.EMERALD_BLOCK)
                    || stack.is(Items.GOLD_BLOCK) || stack.is(Items.RAW_GOLD_BLOCK)
                    || stack.is(Items.AMETHYST_BLOCK)) {
                valuableCount += stack.getCount();
                if (valuableCount >= 4) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasArmorTrimEquipped(ServerPlayer player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            if (player.getItemBySlot(slot).has(DataComponents.TRIM)) {
                return true;
            }
        }
        return false;
    }

    private static void checkHighTreasonOnAttack(ServerLevel level, ServerPlayer attacker, LivingEntity victim) {
        if (victim instanceof GodAcolyteEntity acolyte) {
            SinGod god = acolyte.getGod();
            if (SinFavorManager.getFavor(attacker, god) >= 0) {
                applyHighTreasonPenalty(level, attacker, god, "Atacar a un Acólito sagrado de su culto");
            }
        } else if (victim instanceof Villager) {
            GodVillageSavedData.get(level).findVillageAt(victim.blockPosition()).ifPresent(village -> {
                if (village.state() != GodVillageState.ARRASADA) {
                    SinGod god = village.patronGod();
                    if (SinFavorManager.getFavor(attacker, god) >= 0) {
                        applyHighTreasonPenalty(level, attacker, god, "Derramar sangre inocente en su aldea consagrada");
                    }
                }
            });
        }
    }

    public static void applyHighTreasonPenalty(ServerLevel level, ServerPlayer player, SinGod betrayedGod, String reason) {
        SinFavorManager.addDirectFavor(player, betrayedGod, -60, "Alta Traición: " + reason);
        ModAdvancements.award(player, "secret_high_treason");
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5F, 0.7F);
        player.sendSystemMessage(
                betrayedGod.getFormattedPrefix().append(
                        Component.translatable("message.sevendeadlygods.favor.high_treason_rebuke")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                )
        );
    }

    private static boolean isRivalGodServant(Entity entity) {
        if (entity instanceof GodAcolyteEntity acolyte && !acolyte.isAlliedProtector()) {
            return true;
        }
        if (entity instanceof GodAvatarEntity avatar && avatar.isWrathHunter()) {
            return true;
        }
        if (entity != null) {
            if (entity.entityTags().contains("sevendeadlygods:god_envoy")) {
                return true;
            }
            if (entity.getCustomName() != null) {
                String name = entity.getCustomName().getString();
                return name.startsWith("Enviado de ") || name.startsWith("Envoy of ");
            }
        }
        return false;
    }

    /**
     * Intervencion milagrosa exclusiva de Campeones Divinos (+91 a +100 de favor):
     * El dios salva al jugador de un golpe letal.
     */
    private static boolean tryMiraculousSaveFromDeath(ServerLevel level, ServerPlayer player, LivingDeathEvent event) {
        PlayerSinData data = SinFavorManager.get(player);
        long now = level.getGameTime();

        for (SinGod god : SinGod.values()) {
            if (data.getFavor(god) >= 91 && now >= data.getNextAvatarManifestTick(god)) {
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth() * 0.6F);
                player.removeAllEffects();
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 2));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 3));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0));

                // Enfriamiento de 1 dia de Minecraft para salvacion de muerte
                data.setNextAvatarManifestTick(god, now + 24000L);

                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        60, 0.6D, 0.8D, 0.6D, 0.3D);
                level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

                player.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.translatable("message.sevendeadlygods.intervention.champion_saved")
                                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC))
                );
                return true;
            }
        }
        return false;
    }

    private static void notifyNearbyWrathAltar(ServerLevel level, ServerPlayer player, LivingEntity victim) {
        SinAltarBlockEntity altar = findNearbyAltar(level, player.blockPosition(), SinGod.WRATH, 7);
        if (altar != null) {
            altar.onWrathBloodSpilled(level, player, victim);
        }
    }

    private static void notifyNearbyGluttonyAltar(ServerLevel level, ServerPlayer player, ItemStack food) {
        SinAltarBlockEntity altar = findNearbyAltar(level, player.blockPosition(), SinGod.GLUTTONY, 6);
        if (altar != null) {
            altar.onGluttonyFeastBite(level, player, food);
        }
    }

    private static void notifyNearbySlothAltar(ServerLevel level, ServerPlayer player) {
        SinAltarBlockEntity altar = findNearbyAltar(level, player.blockPosition(), SinGod.SLOTH, 5);
        if (altar != null) {
            altar.onSlothStillnessCompleted(level, player);
        }
    }

    private static void notifyNearbyLustAltar(ServerLevel level, ServerPlayer player) {
        SinAltarBlockEntity altar = findNearbyAltar(level, player.blockPosition(), SinGod.LUST, 7);
        if (altar != null) {
            altar.onLustLifeCreated(level, player);
        }
    }

    public static SinAltarBlockEntity findNearbyAltar(ServerLevel level, BlockPos center, SinGod targetGod, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -3, -radius),
                center.offset(radius, 3, radius))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SinAltarBlockEntity altar && (targetGod == null || altar.getGod() == targetGod)) {
                return altar;
            }
        }
        return null;
    }

    public static void unlockGodCult(ServerPlayer player, SinGod god) {
        Identifier advId = Identifier.fromNamespaceAndPath(
                SevenDeadlyGodsMod.MOD_ID,
                "cult_" + god.getSerializedName()
        );

        AdvancementHolder adv = player.level().getServer().getAdvancements().get(advId);
        if (adv != null) {
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(adv, criterion);
                }

                player.level().playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS,
                        0.8F,
                        1.2F
                );

                player.sendSystemMessage(
                        god.getFormattedPrefix()
                                .append(Component.translatable("message.sevendeadlygods.cult_unlocked", god.getSinNameComponent())
                                        .withStyle(ChatFormatting.GRAY))
                );
            }
        }
    }
}
