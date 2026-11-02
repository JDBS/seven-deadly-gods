package com.volcov.sevendeadlygods.entity;

import com.volcov.sevendeadlygods.favor.PlayerSinData;
import com.volcov.sevendeadlygods.favor.SinFavorManager;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.intervention.DivineInterventionScheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Avatar fisico de uno de los 7 Dioses de los Pecados Capitales.
 * - Escala de altura: 1.5x respecto al jugador.
 * - Salud: 500 puntos de vida.
 * - Resistencia al dano del 80%, regeneracion y efectos de duracion infinita.
 * - Armas y armaduras equipadas con encantamientos mas alla del limite de la mesa de encantamientos.
 * - Se manifiesta para cazar al jugador SOLO a -100 de favor, o de forma benevola SOLO entre +91 y +100.
 */
public class GodAvatarEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> DATA_GOD_ID =
            SynchedEntityData.defineId(GodAvatarEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_WRATH_HUNTER =
            SynchedEntityData.defineId(GodAvatarEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            UUID.randomUUID(),
            Component.literal("Avatar Divino"),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_10
    );

    @Nullable
    private UUID targetPlayerUuid;
    private boolean relicGifted = false;
    private int manifestationTicks = 0;

    public GodAvatarEntity(EntityType<? extends GodAvatarEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 250;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.SCALE, 1.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GOD_ID, SinGod.PRIDE.getId());
        builder.define(DATA_WRATH_HUNTER, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(2, new StayNearChampionGoal(this, 0.55D, 5.5F, 3.5F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6D) {
            @Override
            public boolean canUse() {
                // Si es un Avatar benevolo acompanando a su Campeon, no deambular alejandose
                if (!GodAvatarEntity.this.isWrathHunter() && GodAvatarEntity.this.hasChampionNearby()) {
                    return false;
                }
                return super.canUse();
            }
        });
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                if (!GodAvatarEntity.this.isWrathHunter() && this.mob.getLastHurtByMob() instanceof Player) {
                    return false;
                }
                return super.canUse();
            }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this,
                Player.class,
                10,
                true,
                false,
                (candidate, serverLevel) -> this.isWrathHunter()
                        && (this.targetPlayerUuid == null || candidate.getUUID().equals(this.targetPlayerUuid))
        ));
        this.targetSelector.addGoal(2, new DefendChampionTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                this,
                Mob.class,
                10,
                true,
                false,
                (candidate, serverLevel) -> {
                    if (this.isWrathHunter()) {
                        return false;
                    }
                    if (!(candidate instanceof Enemy) && !(candidate instanceof GodAcolyteEntity acolyte && !acolyte.isAlliedProtector())) {
                        return false;
                    }
                    if (candidate instanceof GodAcolyteEntity acolyte && acolyte.isAlliedProtector()) {
                        return false;
                    }
                    Player champ = this.getChampionPlayer();
                    if (champ != null) {
                        return candidate.distanceToSqr(champ) <= 20.0D * 20.0D;
                    }
                    return candidate.distanceToSqr(this) <= 16.0D * 16.0D;
                }
        ));
    }

    public void initializeAvatar(ServerLevel level, SinGod god, @Nullable UUID playerUuid, boolean wrathHunter) {
        this.entityData.set(DATA_GOD_ID, god.getId());
        this.entityData.set(DATA_WRATH_HUNTER, wrathHunter);
        this.targetPlayerUuid = playerUuid;
        this.manifestationTicks = 0;

        Component customName = god.getAvatarCustomName();
        this.setCustomName(customName);
        this.setCustomNameVisible(true);
        this.bossEvent.setName(customName);
        this.bossEvent.setColor(god.getBossBarColor());

        // Equipar armas y armaduras sobre-encantadas
        DivineEquipmentHelper.equipAvatarWithOverleveledGear(level, this, god);

        // Aplicar efectos de duracion infinita (Regeneracion, Resistencia IV = 80%, Fuerza, Resistencia al Fuego)
        applyInfiniteDivineEffects();
        this.setHealth(this.getMaxHealth());

        DivineInterventionScheduler.onAvatarSpawned(this);
    }

    private void applyInfiniteDivineEffects() {
        int inf = MobEffectInstance.INFINITE_DURATION;
        this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, inf, 1, false, false));
        this.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, inf, 3, false, false)); // 80% reduccion
        this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, inf, 0, false, false));
        this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, inf, 1, false, false));
    }

    public SinGod getGod() {
        return SinGod.byId(this.entityData.get(DATA_GOD_ID)).orElse(SinGod.PRIDE);
    }

    public boolean isWrathHunter() {
        return this.entityData.get(DATA_WRATH_HUNTER);
    }

    @Nullable
    public UUID getTargetPlayerUuid() {
        return this.targetPlayerUuid;
    }

    public void setTargetPlayerUuid(@Nullable UUID targetPlayerUuid) {
        this.targetPlayerUuid = targetPlayerUuid;
    }

    @Nullable
    public Player getChampionPlayer() {
        if (this.targetPlayerUuid != null) {
            Player player = this.level().getPlayerByUUID(this.targetPlayerUuid);
            if (player != null && player.isAlive() && !player.isSpectator()) {
                return player;
            }
        }
        Player nearest = this.level().getNearestPlayer(this, 32.0D);
        return (nearest != null && nearest.isAlive() && !nearest.isSpectator()) ? nearest : null;
    }

    public boolean hasChampionNearby() {
        Player champ = getChampionPlayer();
        return champ != null && this.distanceToSqr(champ) <= 48.0D * 48.0D;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            spawnClientAuraParticles();
            return;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            this.manifestationTicks++;
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

            if (this.manifestationTicks % 100 == 0) {
                DivineInterventionScheduler.onAvatarTick(this);
            }

            // Asegurar regeneracion continua (500 HP max)
            if (this.manifestationTicks % 20 == 0 && this.getHealth() < this.getMaxHealth()) {
                this.heal(2.5F);
            }

            if (this.isWrathHunter() && this.targetPlayerUuid != null && this.manifestationTicks % 20 == 0) {
                Player target = serverLevel.getPlayerByUUID(this.targetPlayerUuid);
                if (target != null && target.isAlive()) {
                    this.setTarget(target);
                }
            }

            // Si es un Avatar benevolo defendiendo al jugador, no perseguir enemigos demasiado lejos del Campeon
            if (!this.isWrathHunter() && this.getTarget() != null) {
                Player champ = this.getChampionPlayer();
                if (champ != null && (champ.distanceToSqr(this.getTarget()) > 24.0D * 24.0D || this.distanceToSqr(champ) > 28.0D * 28.0D)) {
                    this.setTarget(null);
                }
            }

            // Si es una manifestacion benevola (+91 a +100), permanece 4 minutos (4800 ticks) conversando y luego asciende
            if (!this.isWrathHunter() && this.manifestationTicks > 4800) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5D, getZ(), 60, 0.5D, 1.0D, 0.5D, 0.1D);
                this.discard();
            }
        }
    }

    private void spawnClientAuraParticles() {
        if (this.random.nextInt(2) != 0) {
            return;
        }
        ParticleOptions particle = switch (getGod()) {
            case GREED -> ParticleTypes.WAX_ON;
            case WRATH -> ParticleTypes.FLAME;
            case GLUTTONY -> ParticleTypes.HAPPY_VILLAGER;
            case PRIDE -> ParticleTypes.END_ROD;
            case ENVY -> ParticleTypes.SOUL_FIRE_FLAME;
            case SLOTH -> ParticleTypes.PORTAL;
            case LUST -> ParticleTypes.HEART;
        };
        double ox = (this.random.nextDouble() - 0.5D) * 1.4D;
        double oy = this.random.nextDouble() * 2.6D;
        double oz = (this.random.nextDouble() - 0.5D) * 1.4D;
        this.level().addParticle(particle, this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0.0D, 0.03D, 0.0D);
    }

    /**
     * Garantia explicita de 80% de resistencia al dano en el Avatar.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float reducedAmount = amount * 0.20F; // 80% de resistencia al dano
        return super.hurtServer(level, source, reducedAmount);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean result = super.doHurtTarget(level, target);
        if (result && target instanceof ServerPlayer killedOrHitPlayer && !killedOrHitPlayer.isAlive() && this.isWrathHunter()) {
            // Si el Avatar cazador (-100) ejecuta al jugador, su ira queda saciada (reajusta favor a -85)
            SinFavorManager.setFavorExact(killedOrHitPlayer, getGod(), -85);
            killedOrHitPlayer.sendSystemMessage(
                    getGod().getFormattedPrefix()
                            .append(Component.literal("«Tu vida ha pagado parte de tu insolencia. No vuelvas a desafiarme.»")
                                    .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC))
            );
            this.discard();
        }
        return result;
    }

    public static boolean hasClaimedRelic(ServerPlayer player, SinGod god) {
        String godTag = "has_relic_" + god.getId();
        if (player.entityTags().contains(godTag)
                || player.entityTags().contains("has_received_relic_" + god.getId())) {
            return true;
        }
        PlayerSinData data = SinFavorManager.get(player);
        if (data != null && data.hasClaimedRelic(god)) {
            player.addTag(godTag);
            return true;
        }
        if (hasRelicInInventory(player, god)) {
            player.addTag(godTag);
            if (data != null) {
                data.setClaimedRelic(god, true);
            }
            return true;
        }
        return false;
    }

    public static boolean hasRelicInInventory(ServerPlayer player, SinGod god) {
        String expectedName = switch (god) {
            case GREED -> "Colmillo Áureo de Mammon";
            case WRATH -> "Ejecutora Sangrienta de Khorvath";
            case GLUTTONY -> "Fauces Eternas de Beelze";
            case PRIDE -> "Cetro Solar de Lucian";
            case ENVY -> "Tridente Abisal de Leviatán";
            case SLOTH -> "Maza del Letargo de Belphegor";
            case LUST -> "Espina Carmesí de Asmodeus";
        };
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && !item.isEmpty() && item.has(DataComponents.CUSTOM_NAME)) {
                String name = item.getHoverName().getString();
                if (name.contains(expectedName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Cuando el Avatar se manifiesta benevolamente (+100 de favor), el Campeon puede interactuar con el
     * para recibir por unica vez su Reliquia Divina sobre-encantada y conversar por chat.
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !(this.level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        if (this.isWrathHunter()) {
            return InteractionResult.PASS;
        }

        SinGod god = getGod();
        int favor = SinFavorManager.getFavor(serverPlayer, god);
        if (favor < 100) {
            serverPlayer.sendSystemMessage(
                    god.getFormattedPrefix()
                            .append(Component.literal("«Solo mi Campeón Supremo es digno de reclamar mis reliquias sagradas. Tu devoción aún no ha alcanzado la cúspide.»")
                                    .withStyle(ChatFormatting.YELLOW))
            );
            return InteractionResult.SUCCESS;
        }

        if (hasClaimedRelic(serverPlayer, god)) {
            this.relicGifted = true;
            serverPlayer.sendSystemMessage(
                    god.getFormattedPrefix()
                            .append(Component.literal("«Ya portas mi reliquia divina, Campeón. Demuestra al mundo tu grandeza y devoción. Escribe en el chat y te responderé cara a cara.»")
                                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            );
            return InteractionResult.SUCCESS;
        }

        this.relicGifted = true;
        String godTag = "has_relic_" + god.getId();
        serverPlayer.addTag(godTag);
        serverPlayer.addTag("has_received_relic_" + god.getId());
        serverPlayer.addTag("has_god_relic");

        PlayerSinData data = SinFavorManager.get(serverPlayer);
        if (data != null) {
            data.setClaimedRelic(god, true);
        }

        ItemStack relic = DivineEquipmentHelper.createOverleveledRelicWeapon(serverLevel, god);
        if (!serverPlayer.getInventory().add(relic)) {
            serverPlayer.drop(relic, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        serverLevel.playSound(null, blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        serverPlayer.sendSystemMessage(
                god.getFormattedPrefix()
                        .append(Component.literal("«Toma esta reliquia forjada con mi propio poder, mi Campeón. Háblame en el chat si deseas mi consejo.»")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
        );
        return InteractionResult.SUCCESS;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        if (!this.level().isClientSide()) {
            DivineInterventionScheduler.onAvatarRemoved(this);
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (this.level() instanceof ServerLevel serverLevel) {
            handleAvatarDeath(serverLevel, damageSource);
            DivineInterventionScheduler.onAvatarRemoved(this);
        }
    }

    private void handleAvatarDeath(ServerLevel serverLevel, DamageSource damageSource) {
        SinGod god = getGod();

        // 1. Mensaje de derrota del Dios transmitido al servidor
        String defeatQuote = switch (god) {
            case GREED -> "¡Insolente! ¡Has destrozado una vasija invaluable! La deuda que acabas de contraer conmigo se cobrará con creces cuando despierte...";
            case WRATH -> "¡Aaaargh! ¡Disfruta tu efímera victoria, escoria! ¡Mi carne arde, pero mi ira es eterna! ¡Volveré para arrancarte el corazón!";
            case GLUTTONY -> "No... ¡Mi apetito aún no estaba saciado! Te devoraré la próxima vez, pedazo de carne... hasta el último hueso...";
            case PRIDE -> "¿Cómo se atreve una simple mota de polvo a doblegar la encarnación del Sol? ¡Pagarás esta humillación con tormento infinito!";
            case ENVY -> "Maldito seas... Odio tu fuerza, odio tu aliento... Disfruta tus días de paz, porque cuando regrese de las profundidades, nada te salvará.";
            case SLOTH -> "Qué molestia... Tanta violencia... Descansaré en el vacío cósmico... pero tu pesadilla apenas ha comenzado...";
            case LUST -> "Has desgarrado mi belleza terrenal... Qué crueldad tan exquisita... Cuando despierte, sabrás lo que es el verdadero tormento pasional...";
        };

        Component broadcastMsg = god.getFormattedPrefix().append(
                Component.literal("«" + defeatQuote + "»").withStyle(god.getChatColor(), ChatFormatting.ITALIC)
        );

        // Identificar al jugador responsable
        Entity directKiller = damageSource.getEntity();
        if (directKiller == null) {
            directKiller = this.getLastHurtByMob();
        }
        if (directKiller == null && this.targetPlayerUuid != null) {
            directKiller = serverLevel.getPlayerByUUID(this.targetPlayerUuid);
        }
        ServerPlayer killerPlayer = directKiller instanceof ServerPlayer sp ? sp : null;

        // Transmitir el mensaje de derrota únicamente a los jugadores en la misma dimensión dentro de 64 bloques
        double maxDistSq = 64.0D * 64.0D;
        boolean killerNotified = false;
        for (ServerPlayer nearbyPlayer : serverLevel.players()) {
            if (nearbyPlayer.distanceToSqr(this) <= maxDistSq) {
                nearbyPlayer.sendSystemMessage(broadcastMsg);
                if (killerPlayer != null && nearbyPlayer.getUUID().equals(killerPlayer.getUUID())) {
                    killerNotified = true;
                }
            }
        }
        if (killerPlayer != null && !killerNotified) {
            killerPlayer.sendSystemMessage(broadcastMsg);
        }

        // Efectos sonoros y partículas de deicidio
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 1.2F, 0.8F);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.WEATHER, 1.5F, 0.7F);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.5D, this.getZ(), 3, 0.5D, 0.5D, 0.5D, 0.0D);
        serverLevel.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, -1), this.getX(), this.getY() + 2.0D, this.getZ(), 5, 0.2D, 0.2D, 0.2D, 0.0D);

        ParticleOptions themeParticle = switch (god) {
            case GREED -> ParticleTypes.WAX_ON;
            case WRATH -> ParticleTypes.FLAME;
            case GLUTTONY -> ParticleTypes.HAPPY_VILLAGER;
            case PRIDE -> ParticleTypes.END_ROD;
            case ENVY -> ParticleTypes.SOUL_FIRE_FLAME;
            case SLOTH -> ParticleTypes.PORTAL;
            case LUST -> ParticleTypes.HEART;
        };
        serverLevel.sendParticles(themeParticle, this.getX(), this.getY() + 1.5D, this.getZ(), 80, 1.2D, 1.5D, 1.2D, 0.15D);

        // 4. Duración aleatoria del Letargo Divino (entre 3 y 7 días de Minecraft)
        int slumberDays = serverLevel.getRandom().nextIntBetweenInclusive(3, 7);
        long slumberTicks = (long) slumberDays * DivineInterventionScheduler.ONE_MC_DAY;
        long untilTick = serverLevel.getGameTime() + slumberTicks;

        if (killerPlayer != null) {
            PlayerSinData killerData = SinFavorManager.get(killerPlayer);

            // A) Rencor ciego / Traición
            if (!this.isWrathHunter()) {
                // El jugador traicionó y asesinó a su propio Avatar benevolente
                SinFavorManager.setFavorExact(killerPlayer, god, -100);
                killerPlayer.sendSystemMessage(
                        god.getFormattedPrefix().append(
                                Component.literal("«¡Traición suprema! Has destruido la manifestación terrenal de tu protector divino. Mi favor se ha extinguido y la condena eterna cae sobre ti.»")
                                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                        )
                );
            } else {
                // Rencor ciego: el dios derrotado no perdona; permanece en -100
                SinFavorManager.setFavorExact(killerPlayer, god, -100);
                killerPlayer.sendSystemMessage(
                        god.getFormattedPrefix().append(
                                Component.literal("«Tu insolencia no quedará impune. Ningún mortal derrama sangre divina y escapa a la retribución.»")
                                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                        )
                );
            }

            // A.2) Recompensa de favor por Deicidio con Soberbia (+20 base), Ira (+15 base) y Envidia (+15 base),
            // anulando cualquier bono positivo hacia el propio dios que acaba de ser abatido.
            SinFavorManager.applyDeicideFavorRewards(killerPlayer, god);

            // B) Drop condicional de la Reliquia Divina sobre-encantada
            if (!hasClaimedRelic(killerPlayer, god)) {
                ItemStack relic = DivineEquipmentHelper.createOverleveledRelicWeapon(serverLevel, god);
                dropItem(serverLevel, relic);
                killerPlayer.addTag("has_relic_" + god.getId());
                killerPlayer.addTag("has_received_relic_" + god.getId());
                killerPlayer.addTag("has_god_relic");
                if (killerData != null) {
                    killerData.setClaimedRelic(god, true);
                }
                killerPlayer.sendSystemMessage(
                        Component.literal("✦ ¡Has despojado la Reliquia Divina de " + god.getDisplayName() + "! ✦")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                );
            }

            // C) Drop garantizado del Trofeo de Deicidio decorativo
            ItemStack trophy = DivineEquipmentHelper.createDeicideTrophy(serverLevel, god, killerPlayer);
            dropItem(serverLevel, trophy);

            // D) Aplicar Letargo Divino (3 a 7 días)
            if (killerData != null) {
                killerData.setGodSlumberUntil(god, untilTick);
            }

            killerPlayer.sendSystemMessage(
                    god.getFormattedPrefix().append(
                            Component.literal("«El Dios " + god.getDisplayName() + " ha caído en un profundo letargo cósmico. Sus altares permanecerán inertes hasta que su presencia despierte del vacío...»")
                                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
                    )
            );
        }

        // Si había un jugador objetivo distinto, aplicar también el letargo a sus datos
        if (this.targetPlayerUuid != null && (killerPlayer == null || !this.targetPlayerUuid.equals(killerPlayer.getUUID()))) {
            if (serverLevel.getServer() != null) {
                ServerPlayer targetPlayer = serverLevel.getServer().getPlayerList().getPlayer(this.targetPlayerUuid);
                if (targetPlayer != null) {
                    PlayerSinData targetData = SinFavorManager.get(targetPlayer);
                    if (targetData != null) {
                        targetData.setGodSlumberUntil(god, untilTick);
                    }
                }
            }
        }
    }

    private void dropItem(ServerLevel level, ItemStack stack) {
        if (!stack.isEmpty()) {
            ItemEntity entity = new ItemEntity(level, this.getX(), this.getY() + 0.5D, this.getZ(), stack);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("god", getGod().getId());
        output.putBoolean("wrath_hunter", isWrathHunter());
        output.putBoolean("relic_gifted", this.relicGifted);
        output.putInt("manifest_ticks", this.manifestationTicks);
        if (this.targetPlayerUuid != null) {
            output.putString("target_player", this.targetPlayerUuid.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        SinGod god = SinGod.byId(input.getStringOr("god", SinGod.PRIDE.getId())).orElse(SinGod.PRIDE);
        boolean wrath = input.getBooleanOr("wrath_hunter", false);
        this.entityData.set(DATA_GOD_ID, god.getId());
        this.entityData.set(DATA_WRATH_HUNTER, wrath);
        this.relicGifted = input.getBooleanOr("relic_gifted", false);
        this.manifestationTicks = input.getIntOr("manifest_ticks", 0);
        this.bossEvent.setName(god.getAvatarCustomName());
        this.bossEvent.setColor(god.getBossBarColor());

        String uuidStr = input.getStringOr("target_player", "");
        if (!uuidStr.isBlank()) {
            try {
                this.targetPlayerUuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ignored) {
                this.targetPlayerUuid = null;
            }
        }

        if (!this.level().isClientSide()) {
            DivineInterventionScheduler.onAvatarSpawned(this);
        }
    }

    /**
     * Comportamiento para el Avatar en estado benevolo (+91 a +100 / invocacion por favor 100):
     * - Se mantiene cerca del Campeon a una distancia comoda y solemne (entre 3.5 y 5.5 bloques).
     * - Fija su mirada en el Campeon de forma majestuosa y constante.
     * - Si el Campeon se aleja, camina calmadamente hacia el a paso solemne sin corretear ni alejarse.
     * - Si el Campeon se aleja demasiado o se teletransporta (> 28 bloques), el Avatar se teletransporta junto a el con efectos divinos.
     * - Suprime por completo los movimientos erraticos de alta frecuencia y el deambular aleatorio.
     */
    public static class StayNearChampionGoal extends Goal {
        private final GodAvatarEntity avatar;
        private final double speedModifier;
        private final float startDistance;
        private final float stopDistance;
        @Nullable
        private Player champion;
        private int timeToRecalcPath;

        public StayNearChampionGoal(GodAvatarEntity avatar, double speedModifier, float startDistance, float stopDistance) {
            this.avatar = avatar;
            this.speedModifier = speedModifier;
            this.startDistance = startDistance;
            this.stopDistance = stopDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.avatar.isWrathHunter()) {
                return false;
            }
            // Si el Avatar esta combatiendo activamente para defender al jugador, ceder el control al combate
            if (this.avatar.getTarget() != null && this.avatar.getTarget().isAlive()) {
                return false;
            }
            this.champion = this.avatar.getChampionPlayer();
            return this.champion != null && this.champion.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            if (this.avatar.isWrathHunter()) {
                return false;
            }
            if (this.avatar.getTarget() != null && this.avatar.getTarget().isAlive()) {
                return false;
            }
            if (this.champion == null || !this.champion.isAlive() || this.champion.isSpectator()) {
                this.champion = this.avatar.getChampionPlayer();
            }
            return this.champion != null && this.champion.isAlive();
        }

        @Override
        public void start() {
            this.timeToRecalcPath = 0;
        }

        @Override
        public void stop() {
            this.champion = null;
            this.avatar.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.champion == null || !this.champion.isAlive()) {
                return;
            }

            // Mirar fijamente al Campeón con presencia divina majestuosa
            this.avatar.getLookControl().setLookAt(
                    this.champion.getX(),
                    this.champion.getEyeY(),
                    this.champion.getZ(),
                    25.0F,
                    this.avatar.getMaxHeadXRot()
            );

            double distSqr = this.avatar.distanceToSqr(this.champion);

            // Si el jugador se teletransportó o se alejó demasiado (> 28 bloques), teletransportarse junto a él
            if (distSqr > 28.0D * 28.0D) {
                this.tryTeleportNearChampion();
                return;
            }

            // Si ya está dentro de la distancia cómoda (ej. <= 3.5 bloques), detenerse en el sitio
            if (distSqr <= this.stopDistance * this.stopDistance) {
                if (!this.avatar.getNavigation().isDone()) {
                    this.avatar.getNavigation().stop();
                }
                return;
            }

            // Si el jugador se aleja más allá de startDistance (ej. > 5.5 bloques) o ya está en camino
            if (distSqr > this.startDistance * this.startDistance || !this.avatar.getNavigation().isDone()) {
                if (--this.timeToRecalcPath <= 0) {
                    this.timeToRecalcPath = this.adjustedTickDelay(10);
                    this.avatar.getNavigation().moveTo(this.champion, this.speedModifier);
                }
            }
        }

        private void tryTeleportNearChampion() {
            if (this.champion == null || !(this.avatar.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            BlockPos champPos = this.champion.blockPosition();
            for (int attempt = 0; attempt < 10; attempt++) {
                int dx = this.avatar.getRandom().nextIntBetweenInclusive(-3, 3);
                int dz = this.avatar.getRandom().nextIntBetweenInclusive(-3, 3);
                if (Math.abs(dx) >= 2 || Math.abs(dz) >= 2) {
                    int dy = this.avatar.getRandom().nextIntBetweenInclusive(-1, 1);
                    BlockPos targetPos = champPos.offset(dx, dy, dz);
                    if (canTeleportTo(serverLevel, targetPos)) {
                        // Partículas en posición anterior
                        serverLevel.sendParticles(ParticleTypes.PORTAL, this.avatar.getX(), this.avatar.getY() + 1.2D, this.avatar.getZ(), 25, 0.4D, 0.8D, 0.4D, 0.1D);
                        this.avatar.snapTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, this.avatar.getYRot(), this.avatar.getXRot());
                        this.avatar.getNavigation().stop();
                        // Sonido y partículas en el nuevo destino
                        serverLevel.playSound(null, targetPos, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.2F);
                        serverLevel.sendParticles(ParticleTypes.END_ROD, targetPos.getX() + 0.5D, targetPos.getY() + 1.2D, targetPos.getZ() + 0.5D, 30, 0.4D, 0.8D, 0.4D, 0.05D);
                        return;
                    }
                }
            }
        }

        private boolean canTeleportTo(ServerLevel level, BlockPos pos) {
            PathType pathType = WalkNodeEvaluator.getPathTypeStatic(this.avatar, pos);
            if (pathType != PathType.WALKABLE) {
                return false;
            }
            BlockPos delta = pos.subtract(this.avatar.blockPosition());
            return level.noCollision(this.avatar, this.avatar.getBoundingBox().move(delta));
        }
    }

    /**
     * Meta de combate para proteger al Campeon:
     * El Avatar ataca a cualquier entidad que haya danado a su Campeon
     * o a la que su Campeon este atacando en combate.
     */
    public static class DefendChampionTargetGoal extends TargetGoal {
        private final GodAvatarEntity avatar;
        private LivingEntity attacker;
        private int lastHurtTimestamp;

        public DefendChampionTargetGoal(GodAvatarEntity avatar) {
            super(avatar, false);
            this.avatar = avatar;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (this.avatar.isWrathHunter()) {
                return false;
            }
            Player champ = this.avatar.getChampionPlayer();
            if (champ == null) {
                return false;
            }

            // 1. Proteger si alguien hirió al Campeón
            LivingEntity lastHurtBy = champ.getLastHurtByMob();
            int hurtByTimestamp = champ.getLastHurtByMobTimestamp();
            if (hurtByTimestamp != this.lastHurtTimestamp && lastHurtBy != null && lastHurtBy.isAlive()) {
                if (isValidTarget(champ, lastHurtBy)) {
                    this.attacker = lastHurtBy;
                    this.lastHurtTimestamp = hurtByTimestamp;
                    return this.canAttack(this.attacker, TargetingConditions.DEFAULT);
                }
            }

            // 2. Apoyar si el Campeón atacó a un mob hostil
            LivingEntity lastAttacked = champ.getLastHurtMob();
            int attackTimestamp = champ.getLastHurtMobTimestamp();
            if (attackTimestamp != this.lastHurtTimestamp && lastAttacked != null && lastAttacked.isAlive()) {
                if (isValidTarget(champ, lastAttacked)) {
                    this.attacker = lastAttacked;
                    this.lastHurtTimestamp = attackTimestamp;
                    return this.canAttack(this.attacker, TargetingConditions.DEFAULT);
                }
            }

            return false;
        }

        private boolean isValidTarget(Player champ, LivingEntity entity) {
            if (entity == this.avatar || entity == champ || entity instanceof Player) {
                return false;
            }
            if (entity instanceof GodAcolyteEntity acolyte && acolyte.isAlliedProtector()) {
                return false;
            }
            return champ.distanceToSqr(entity) <= 24.0D * 24.0D;
        }

        @Override
        public void start() {
            this.mob.setTarget(this.attacker);
            this.targetMob = this.attacker;
            super.start();
        }
    }
}
