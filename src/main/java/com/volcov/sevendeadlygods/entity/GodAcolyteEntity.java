package com.volcov.sevendeadlygods.entity;

import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Sirviente / Acolito de un Dios del Pecado.
 * Usa un modelo tipo aldeano con capucha, colores distintivos de su dios y alas en la espalda.
 * - A <= -80 de favor: aparece cada 2-5 dias para dar caza al jugador.
 * - A >= +85 de favor: acude en ayuda del jugador si esta bajo de vida o si lo ataca otro dios (Guerras de Dioses).
 */
public class GodAcolyteEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> DATA_GOD_ID =
            SynchedEntityData.defineId(GodAcolyteEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_ALLIED =
            SynchedEntityData.defineId(GodAcolyteEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID assignedPlayerUuid;
    private int lifeTicks = 0;

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
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Guerras de Dioses: los Acolitos atacan a Acolitos o Enviados de otros dioses rivales
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this,
                GodAcolyteEntity.class,
                10,
                true,
                false,
                (candidate, serverLevel) -> candidate instanceof GodAcolyteEntity other && other.getGod() != this.getGod()
        ));
        // Si es un Acolito aliado (>= +85), defiende al jugador de monstruos y Enviados rivales
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                this,
                Monster.class,
                10,
                true,
                false,
                (candidate, serverLevel) -> this.isAlliedProtector()
        ));
        // Si es un Acolito hostil (<= -80), caza al jugador objetivo
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                this,
                Player.class,
                10,
                true,
                false,
                (candidate, serverLevel) -> !this.isAlliedProtector()
                        && (this.assignedPlayerUuid == null || candidate.getUUID().equals(this.assignedPlayerUuid))
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

    public SinGod getGod() {
        return SinGod.byId(this.entityData.get(DATA_GOD_ID)).orElse(SinGod.WRATH);
    }

    public boolean isAlliedProtector() {
        return this.entityData.get(DATA_ALLIED);
    }

    @Override
    public void tick() {
        super.tick();

        // Caida lenta gracias a sus alas divinas
        if (!this.onGround() && this.getDeltaMovement().y < 0.0D) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.65D, 1.0D));
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            this.lifeTicks++;
            if (this.lifeTicks % 20 == 0 && this.assignedPlayerUuid != null) {
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

            // Desaparecer tras 5 minutos (6000 ticks)
            if (this.lifeTicks > 6000) {
                serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0D, getZ(), 20, 0.3D, 0.5D, 0.3D, 0.05D);
                this.discard();
            }
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("god", getGod().getId());
        output.putBoolean("allied", isAlliedProtector());
        output.putInt("life_ticks", this.lifeTicks);
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
