package com.volcov.sevendeadlygods.block;

import com.volcov.sevendeadlygods.advancement.ModAdvancements;
import com.volcov.sevendeadlygods.entity.GodAvatarEntity;
import com.volcov.sevendeadlygods.favor.PlayerSinData;
import com.volcov.sevendeadlygods.favor.SinFavorManager;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.intervention.DivineInterventionScheduler;
import com.volcov.sevendeadlygods.registry.ModBlockEntities;
import com.volcov.sevendeadlygods.registry.ModMobEffects;
import com.volcov.sevendeadlygods.village.GodVillageRaidManager;
import com.volcov.sevendeadlygods.village.GodVillageSavedData;
import com.volcov.sevendeadlygods.village.GodVillageState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

/**
 * BlockEntity que gestiona los 7 Rituales Asimetricos de cada Dios del Pecado,
 * asi como la concesion de bendiciones de larga duracion (20-40 min) a partir de +61 de favor.
 */
public class SinAltarBlockEntity extends BlockEntity {
    private static final float VILLAGE_ALTAR_EFFORT_MULTIPLIER = 1.20F;

    private SinGod god;
    private int ritualCharges = 0;
    private boolean awaitingCondition = false;
    private boolean villageShrine = false;

    public SinAltarBlockEntity(BlockPos pos, BlockState blockState) {
        this(pos, blockState, blockState.getBlock() instanceof SinAltarBlock altar ? altar.getGod() : SinGod.GREED);
    }

    public SinAltarBlockEntity(BlockPos pos, BlockState blockState, SinGod god) {
        super(ModBlockEntities.SIN_ALTAR_BE.get(), pos, blockState);
        this.god = god;
    }

    public SinGod getGod() {
        if (this.god == null && getBlockState().getBlock() instanceof SinAltarBlock altar) {
            this.god = altar.getGod();
        }
        return this.god != null ? this.god : SinGod.GREED;
    }

    public boolean isVillageShrine() {
        return this.villageShrine;
    }

    public void setVillageShrine(boolean villageShrine) {
        this.villageShrine = villageShrine;
        setChanged();
        tryRegisterVillageShrine();
    }

    public boolean isCanonicalVillageAltar(ServerLevel level) {
        return GodVillageSavedData.get(level).isCanonicalVillageAltar(this.worldPosition);
    }

    /**
     * Si el Altar Mayor pertenece a una aldea en estado DESOLADA, permite que un jugador devoto (Favor >= +30)
     * deposite una ofrenda de restauración para invocar de inmediato la Caravana de Peregrinos.
     */
    public boolean tryRevitalizeDesolatedVillage(ServerLevel serverLevel, ServerPlayer player, ItemStack offering) {
        GodVillageSavedData data = GodVillageSavedData.get(serverLevel);
        Optional<GodVillageSavedData.GodVillageRecord> opt = data.getVillageAt(this.worldPosition);
        if (opt.isEmpty() || opt.get().state() != GodVillageState.DESOLADA || !opt.get().altarPos().equals(this.worldPosition)) {
            return false;
        }

        GodVillageSavedData.GodVillageRecord village = opt.get();
        SinGod altarGod = village.patronGod();
        int favor = SinFavorManager.getFavor(player, altarGod);

        if (favor < 30) {
            player.sendSystemMessage(
                    altarGod.getFormattedPrefix().append(
                            Component.translatable("altar.sevendeadlygods.desolate_insufficient_favor")
                                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
                    )
            );
            return true;
        }

        if (!isValidRevitalizationOffering(altarGod, offering)) {
            player.sendSystemMessage(
                    altarGod.getFormattedPrefix().append(
                            Component.translatable("altar.sevendeadlygods.desolate_prompt")
                                    .withStyle(ChatFormatting.YELLOW)
                    )
            );
            return true;
        }

        offering.shrink(1);
        int pilgrimCount = (altarGod == SinGod.LUST) ? 4 : 3;
        GodVillageRaidManager.spawnPilgrimCaravan(serverLevel, this.worldPosition, altarGod, pilgrimCount);
        data.updateVillage(village.withState(GodVillageState.CONSAGRADA, serverLevel.getGameTime()));

        SinFavorManager.addEffort(player, altarGod, 4.0F, "Milagro de Restauración del Santuario de " + altarGod.getDisplayName());
        ModAdvancements.award(player, "village_miracle_restore");

        serverLevel.sendParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 1.2D,
                worldPosition.getZ() + 0.5D,
                50, 0.6D, 0.6D, 0.6D, 0.15D
        );
        serverLevel.playSound(null, worldPosition, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 1.2F, 1.0F);

        player.sendSystemMessage(
                altarGod.getFormattedPrefix().append(
                        Component.translatable("altar.sevendeadlygods.miracle_restored")
                                .withStyle(altarGod.getChatColor(), ChatFormatting.BOLD)
                )
        );
        return true;
    }

    private static boolean isValidRevitalizationOffering(SinGod god, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return switch (god) {
            case GREED -> evaluateGreedValue(stack) > 0.0F;
            case WRATH -> stack.is(Items.BLAZE_POWDER) || stack.is(Items.NETHERITE_SCRAP) || stack.is(Items.DIAMOND) || stack.is(Items.IRON_INGOT);
            case GLUTTONY -> stack.is(Items.GOLDEN_APPLE) || stack.is(Items.CAKE) || stack.is(Items.GOLDEN_CARROT) || stack.is(Items.COOKED_BEEF);
            case PRIDE -> stack.is(Items.GOLD_INGOT) || stack.is(Items.DIAMOND) || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.AMETHYST_SHARD);
            case ENVY -> stack.is(Items.EMERALD) || stack.is(Items.ENDER_PEARL) || stack.is(Items.TOTEM_OF_UNDYING) || stack.is(Items.GOLDEN_APPLE);
            case SLOTH -> stack.is(Items.CLOCK) || stack.is(ItemTags.WOOL) || stack.is(Items.FEATHER) || stack.is(Items.ENDER_PEARL);
            case LUST -> stack.is(Items.AMETHYST_SHARD) || stack.is(Items.ROSE_BUSH) || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.HONEYCOMB);
        };
    }

    private void addRitualEffort(ServerLevel level, ServerPlayer player, SinGod targetGod, float baseEffort, String reason) {
        float finalEffort = isCanonicalVillageAltar(level)
                ? baseEffort * VILLAGE_ALTAR_EFFORT_MULTIPLIER
                : baseEffort;
        SinFavorManager.addEffort(player, targetGod, finalEffort, reason);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        tryRegisterVillageShrine();
    }

    private void tryRegisterVillageShrine() {
        if (this.villageShrine && this.level instanceof ServerLevel serverLevel) {
            GodVillageSavedData.get(serverLevel)
                    .registerVillageIfAbsent(this.worldPosition, getGod(), serverLevel.getGameTime());
        }
    }

    public InteractionResult handlePlayerInteraction(
            ServerLevel level,
            ServerPlayer player,
            ItemStack heldStack,
            InteractionHand hand
    ) {
        SinGod altarGod = getGod();
        PlayerSinData data = SinFavorManager.get(player);
        long gameTime = level.getGameTime();

        if (data != null && data.isGodInSlumber(altarGod, gameTime)) {
            level.playSound(null, getBlockPos(), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.6F);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getBlockPos().getX() + 0.5D, getBlockPos().getY() + 1.0D, getBlockPos().getZ() + 0.5D, 20, 0.3D, 0.4D, 0.3D, 0.02D);
            level.sendParticles(ParticleTypes.SOUL, getBlockPos().getX() + 0.5D, getBlockPos().getY() + 0.8D, getBlockPos().getZ() + 0.5D, 12, 0.25D, 0.3D, 0.25D, 0.01D);

            player.sendSystemMessage(
                    altarGod.getFormattedPrefix().append(
                            Component.translatable("altar.sevendeadlygods.avatar_slumber", altarGod.getDisplayNameComponent())
                                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                    )
            );
            return InteractionResult.SUCCESS;
        }

        if (tryRevitalizeDesolatedVillage(level, player, heldStack)) {
            return InteractionResult.SUCCESS;
        }

        return switch (altarGod) {
            case GREED -> handleGreedRitual(level, player, heldStack);
            case WRATH -> handleWrathRitual(level, player, heldStack);
            case GLUTTONY -> handleGluttonyRitual(level, player, heldStack);
            case PRIDE -> handlePrideRitual(level, player, heldStack);
            case ENVY -> handleEnvyRitual(level, player, heldStack);
            case SLOTH -> handleSlothRitual(level, player, heldStack);
            case LUST -> handleLustRitual(level, player, heldStack);
        };
    }

    /**
     * 1. AVARICIA (Mammon): Tributo de Riqueza.
     * Ofrecer minerales y joyas calman la codicia de Mammon y permiten portar armaduras ricas sin castigo.
     */
    private InteractionResult handleGreedRitual(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (stack.is(Items.RAW_GOLD) || stack.is(Items.RAW_GOLD_BLOCK)) {
            player.sendSystemMessage(SinGod.GREED.getFormattedPrefix()
                    .append(Component.translatable("altar.sevendeadlygods.greed.raw_ore")
                            .withStyle(ChatFormatting.GOLD)));
            return InteractionResult.SUCCESS;
        }

        float tributeValue = evaluateGreedValue(stack);
        if (tributeValue <= 0.0F) {
            player.sendSystemMessage(SinGod.GREED.getFormattedPrefix()
                    .append(Component.translatable("altar.sevendeadlygods.greed.trinkets")
                            .withStyle(ChatFormatting.YELLOW)));
            return InteractionResult.SUCCESS;
        }

        stack.shrink(1);
        PlayerSinData data = SinFavorManager.get(player);
        data.setLastGreedTributeTick(level.getGameTime());

        addRitualEffort(level, player, SinGod.GREED, tributeValue, "Tributo de riqueza en el Altar de Mammon");
        completeRitual(level, player, SinGod.GREED,
                "«Acepto tu diezmo áureo, mortal. Por hoy no te castigaré por las joyas que vistes.»");
        return InteractionResult.SUCCESS;
    }

    private static float evaluateGreedValue(ItemStack stack) {
        // Bloque de Netherite: 9 lingotes (24.75) + ~5% bono de bloque = 26.0F (tope supremo balanceado)
        if (stack.is(Items.NETHERITE_BLOCK)) {
            return 26.0F;
        }
        // Bloque de Diamante: 9 diamantes (7.20) + ~5% bono = 7.60F
        if (stack.is(Items.DIAMOND_BLOCK)) {
            return 7.60F;
        }
        // Lingote de Netherite: 4 fragmentos (2.20) + 4 lingotes de oro (0.40) + ~5% bono de forja = 2.75F
        if (stack.is(Items.NETHERITE_INGOT)) {
            return 2.75F;
        }
        // Bloque de Oro: 9 lingotes de oro (0.90) + ~5% bono = 0.95F
        if (stack.is(Items.GOLD_BLOCK)) {
            return 0.95F;
        }
        // Bloque de Amatista: 4 fragmentos de amatista (0.80) + ~5% bono = 0.85F
        if (stack.is(Items.AMETHYST_BLOCK)) {
            return 0.85F;
        }
        // Diamante suelto: gema noble de alto valor y escasa generacion
        if (stack.is(Items.DIAMOND)) {
            return 0.80F;
        }
        // Fragmento de Netherite: residuo ancestral raro
        if (stack.is(Items.NETHERITE_SCRAP)) {
            return 0.55F;
        }
        // Bloque de Esmeralda: 9 esmeraldas (0.45) + ~5% bono = 0.48F (por debajo del bloque de oro)
        if (stack.is(Items.EMERALD_BLOCK)) {
            return 0.48F;
        }
        // Fragmento de Amatista suelto: gema de geoda (mas rara que el oro comun)
        if (stack.is(Items.AMETHYST_SHARD)) {
            return 0.20F;
        }
        // Lingote de Oro: unidad aurea basica refinada
        if (stack.is(Items.GOLD_INGOT)) {
            return 0.10F;
        }
        // Esmeralda suelta: gema comun farmeable con aldeanos (por debajo del oro)
        if (stack.is(Items.EMERALD)) {
            return 0.05F;
        }
        return 0.0F;
    }

    /**
     * 2. IRA (Khorvath): Ritual de Sangre.
     * No acepta items pasivos: el jugador ofrece su propia sangre (8 HP) o mata criaturas junto al altar.
     */
    private InteractionResult handleWrathRitual(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (player.getHealth() <= 8.5F) {
            player.sendSystemMessage(SinGod.WRATH.getFormattedPrefix()
                    .append(Component.translatable("altar.sevendeadlygods.wrath.too_weak")
                            .withStyle(ChatFormatting.RED)));
            return InteractionResult.SUCCESS;
        }

        player.hurtServer(level, level.damageSources().magic(), 8.0F);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                worldPosition.getX() + 0.5D, worldPosition.getY() + 1.1D, worldPosition.getZ() + 0.5D,
                20, 0.3D, 0.3D, 0.3D, 0.1D);

        addRitualEffort(level, player, SinGod.WRATH, 1.2F, "Pacto de sangre en el Altar de Khorvath");
        completeRitual(level, player, SinGod.WRATH,
                "«¡Tu propia sangre hierve sobre la piedra! ¡Sal y despedaza a tus enemigos en mi nombre!»");
        return InteractionResult.SUCCESS;
    }

    public void onWrathBloodSpilled(ServerLevel level, ServerPlayer player, LivingEntity victim) {
        this.ritualCharges++;
        level.sendParticles(ParticleTypes.LAVA,
                worldPosition.getX() + 0.5D, worldPosition.getY() + 1.1D, worldPosition.getZ() + 0.5D,
                15, 0.25D, 0.25D, 0.25D, 0.05D);
        setChanged();

        if (this.ritualCharges >= 3) {
            this.ritualCharges = 0;
            addRitualEffort(level, player, SinGod.WRATH, 1.5F, "Ritual de Carniceria completado");
            completeRitual(level, player, SinGod.WRATH,
                    "«¡Tres almas segadas ante mi altar! ¡Que mi furia corra por tus venas!»");
        }
    }

    /**
     * 3. GULA (Beelze): El Gran Banquete.
     * Ofrecer manjares variados o comer compulsivamente frente al altar.
     */
    private InteractionResult handleGluttonyRitual(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.CAKE) || stack.is(Items.PUMPKIN_PIE)
                || stack.is(Items.COOKED_BEEF) || stack.is(Items.COOKED_PORKCHOP) || stack.is(Items.GOLDEN_CARROT)
                || stack.is(Items.HONEY_BOTTLE) || stack.is(Items.BREAD)) {
            stack.shrink(1);
            this.ritualCharges++;
            setChanged();

            level.playSound(null, worldPosition, SoundEvents.GENERIC_EAT.value(), SoundSource.BLOCKS, 1.0F, 0.8F);
            if (this.ritualCharges >= 3) {
                this.ritualCharges = 0;
                addRitualEffort(level, player, SinGod.GLUTTONY, 1.4F, "Gran Banquete de Beelze");
                completeRitual(level, player, SinGod.GLUTTONY,
                        "«¡Un festín digno de mi fauces! ¡Come y no conozcas jamás el vacío del hambre!»");
            } else {
                player.sendSystemMessage(SinGod.GLUTTONY.getFormattedPrefix()
                        .append(Component.translatable("altar.sevendeadlygods.gluttony.progress", this.ritualCharges)
                                .withStyle(ChatFormatting.GOLD)));
            }
            return InteractionResult.SUCCESS;
        }

        player.sendSystemMessage(SinGod.GLUTTONY.getFormattedPrefix()
                .append(Component.translatable("altar.sevendeadlygods.gluttony.hint")
                        .withStyle(ChatFormatting.GOLD)));
        return InteractionResult.SUCCESS;
    }

    public void onGluttonyFeastBite(ServerLevel level, ServerPlayer player, ItemStack food) {
        this.ritualCharges++;
        setChanged();
        if (this.ritualCharges >= 3) {
            this.ritualCharges = 0;
            addRitualEffort(level, player, SinGod.GLUTTONY, 1.4F, "Banquete voraz frente al Altar de Gula");
            completeRitual(level, player, SinGod.GLUTTONY,
                    "«¡Así me gusta, devorando sin mesura ante mi altar! Recibe mi bendición voraz.»");
        }
    }

    /**
     * 4. SOBERBIA (Lucian): Altar de las Alturas (Y > 120 a cielo abierto, o Catedral Consagrada de Aldea, + agacharse + alabar en el chat).
     */
    private InteractionResult handlePrideRitual(ServerLevel level, ServerPlayer player, ItemStack stack) {
        boolean isVillageCathedral = isCanonicalVillageAltar(level);
        if (!isVillageCathedral && (worldPosition.getY() <= 120 || !level.canSeeSky(worldPosition.above()))) {
            player.sendSystemMessage(SinGod.PRIDE.getFormattedPrefix()
                    .append(Component.translatable("altar.sevendeadlygods.pride.low_altitude")
                            .withStyle(ChatFormatting.RED)));
            return InteractionResult.SUCCESS;
        }

        if (!player.isCrouching()) {
            player.sendSystemMessage(SinGod.PRIDE.getFormattedPrefix()
                    .append(Component.translatable("altar.sevendeadlygods.pride.not_kneeling")
                            .withStyle(ChatFormatting.RED)));
            SinFavorManager.addEffort(player, SinGod.PRIDE, -0.5F, "Falta de reverencia ante el Altar de Soberbia");
            return InteractionResult.SUCCESS;
        }

        if (stack.is(Items.GOLD_INGOT) || stack.is(Items.DIAMOND) || stack.is(Items.GOLDEN_APPLE)
                || stack.is(Items.NETHER_STAR) || stack.is(Items.DRAGON_BREATH) || stack.is(Items.AMETHYST_SHARD)) {
            stack.shrink(1);
            this.awaitingCondition = true;
            setChanged();
            player.sendSystemMessage(SinGod.PRIDE.getFormattedPrefix()
                    .append(Component.translatable("altar.sevendeadlygods.pride.offer_accepted")
                            .withStyle(ChatFormatting.YELLOW)));
            return InteractionResult.SUCCESS;
        }

        player.sendSystemMessage(SinGod.PRIDE.getFormattedPrefix()
                .append(Component.translatable("altar.sevendeadlygods.pride.hint")
                        .withStyle(ChatFormatting.YELLOW)));
        return InteractionResult.SUCCESS;
    }

    public void onPridePraiseSpoken(ServerLevel level, ServerPlayer player, String chatMessage) {
        if (!this.awaitingCondition) {
            return;
        }
        boolean isVillageCathedral = isCanonicalVillageAltar(level);
        if ((!isVillageCathedral && worldPosition.getY() <= 120) || !player.isCrouching()) {
            player.sendSystemMessage(SinGod.PRIDE.getFormattedPrefix()
                    .append(Component.translatable("altar.sevendeadlygods.pride.praise_hint")
                            .withStyle(ChatFormatting.RED)));
            return;
        }
        this.awaitingCondition = false;
        setChanged();
        addRitualEffort(level, player, SinGod.PRIDE, 1.6F, "Alabanza imperial en las cumbres");
        completeRitual(level, player, SinGod.PRIDE,
                "«Tus palabras de sumisión complacen mi grandeza. Camina bajo el resplandor de mi sol.»");
    }

    /**
     * 5. ENVIDIA (Leviatan): Ritual de Despojo.
     * Sacrificar objetos saqueados de aldeanos/estructuras (esmeraldas, libros encantados, monturas, campanas, etc.).
     */
    private InteractionResult handleEnvyRitual(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (stack.is(Items.EMERALD) || stack.is(Items.ENCHANTED_BOOK) || stack.is(Items.SADDLE)
                || stack.is(Items.NAME_TAG) || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.TOTEM_OF_UNDYING)
                || stack.is(Items.BELL) || stack.is(Items.EXPERIENCE_BOTTLE) || stack.is(Items.ENDER_PEARL)) {
            stack.shrink(1);
            addRitualEffort(level, player, SinGod.ENVY, 1.4F, "Sacrificio de despojo en el Altar de Leviatan");
            completeRitual(level, player, SinGod.ENVY,
                    "«Si no es nuestro, que no sea de nadie. Las aguas abisales consumen tu botín robado.»");
            return InteractionResult.SUCCESS;
        }

        player.sendSystemMessage(SinGod.ENVY.getFormattedPrefix()
                .append(Component.translatable("altar.sevendeadlygods.envy.hint")
                        .withStyle(ChatFormatting.DARK_AQUA)));
        return InteractionResult.SUCCESS;
    }

    /**
     * 6. PEREZA (Belphegor): Ritual del Letargo.
     * Ofrecer un objeto de descanso y permanecer quieto 30 segundos junto al altar.
     */
    private InteractionResult handleSlothRitual(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (stack.is(Items.CLOCK) || stack.is(ItemTags.WOOL) || stack.is(Items.ENDER_PEARL)
                || stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.FEATHER)) {
            stack.shrink(1);
            this.awaitingCondition = true;
            setChanged();
            player.sendSystemMessage(SinGod.SLOTH.getFormattedPrefix()
                    .append(Component.translatable("altar.sevendeadlygods.sloth.started")
                            .withStyle(ChatFormatting.BLUE)));
            return InteractionResult.SUCCESS;
        }

        player.sendSystemMessage(SinGod.SLOTH.getFormattedPrefix()
                .append(Component.translatable("altar.sevendeadlygods.sloth.hint")
                        .withStyle(ChatFormatting.BLUE)));
        return InteractionResult.SUCCESS;
    }

    public void onSlothStillnessCompleted(ServerLevel level, ServerPlayer player) {
        if (!this.awaitingCondition) {
            return;
        }
        this.awaitingCondition = false;
        setChanged();
        addRitualEffort(level, player, SinGod.SLOTH, 1.5F, "Ritual del Letargo completado");
        completeRitual(level, player, SinGod.SLOTH,
                "«Ves qué fácil es cuando dejas de correr... Disfruta de mi paz somnolienta.»");
    }

    /**
     * 7. LUJURIA (Asmodeus): Ritual de Extasis y Armonia.
     * Requiere flores/amatistas/bloques musicales alrededor y ofrecer amatista/flores o reproducir criaturas cerca.
     */
    private InteractionResult handleLustRitual(ServerLevel level, ServerPlayer player, ItemStack stack) {
        int beautyScore = countNearbyBeautyBlocks(level, worldPosition, 4);
        if (beautyScore < 3) {
            player.sendSystemMessage(SinGod.LUST.getFormattedPrefix()
                    .append(Component.translatable("altar.sevendeadlygods.lust.cold_environment")
                            .withStyle(ChatFormatting.LIGHT_PURPLE)));
            return InteractionResult.SUCCESS;
        }

        if (stack.is(Items.AMETHYST_SHARD) || stack.is(Items.ROSE_BUSH) || stack.is(Items.SPORE_BLOSSOM)
                || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.HONEYCOMB) || stack.is(Items.HEART_OF_THE_SEA)) {
            stack.shrink(1);
            addRitualEffort(level, player, SinGod.LUST, 1.4F, "Ofrenda de deseo y belleza a Asmodeus");
            completeRitual(level, player, SinGod.LUST,
                    "«Qué exquisita ofrenda... Tu devoción despierta mi deseo y mi favor.»");
            return InteractionResult.SUCCESS;
        }

        player.sendSystemMessage(SinGod.LUST.getFormattedPrefix()
                .append(Component.translatable("altar.sevendeadlygods.lust.hint")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)));
        return InteractionResult.SUCCESS;
    }

    public void onLustLifeCreated(ServerLevel level, ServerPlayer player) {
        if (countNearbyBeautyBlocks(level, worldPosition, 4) >= 2) {
            addRitualEffort(level, player, SinGod.LUST, 1.5F, "Ritual de Creacion de Vida ante Asmodeus");
            completeRitual(level, player, SinGod.LUST,
                    "«La pasión engendra nueva vida ante mi altar. Recibe mi caricia divina.»");
        }
    }

    private static int countNearbyBeautyBlocks(ServerLevel level, BlockPos center, int radius) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -2, -radius),
                center.offset(radius, 2, radius))) {
            BlockState st = level.getBlockState(pos);
            if (st.is(BlockTags.FLOWERS)
                    || st.is(Blocks.AMETHYST_BLOCK)
                    || st.is(Blocks.BUDDING_AMETHYST)
                    || st.is(Blocks.JUKEBOX)
                    || st.is(Blocks.NOTE_BLOCK)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Completa el ritual, emite efectos audiovisuales, concede la Bendicion Mayor de 20-40 min si Favor >= +61,
     * y si el jugador esta en +91..+100 puede manifestar benevolamente al Avatar del Dios.
     */
    private void completeRitual(ServerLevel level, ServerPlayer player, SinGod god, String godSpokenText) {
        level.sendParticles(ParticleTypes.ENCHANT,
                worldPosition.getX() + 0.5D, worldPosition.getY() + 1.5D, worldPosition.getZ() + 0.5D,
                50, 0.4D, 0.5D, 0.4D, 0.4D);
        level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 1.1F);

        int currentFavor = SinFavorManager.getFavor(player, god);
        player.sendSystemMessage(
                god.getFormattedPrefix()
                        .append(Component.literal(godSpokenText).withStyle(ChatFormatting.WHITE))
        );

        // Requisito clave: A partir de +61, cada vez que se realiza un ritual el dios concede
        // su efecto positivo especifico de larga duracion (20 a 40 minutos = 24,000 a 48,000 ticks)
        if (currentFavor >= 61) {
            int durationTicks = Mth.nextInt(level.getRandom(), 20 * 60 * 20, 40 * 60 * 20);
            Holder<MobEffect> blessingEffect = ModMobEffects.getBlessingFor(god);
            player.addEffect(new MobEffectInstance(blessingEffect, durationTicks, 0, false, true, true));

            player.sendSystemMessage(
                    god.getFormattedPrefix()
                            .append(Component.literal("✦ Bendición Mayor de Ritual concedida. El favor divino fortalece tu espíritu. ✦")
                                    .withStyle(god.getChatColor(), ChatFormatting.BOLD))
            );
        }

        // Requisito clave: Al alcanzar el favor maximo (+100), si el jugador todavia no ha recibido la reliquia del dios,
        // el Avatar desciende por primera vez para entregarsela en persona.
        // En ofrendas posteriores, una vez entregada la reliquia, el avatar no vuelve a descender (solo se renuevan bendiciones).
        if (currentFavor >= 100 && !GodAvatarEntity.hasClaimedRelic(player, god)) {
            DivineInterventionScheduler.tryTriggerBenevolentAvatar(level, player, god);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("god", getGod().getId());
        output.putInt("charges", this.ritualCharges);
        output.putBoolean("awaiting", this.awaitingCondition);
        output.putBoolean("village_shrine", this.villageShrine);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        String godId = input.getStringOr("god", "greed");
        if ("random".equalsIgnoreCase(godId)) {
            SinGod[] values = SinGod.values();
            this.god = values[Math.floorMod(this.worldPosition.asLong(), values.length)];
        } else {
            this.god = SinGod.byId(godId).orElse(SinGod.GREED);
        }
        this.ritualCharges = input.getIntOr("charges", 0);
        this.awaitingCondition = input.getBooleanOr("awaiting", false);
        this.villageShrine = input.getBooleanOr("village_shrine", false);
        tryRegisterVillageShrine();
    }
}
