package com.volcov.sevendeadlygods.chat;

import com.volcov.sevendeadlygods.advancement.ModAdvancements;
import com.volcov.sevendeadlygods.ai.DivinePhysicalAction;
import com.volcov.sevendeadlygods.ai.GeminiAiService;
import com.volcov.sevendeadlygods.block.SinAltarBlockEntity;
import com.volcov.sevendeadlygods.config.ModConfig;
import com.volcov.sevendeadlygods.entity.GodAvatarEntity;
import com.volcov.sevendeadlygods.favor.PlayerSinBehaviorEvents;
import com.volcov.sevendeadlygods.favor.PlayerSinData;
import com.volcov.sevendeadlygods.favor.SinFavorManager;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.intervention.DivineInterventionScheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.List;

public final class DivineChatRouter {

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        String rawMessage = event.getRawText().trim();
        if (rawMessage.length() < 3) {
            return;
        }

        // 1. Comprobar si el jugador esta frente a un Altar de Soberbia (Lucian) en Y > 120 realizando su ritual de alabanza
        SinAltarBlockEntity nearbyAltar = PlayerSinBehaviorEvents.findNearbyAltar(serverLevel, player.blockPosition(), null, 6);
        if (nearbyAltar != null && nearbyAltar.getGod() == SinGod.PRIDE) {
            nearbyAltar.onPridePraiseSpoken(serverLevel, player, rawMessage);
        }

        // 2. Comprobar si hay un Avatar manifestado cerca del jugador (comunicacion presencial directa)
        List<GodAvatarEntity> nearbyAvatars = serverLevel.getEntitiesOfClass(
                GodAvatarEntity.class,
                player.getBoundingBox().inflate(24.0D)
        );
        SinGod manifestedGod = nearbyAvatars.isEmpty() ? null : nearbyAvatars.getFirst().getGod();

        // 3. Comprobar si algún dios puede tener interés en la conversación del jugador
        PlayerSinData data = SinFavorManager.get(player);
        boolean anyGodCanBeInterested = false;
        for (SinGod god : SinGod.values()) {
            boolean isManifested = (manifestedGod == god);
            int fav = data.getFavor(god);
            if (canGodBeInterestedInChat(god, fav, rawMessage, isManifested)) {
                anyGodCanBeInterested = true;
                break;
            }
        }

        if (!anyGodCanBeInterested) {
            // Todos los dioses tienen bajo interés (-19 a +19) y ninguno fue nombrado directamente
            return;
        }

        // 4. Si NO hay un Avatar manifestado cerca, aplicar filtro de relevancia y cooldown
        long now = serverLevel.getGameTime();
        long cooldownTicks = ModConfig.CHAT_EVALUATION_COOLDOWN_SECONDS.get() * 20L;

        boolean hasKeywordMatch = GeminiAiService.detectRelevantGodHeuristic(rawMessage).isPresent();
        if (manifestedGod == null && nearbyAltar == null && !hasKeywordMatch) {
            // Si no menciona nada cercano a los dominios de los 7 pecados y aun esta en enfriamiento, ignorar
            if ((now - data.getLastChatEvalTick()) < cooldownTicks) {
                return;
            }
            // Incluso fuera de enfriamiento, descartar mensajes muy breves que no tengan interes tematico
            if (rawMessage.split("\\s+").length < 4) {
                return;
            }
        }

        data.setLastChatEvalTick(now);
        SinGod altarGod = nearbyAltar != null ? nearbyAltar.getGod() : null;
        GeminiAiService.PlayerContextSnapshot snapshot = GeminiAiService.PlayerContextSnapshot.capture(
                player,
                rawMessage,
                manifestedGod,
                altarGod
        );

        MinecraftServer server = serverLevel.getServer();
        GeminiAiService.evaluatePlayerChatAsync(snapshot).thenAccept(decision -> {
            if (!decision.shouldRespond() || decision.spokenMessage().isBlank()) {
                return;
            }
            server.execute(() -> {
                if (!player.isAlive() || player.hasDisconnected()) {
                    return;
                }
                SinGod god = decision.god();
                int currentFavor = SinFavorManager.get(player).getFavor(god);
                boolean isManifested = (manifestedGod == god);

                // Comprobación de seguridad: los dioses con bajo interés (-19 a +19) no se interesan a menos que los nombre directamente
                if (!canGodBeInterestedInChat(god, currentFavor, rawMessage, isManifested)) {
                    return;
                }

                Component formatted = god.getFormattedPrefix()
                        .append(Component.literal(decision.spokenMessage()).withStyle(ChatFormatting.WHITE));

                // Transmitir la voz del Dios a los jugadores en la misma dimensión dentro de 64 bloques
                // (tanto en torno al jugador que habló como en torno al Avatar manifestado)
                double maxDistSq = 64.0D * 64.0D;
                GodAvatarEntity avatar = DivineInterventionScheduler.getActiveAvatar(server, god);

                for (ServerPlayer recipient : serverLevel.players()) {
                    boolean nearPlayer = recipient.distanceToSqr(player) <= maxDistSq;
                    boolean nearAvatar = avatar != null && avatar.level() == serverLevel && recipient.distanceToSqr(avatar) <= maxDistSq;
                    if (recipient == player || nearPlayer || nearAvatar) {
                        recipient.sendSystemMessage(formatted);
                    }
                }

                player.level().playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.PLAYERS,
                        1.0F,
                        0.75F
                );

                // Modificacion probabilistica del favor segun el sentido (hacia extremos vs hacia 0) y el progreso (|favor|)
                SinFavorManager.tryApplyChatFavor(player, god, decision.favorEffort(), serverLevel.getRandom());

                // Si el jugador pide expresamente la presencia del dios y cuenta con Favor +100, promover a invocacion
                DivinePhysicalAction actionToExecute = decision.physicalAction();
                if ((actionToExecute == DivinePhysicalAction.NONE || actionToExecute == DivinePhysicalAction.BLESSING)
                        && isInvocationChatIntent(rawMessage)
                        && SinFavorManager.get(player).getFavor(god) >= 100) {
                    actionToExecute = DivinePhysicalAction.SUMMON_AVATAR;
                }

                // Ejecutar la reaccion fisica activa inmediata (rayo, maldicion, repulsion, bendicion o manifestacion del Avatar)
                executePhysicalReaction(serverLevel, player, god, actionToExecute);
            });
        });
    }

    /**
     * Determina si un dios puede interesarse en la conversación del jugador:
     * Si su favor está en bajo interés (-19 a +19) y no está manifestado como Avatar presencial,
     * SOLO puede interesarse si el jugador lo nombra directamente por su nombre o título.
     */
    public static boolean canGodBeInterestedInChat(SinGod god, int favor, String message, boolean isManifestedNearby) {
        if (isManifestedNearby) {
            return true;
        }
        if (SinFavorManager.isLowInterest(favor)) {
            return god.isDirectlyNamed(message);
        }
        return true;
    }

    private static boolean isInvocationChatIntent(String message) {
        String lower = message.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("invoco") || lower.contains("manifiestate") || lower.contains("manifiéstate")
                || lower.contains("desciende") || lower.contains("acude") || lower.contains("aparece")
                || lower.contains("ven a mi") || lower.contains("ven a mí") || lower.contains("ven ante mi")
                || lower.contains("ven ante mí") || lower.contains("presentate") || lower.contains("preséntate")
                || lower.contains("summon") || lower.contains("manifest");
    }

    private static void executePhysicalReaction(
            ServerLevel level,
            ServerPlayer player,
            SinGod god,
            DivinePhysicalAction action
    ) {
        if (action == null || action == DivinePhysicalAction.NONE) {
            return;
        }

        switch (action) {
            case NONE -> {}
            case LIGHTNING -> {
                if (level.canSeeSky(player.blockPosition())) {
                    LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
                    if (bolt != null) {
                        bolt.snapTo(player.getX(), player.getY(), player.getZ());
                        level.addFreshEntity(bolt);
                    }
                } else {
                    player.hurtServer(level, level.damageSources().magic(), 6.0F);
                    level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, -1), player.getX(), player.getY() + 1.0D, player.getZ(), 5, 0.2D, 0.2D, 0.2D, 0.0D);
                    level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
            case CURSE -> {
                MobEffectInstance debuff = switch (god) {
                    case GREED -> new MobEffectInstance(MobEffects.MINING_FATIGUE, 400, 1);
                    case WRATH -> {
                        player.igniteForSeconds(3);
                        yield new MobEffectInstance(MobEffects.WEAKNESS, 400, 1);
                    }
                    case GLUTTONY -> new MobEffectInstance(MobEffects.HUNGER, 300, 2);
                    case PRIDE -> new MobEffectInstance(MobEffects.LEVITATION, 60, 1);
                    case ENVY -> new MobEffectInstance(MobEffects.BLINDNESS, 200, 0);
                    case SLOTH -> new MobEffectInstance(MobEffects.SLOWNESS, 400, 2);
                    case LUST -> new MobEffectInstance(MobEffects.DARKNESS, 300, 0);
                };
                player.addEffect(debuff);
                level.playSound(null, player.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.8F, 0.9F);
                level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0D, player.getZ(), 25, 0.4D, 0.6D, 0.4D, 0.05D);
            }
            case REPULSION -> {
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(-look.x * 1.6D, 0.55D, -look.z * 1.6D);
                player.hurtServer(level, level.damageSources().magic(), 3.0F);
                level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
                level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1.0D, player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            case BLESSING -> {
                player.heal(4.0F);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                if (player.getFoodData().getFoodLevel() < 20) {
                    player.getFoodData().eat(4, 4.0F);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.PLAYERS, 1.0F, 1.2F);
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.4D, 0.6D, 0.4D, 0.1D);
            }
            case SUMMON_AVATAR -> {
                PlayerSinData data = SinFavorManager.get(player);
                int favor = data.getFavor(god);
                if (favor < 100) {
                    player.sendSystemMessage(god.getFormattedPrefix()
                            .append(Component.literal("«Solo atiendo la llamada presencial de un Campeón Supremo. Tu devoción aún debe elevarse antes de que descienda a tu lado.»")
                                    .withStyle(ChatFormatting.YELLOW)));
                    return;
                }

                long now = level.getGameTime();
                long nextInvocation = data.getNextAvatarInvocationTick(god);
                if (now < nextInvocation) {
                    player.sendSystemMessage(god.getFormattedPrefix()
                            .append(Component.literal("«El velo celestial aún se reconstituye tras nuestra última comunión. Permite que el ciclo del sol complete su curso antes de volver a convocar mi presencia terrenal.»")
                                    .withStyle(ChatFormatting.YELLOW)));
                    return;
                }

                // Invocar o reubicar Avatar benévolo (aplica chequeo anti-exploit de ira y avatar único)
                boolean triggered = DivineInterventionScheduler.tryTriggerBenevolentAvatar(level, player, god);
                if (triggered) {
                    // Logro secreto: Un dios acudio a tu llamada presencial en el mundo
                    ModAdvancements.award(player, "secret_god_answered_call");
                    // Fijar enfriamiento de 1 día de Minecraft (24,000 ticks = 20 minutos reales) solo si se manifestó
                    data.setNextAvatarInvocationTick(god, now + DivineInterventionScheduler.ONE_MC_DAY);
                }
            }
        }
    }
}
