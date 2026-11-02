package com.volcov.sevendeadlygods.entity;

import com.volcov.sevendeadlygods.advancement.ModAdvancements;
import com.volcov.sevendeadlygods.favor.SinFavorManager;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.village.DivineTradingRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Sirviente / Acólito de un Dios del Pecado.
 * Usa un modelo tipo aldeano con capucha, colores distintivos de su dios y alas en la espalda.
 * - A <= -80 de favor: aparece cada 2-5 días para dar caza al jugador.
 * - A >= +85 de favor: acude en ayuda del jugador si está bajo de vida o si lo ataca otro dios (Guerras de Dioses).
 * - En Aldeas Consagradas: actúa como Centinela Permanente y Mercader del Templo:
 *   - Favor <= -40: Hostilidad inmediata a muerte.
 *   - -39 <= Favor < 0: Desconfianza y rechazo de comercio (sonido Villager No).
 *   - Favor >= 0: Abre el catálogo de Comercio Divino de 5 niveles de su deidad.
 */
public class GodAcolyteEntity extends PathfinderMob implements Merchant {
    private static final EntityDataAccessor<String> DATA_GOD_ID =
            SynchedEntityData.defineId(GodAcolyteEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_ALLIED =
            SynchedEntityData.defineId(GodAcolyteEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID assignedPlayerUuid;
    private int lifeTicks = 0;
    private boolean villageSentinel = false;
    @Nullable
    private BlockPos homeAltarPos;

    @Nullable
    private Player tradingPlayer;
    private MerchantOffers offers = new MerchantOffers();
    private int villagerXp = 0;

    public GodAcolyteEntity(EntityType<? extends GodAcolyteEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 45.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 36.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GOD_ID, SinGod.WRATH.getId());
        builder.define(DATA_ALLIED, false);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData
    ) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        if (level.getLevel() instanceof ServerLevel serverLevel && this.getMainHandItem().isEmpty()) {
            SinGod god = this.getGod();
            DivineEquipmentHelper.equipAcolyte(serverLevel, this, god);
            if (this.getCustomName() == null) {
                this.setCustomName(god.getAcolyteCustomName(this.isAlliedProtector()));
                this.setCustomNameVisible(true);
            }
        }
        return data;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Guerras de Dioses: los Acólitos atacan a Acólitos o Enviados de otros dioses rivales
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this,
                GodAcolyteEntity.class,
                10,
                true,
                false,
                (candidate, serverLevel) -> candidate instanceof GodAcolyteEntity other && other.getGod() != this.getGod()
        ));
        // Objetivo de ataque para jugadores con favor <= -40 (Herejes territoriales) o cazas asignadas
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this,
                Player.class,
                10,
                true,
                false,
                (living, serverLevel) -> {
                    if (living instanceof ServerPlayer player) {
                        int favor = SinFavorManager.getFavor(player, this.getGod());
                        if (favor <= -40) {
                            return true; // Hostilidad total por herejía (<= -40)
                        }
                        return !this.isAlliedProtector()
                                && !this.villageSentinel
                                && this.assignedPlayerUuid != null
                                && player.getUUID().equals(this.assignedPlayerUuid);
                    }
                    return false;
                }
        ));
        // Si es un Acólito aliado (>= +85) o Centinela de Aldea, defiende contra monstruos
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                this,
                Monster.class,
                10,
                true,
                false,
                (candidate, serverLevel) -> this.isAlliedProtector() || this.villageSentinel
        ));
    }

    public void configureAcolyte(ServerLevel level, SinGod god, @Nullable UUID playerUuid, boolean alliedProtector) {
        this.entityData.set(DATA_GOD_ID, god.getId());
        this.entityData.set(DATA_ALLIED, alliedProtector);
        this.assignedPlayerUuid = playerUuid;
        this.setCustomName(god.getAcolyteCustomName(alliedProtector));
        this.setCustomNameVisible(true);
        DivineEquipmentHelper.equipAcolyte(level, this, god);
    }

    public void configureVillageSentinel(ServerLevel level, SinGod god, BlockPos altarPos) {
        this.entityData.set(DATA_GOD_ID, god.getId());
        this.entityData.set(DATA_ALLIED, true);
        this.villageSentinel = true;
        this.assignedPlayerUuid = null;
        this.setTarget(null);
        this.homeAltarPos = altarPos.immutable();
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("✦ Centinela de " + god.getDisplayName() + " ✦")
                .withStyle(god.getChatColor(), ChatFormatting.BOLD));
        this.setCustomNameVisible(true);
        DivineEquipmentHelper.equipAcolyte(level, this, god);
    }

    public SinGod getGod() {
        return SinGod.byId(this.entityData.get(DATA_GOD_ID)).orElse(SinGod.WRATH);
    }

    public boolean isAlliedProtector() {
        return this.entityData.get(DATA_ALLIED);
    }

    public boolean isVillageSentinel() {
        return this.villageSentinel;
    }

    @Nullable
    public BlockPos getHomeAltarPos() {
        return this.homeAltarPos;
    }

    public void setHomeAltarPos(@Nullable BlockPos homeAltarPos) {
        this.homeAltarPos = homeAltarPos != null ? homeAltarPos.immutable() : null;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        SinGod god = this.getGod();
        int favor = SinFavorManager.getFavor(serverPlayer, god);

        // 1. Hereje hostil (<= -40) -> Ataca inmediatamente
        if (favor <= -40) {
            this.setTarget(serverPlayer);
            return InteractionResult.CONSUME;
        }

        // 2. Sospechoso / Pagano (-39 a -1) -> Rechazo con sonido Villager No
        if (favor < 0) {
            this.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
            serverPlayer.sendSystemMessage(
                    god.getFormattedPrefix().append(
                            Component.literal("«No tenemos tratos con impíos. Arrepiéntete y purifica tu alma ante el altar antes de pedir nuestra bendición.»")
                                    .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC)
                    )
            );
            return InteractionResult.SUCCESS;
        }

        // 3. Devoto / Fiel (>= 0) -> Abre Comercio Divino
        this.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
        DivineTradingRegistry.openTrading(this, serverPlayer);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        super.tick();

        // Caída lenta gracias a sus alas divinas
        if (!this.onGround() && this.getDeltaMovement().y < 0.0D) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.65D, 1.0D));
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            this.lifeTicks++;

            // Cierre inmediato de comercio si el favor cae por debajo de 0 o el acólito entra en combate
            if (this.tradingPlayer instanceof ServerPlayer sp) {
                if (!sp.isAlive() || this.getTarget() != null || SinFavorManager.getFavor(sp, this.getGod()) < 0) {
                    sp.closeContainer();
                    this.tradingPlayer = null;
                }
            }

            if (this.lifeTicks % 20 == 0 && !this.villageSentinel && this.assignedPlayerUuid != null) {
                Player assigned = serverLevel.getPlayerByUUID(this.assignedPlayerUuid);
                if (assigned != null) {
                    if (!this.isAlliedProtector() && this.getTarget() == null && assigned.isAlive()) {
                        this.setTarget(assigned);
                    } else if (this.isAlliedProtector()) {
                        LivingEntity lastAttacker = assigned.getLastHurtByMob();
                        if (lastAttacker != null && lastAttacker.isAlive() && lastAttacker != this) {
                            this.setTarget(lastAttacker);
                        }
                        if (this.distanceToSqr(assigned) > 256.0D && this.getNavigation().isDone()) {
                            this.getNavigation().moveTo(assigned, 1.15D);
                        }
                    }
                }
            }

            // Patrulla alrededor del Altar Mayor si es un Centinela Permanente de Aldea
            if (this.villageSentinel && this.homeAltarPos != null && this.lifeTicks % 40 == 0 && this.getTarget() == null) {
                double distSq = this.distanceToSqr(
                        this.homeAltarPos.getX() + 0.5D,
                        this.homeAltarPos.getY(),
                        this.homeAltarPos.getZ() + 0.5D
                );
                if (distSq > 1296.0D && this.getNavigation().isDone()) {
                    this.getNavigation().moveTo(
                            this.homeAltarPos.getX() + 0.5D,
                            this.homeAltarPos.getY(),
                            this.homeAltarPos.getZ() + 0.5D,
                            1.0D
                    );
                }
            }

            // Desaparecer tras 5 minutos (6000 ticks) SOLO si no es un Centinela Permanente de Aldea
            if (!this.villageSentinel && this.lifeTicks > 6000) {
                serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0D, getZ(), 20, 0.3D, 0.5D, 0.3D, 0.05D);
                this.discard();
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.villageSentinel;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    // =========================================================================
    // Implementación de net.minecraft.world.item.trading.Merchant
    // =========================================================================

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Override
    @Nullable
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        return this.offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        this.ambientSoundTime = -this.getAmbientSoundInterval();
        this.playSound(SoundEvents.VILLAGER_YES, this.getSoundVolume(), this.getVoicePitch());
        if (this.tradingPlayer instanceof ServerPlayer serverPlayer) {
            SinFavorManager.addEffort(serverPlayer, this.getGod(), 0.15F, "Comercio sagrado con acólito de " + this.getGod().getDisplayName());
            if (SinFavorManager.getFavor(serverPlayer, this.getGod()) >= 60) {
                ModAdvancements.award(serverPlayer, "divine_trade_master");
            }
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack itemStack) {
        if (!this.level().isClientSide() && this.ambientSoundTime > -this.getAmbientSoundInterval() + 20) {
            this.ambientSoundTime = -this.getAmbientSoundInterval();
            SoundEvent sound = itemStack.isEmpty() ? SoundEvents.VILLAGER_NO : SoundEvents.VILLAGER_YES;
            this.playSound(sound, this.getSoundVolume(), this.getVoicePitch());
        }
    }

    @Override
    public int getVillagerXp() {
        return this.villagerXp;
    }

    @Override
    public void overrideXp(int xp) {
        this.villagerXp = xp;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.tradingPlayer != player || !this.isAlive() || this.distanceToSqr(player) > 64.0D) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return SinFavorManager.getFavor(serverPlayer, this.getGod()) >= 0;
        }
        return true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("god", getGod().getId());
        output.putBoolean("allied", isAlliedProtector());
        output.putInt("life_ticks", this.lifeTicks);
        output.putBoolean("village_sentinel", this.villageSentinel);
        if (this.homeAltarPos != null) {
            output.putLong("home_altar_pos", this.homeAltarPos.asLong());
        }
        if (this.assignedPlayerUuid != null) {
            output.putString("assigned_player", this.assignedPlayerUuid.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_GOD_ID, input.getStringOr("god", SinGod.WRATH.getId()));
        this.entityData.set(DATA_ALLIED, input.getBooleanOr("allied", false));
        this.lifeTicks = input.getIntOr("life_ticks", 0);
        this.villageSentinel = input.getBooleanOr("village_sentinel", false);
        long altarLong = input.getLongOr("home_altar_pos", Long.MIN_VALUE);
        this.homeAltarPos = altarLong != Long.MIN_VALUE ? BlockPos.of(altarLong) : null;
        String uuidStr = input.getStringOr("assigned_player", "");
        if (!uuidStr.isBlank()) {
            try {
                this.assignedPlayerUuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ignored) {
                this.assignedPlayerUuid = null;
            }
        }
    }
}
