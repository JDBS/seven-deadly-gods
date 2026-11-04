package com.volcov.sevendeadlygods.favor;

import com.volcov.sevendeadlygods.advancement.ModAdvancements;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.intervention.DivineInterventionScheduler;
import com.volcov.sevendeadlygods.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class SinFavorManager {
    private SinFavorManager() {
    }

    public static PlayerSinData get(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_SIN_DATA.get());
    }

    public static int getFavor(ServerPlayer player, SinGod god) {
        return get(player).getFavor(god);
    }

    /**
     * Aplica un esfuerzo gradual de favor/ira hacia un dios.
     * Recuerda que el marcador solo sube o baja de 1 en 1.
     */
    public static int addEffort(ServerPlayer player, SinGod god, float effort, String reasonHint) {
        PlayerSinData data = get(player);
        long gameTime = player.level().getGameTime();
        if (data.isGodInSlumber(god, gameTime)) {
            return 0;
        }
        int oldFavor = data.getFavor(god);
        int delta = data.addDampedEffort(god, effort);

        if (delta != 0) {
            int newFavor = data.getFavor(god);
            onFavorChanged(player, god, oldFavor, newFavor, reasonHint);

            // Regla especial de Leviatan (Envidia):
            // Le enfada que el jugador suba favor por encima de +20 con otros dioses,
            // y le complace que el jugador caiga en desgracia con otros dioses.
            if (god != SinGod.ENVY && !data.isGodInSlumber(SinGod.ENVY, gameTime)) {
                if (delta > 0 && newFavor > 20) {
                    int envyOld = data.getFavor(SinGod.ENVY);
                    int envyDelta = data.addDampedEffort(SinGod.ENVY, -0.75F);
                    if (envyDelta != 0) {
                        onFavorChanged(player, SinGod.ENVY, envyOld, data.getFavor(SinGod.ENVY),
                                "Celos por tu devocion hacia " + god.getDisplayName());
                    }
                } else if (delta < 0 && newFavor < -10) {
                    int envyOld = data.getFavor(SinGod.ENVY);
                    int envyDelta = data.addDampedEffort(SinGod.ENVY, 0.5F);
                    if (envyDelta != 0) {
                        onFavorChanged(player, SinGod.ENVY, envyOld, data.getFavor(SinGod.ENVY),
                                "Regocijo por tu ruptura con " + god.getDisplayName());
                    }
                }
            }
        }
        return delta;
    }

    public static void setFavorExact(ServerPlayer player, SinGod god, int targetFavor) {
        PlayerSinData data = get(player);
        int oldFavor = data.getFavor(god);
        data.setFavorDirect(god, targetFavor);
        int newFavor = data.getFavor(god);
        if (oldFavor != newFavor) {
            onFavorChanged(player, god, oldFavor, newFavor, "Decreto Divino");
        }
    }

    /**
     * Aplica un cambio directo e inmediato de puntos enteros de favor (por ejemplo, Alta Traición: -60),
     * activando los avisos de umbrales y las reacciones colaterales correspondientes.
     */
    public static int addDirectFavor(ServerPlayer player, SinGod god, int favorDelta, String reasonHint) {
        PlayerSinData data = get(player);
        int oldFavor = data.getFavor(god);
        data.setFavorDirect(god, oldFavor + favorDelta);
        int newFavor = data.getFavor(god);
        if (oldFavor != newFavor) {
            onFavorChanged(player, god, oldFavor, newFavor, reasonHint);
        }
        return newFavor - oldFavor;
    }

    /**
     * Aplica las recompensas de favor tras derrotar al Avatar de un Dios (Deicidio).
     * Otorga una gran subida de favor (base +20 para Soberbia y +15 para Ira y Envidia,
     * amortiguada segun la cercania a +100), anulando por completo el bono positivo
     * contra el propio dios que acaba de ser abatido.
     */
    public static void applyDeicideFavorRewards(ServerPlayer player, SinGod defeatedGod) {
        PlayerSinData data = get(player);

        if (defeatedGod != SinGod.PRIDE) {
            int oldFavor = data.getFavor(SinGod.PRIDE);
            int delta = data.addDampedEffort(SinGod.PRIDE, 20.0F);
            if (delta != 0) {
                onFavorChanged(player, SinGod.PRIDE, oldFavor, data.getFavor(SinGod.PRIDE),
                        "Gesta suprema de Deicidio contra " + defeatedGod.getDisplayName());
            }
        }

        if (defeatedGod != SinGod.WRATH) {
            int oldFavor = data.getFavor(SinGod.WRATH);
            int delta = data.addDampedEffort(SinGod.WRATH, 15.0F);
            if (delta != 0) {
                onFavorChanged(player, SinGod.WRATH, oldFavor, data.getFavor(SinGod.WRATH),
                        "Derramamiento de sangre divina de " + defeatedGod.getDisplayName());
            }
        }

        if (defeatedGod != SinGod.ENVY) {
            int oldFavor = data.getFavor(SinGod.ENVY);
            int delta = data.addDampedEffort(SinGod.ENVY, 15.0F);
            if (delta != 0) {
                onFavorChanged(player, SinGod.ENVY, oldFavor, data.getFavor(SinGod.ENVY),
                        "Humillacion y despojo del Avatar de " + defeatedGod.getDisplayName());
            }
        }
    }

    /**
     * Evalua si una interaccion verbal en el chat consigue mover el favor (+1 o -1),
     * mediante una tirada probabilistica que depende del progreso (|favor| actual) y del sentido
     * (alejarse de 0 hacia los extremos vs regresar hacia la neutralidad).
     *
     * @return true si la tirada probabilistica tuvo exito y el favor cambio 1 punto.
     */
    public static boolean tryApplyChatFavor(ServerPlayer player, SinGod god, float effort, RandomSource random) {
        if (Math.abs(effort) < 0.05F) {
            return false;
        }

        PlayerSinData data = get(player);
        long gameTime = player.level().getGameTime();
        if (data.isGodInSlumber(god, gameTime)) {
            return false;
        }
        int currentFavor = data.getFavor(god);

        // Limites absolutos alcanzados
        if ((currentFavor >= PlayerSinData.MAX_FAVOR && effort > 0)
                || (currentFavor <= PlayerSinData.MIN_FAVOR && effort < 0)) {
            return false;
        }

        float probability = calculateChatFavorProbability(currentFavor, effort);
        if (random.nextFloat() < probability) {
            int delta = effort > 0 ? 1 : -1;
            int appliedDelta = data.adjustFavorPreservingEffort(god, delta);
            if (appliedDelta == 0) {
                return false;
            }
            int newFavor = data.getFavor(god);
            onFavorChanged(player, god, currentFavor, newFavor, "Conversacion en el chat");

            // Reaccion colateral de Leviatan (Envidia), escalada segun su propio favor actual
            if (god != SinGod.ENVY && !data.isGodInSlumber(SinGod.ENVY, gameTime)) {
                if (delta > 0 && newFavor > 20) {
                    int envyOld = data.getFavor(SinGod.ENVY);
                    float envyProb = calculateChatFavorProbability(envyOld, -0.55F);
                    if (envyOld > PlayerSinData.MIN_FAVOR && random.nextFloat() < envyProb) {
                        data.adjustFavorPreservingEffort(SinGod.ENVY, -1);
                        int envyNew = data.getFavor(SinGod.ENVY);
                        if (envyNew != envyOld) {
                            onFavorChanged(player, SinGod.ENVY, envyOld, envyNew, "Celos por tu devocion hacia " + god.getDisplayName());
                        }
                    }
                } else if (delta < 0 && newFavor < -10) {
                    int envyOld = data.getFavor(SinGod.ENVY);
                    float envyProb = calculateChatFavorProbability(envyOld, 0.55F);
                    if (envyOld < PlayerSinData.MAX_FAVOR && random.nextFloat() < envyProb) {
                        data.adjustFavorPreservingEffort(SinGod.ENVY, 1);
                        int envyNew = data.getFavor(SinGod.ENVY);
                        if (envyNew != envyOld) {
                            onFavorChanged(player, SinGod.ENVY, envyOld, envyNew, "Regocijo por tu ruptura con " + god.getDisplayName());
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Calcula la probabilidad (0.02 a 0.95) de que una interaccion en el chat altere el favor del dios,
     * en funcion del progreso actual (|favor| de 0 a 100) y del sentido (alejarse de 0 vs acercarse a 0).
     *
     * Cuanto mas cerca de los extremos (+-100), mas dificil resulta avanzar un punto solo con palabras.
     */
    public static float calculateChatFavorProbability(int currentFavor, float effort) {
        if (Math.abs(effort) < 0.05F) {
            return 0.0F;
        }

        int absFavor = Math.abs(currentFavor);
        boolean movingAwayFromZero = (currentFavor >= 0 && effort > 0) || (currentFavor <= 0 && effort < 0);
        float effortMagnitude = Math.clamp(Math.abs(effort), 0.25F, 1.0F);

        float baseChance;
        if (movingAwayFromZero) {
            // SENTIDO: Avanzar hacia los extremos (-100 o +100).
            // A medida que el progreso aumenta (|favor| sube), la probabilidad disminuye de forma escalonada:
            if (absFavor <= 20) {
                // Progreso inicial (0 a 20): El dios presta atencion facilmente (85% -> 70%)
                baseChance = 0.85F - (absFavor / 20.0F) * 0.15F;
            } else if (absFavor <= 60) {
                // Progreso intermedio (21 a 60): Requiere mayor constancia (70% -> 40%)
                float t = (absFavor - 20) / 40.0F;
                baseChance = 0.70F - t * 0.30F;
            } else if (absFavor <= 85) {
                // Progreso avanzado (61 a 85): El dios se vuelve exigente (40% -> 15%)
                float t = (absFavor - 60) / 25.0F;
                baseChance = 0.40F - t * 0.25F;
            } else {
                // Tramo extremo (86 a 100): Sumamente dificil de alterar solo hablando (15% -> 5%)
                float t = (absFavor - 85) / 15.0F;
                baseChance = 0.15F - t * 0.10F;
            }
        } else {
            // SENTIDO: Regresar hacia 0 (hacia la neutralidad).
            if (currentFavor > 0 && effort < 0) {
                // Caer de la gracia por insolencia o blasfemia: los dioses son orgullosos y faciles de ofender
                if (absFavor > 80) {
                    baseChance = 0.80F; // El campeon supremo que insulta pierde la gracia con gran facilidad
                } else if (absFavor > 40) {
                    baseChance = 0.75F;
                } else {
                    baseChance = 0.70F;
                }
            } else {
                // Redencion desde la ira negativa hacia 0 (suplicar piedad, arrepentirse):
                if (absFavor > 80) {
                    baseChance = 0.35F; // Un hereje condenado debe insistir mucho para que atiendan su suplica
                } else if (absFavor > 40) {
                    baseChance = 0.50F;
                } else {
                    baseChance = 0.65F;
                }
            }
        }

        // Modular segun la intensidad/fuerza del esfuerzo del mensaje (effortMagnitude)
        return Math.clamp(baseChance * effortMagnitude, 0.02F, 0.95F);
    }

    private static void onFavorChanged(ServerPlayer player, SinGod god, int oldFavor, int newFavor, String reasonHint) {
        boolean increased = newFavor > oldFavor;
        int oldTier = getFavorTier(oldFavor);
        int newTier = getFavorTier(newFavor);

        // Notificación en la Action Bar (parte inferior):
        // Solo se muestra cuando hay un cambio de rango para mantener al jugador con desconocimiento al inicio (-19 a +19).
        if (oldTier != newTier) {
            Component statusMsg = Component.literal(
                    increased ? "Tus acciones han complacido a " : "Tus acciones han contrariado a "
            ).withStyle(increased ? ChatFormatting.GREEN : ChatFormatting.RED)
            .append(Component.literal(god.getDisplayName()).withStyle(god.getChatColor(), ChatFormatting.BOLD))
            .append(Component.literal(".").withStyle(increased ? ChatFormatting.GREEN : ChatFormatting.RED));

            player.sendOverlayMessage(statusMsg);
        }

        // Comprobar cruces de umbrales criticos
        checkThresholdCrossing(player, god, oldFavor, newFavor);
    }

    private static void checkThresholdCrossing(ServerPlayer player, SinGod god, int oldFavor, int newFavor) {
        // Concesion de avances por cruce de umbrales negativos (Ira)
        if (newFavor <= -20) {
            ModAdvancements.award(player, "wrath_tier_minor");
        }
        if (newFavor <= -60) {
            ModAdvancements.award(player, "wrath_tier_envoys");
        }
        if (newFavor <= -80) {
            ModAdvancements.award(player, "wrath_tier_heretic");
        }
        if (newFavor <= -100) {
            ModAdvancements.award(player, "wrath_tier_avatar");
        }

        // Concesion de avances por cruce de umbrales positivos (Favor y Devocion)
        if (newFavor >= 20) {
            ModAdvancements.award(player, "favor_tier_sympathy");
        }
        if (newFavor >= 61) {
            ModAdvancements.award(player, "favor_tier_disciple");
        }
        if (newFavor >= 85) {
            ModAdvancements.award(player, "favor_tier_chosen");
        }
        if (newFavor >= 91) {
            ModAdvancements.award(player, "favor_tier_champion");
        }
        if (newFavor >= 100) {
            ModAdvancements.award(player, "favor_tier_relic");
        }

        // Umbrales negativos acumulativos (Ira)
        if (oldFavor > -100 && newFavor <= -100) {
            sendThresholdAlert(player, god,
                    "¡IRA ABSOLUTA! Has colmado la paciencia divina. El Avatar de " + god.getDisplayName() + " desciende en persona para ejecutar su juicio mortal.",
                    SoundEvents.WARDEN_ROAR);
            DivineInterventionScheduler.triggerWrathAvatarHunt(player, god);
        } else if (oldFavor > -80 && newFavor <= -80) {
            sendThresholdAlert(player, god,
                    "Has sido declarado hereje por " + god.getDisplayName() + ". Sus Acólitos alados se suman a la cacería de tu alma.",
                    SoundEvents.END_PORTAL_SPAWN);
        } else if (oldFavor > -60 && newFavor <= -60) {
            sendThresholdAlert(player, god,
                    "La cólera de " + god.getDisplayName() + " se intensifica. Sus Enviados han sido despachados para castigar tu insolencia.",
                    SoundEvents.WITHER_SPAWN);
        } else if (oldFavor > -20 && newFavor <= -20) {
            sendThresholdAlert(player, god,
                    "Has despertado el desdén de " + god.getDisplayName() + ". Su mirada ominosa pesa sobre tus espaldas...",
                    SoundEvents.LIGHTNING_BOLT_THUNDER);
        }

        // Umbrales positivos acumulativos (Favor y Devoción)
        if (oldFavor < 100 && newFavor >= 100) {
            sendThresholdAlert(player, god,
                    "¡CÚSPIDE DE LA DEVOCIÓN! Has alcanzado la comunión suprema con " + god.getDisplayName() + ". Realiza una ofrenda en su Altar para recibir su Reliquia Divina en persona.",
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE);
        } else if (oldFavor < 91 && newFavor >= 91) {
            sendThresholdAlert(player, god,
                    "¡CAMPEÓN DIVINO de " + god.getDisplayName() + "! Tu alma goza de la salvación milagrosa ante la muerte y la plenitud de sus dones.",
                    SoundEvents.BEACON_ACTIVATE);
        } else if (oldFavor < 85 && newFavor >= 85) {
            sendThresholdAlert(player, god,
                    "Has sido consagrado como Elegido de " + god.getDisplayName() + ". Sus Acólitos alados acudirán a combatir a tu lado.",
                    SoundEvents.TOTEM_USE);
        } else if (oldFavor < 61 && newFavor >= 61) {
            sendThresholdAlert(player, god,
                    "Has alcanzado el rango de Discípulo de " + god.getDisplayName() + ". Tus rituales en su altar otorgarán bendiciones de poder supremo.",
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE);
        } else if (oldFavor < 20 && newFavor >= 20) {
            sendThresholdAlert(player, god,
                    "Has ganado la simpatía de " + god.getDisplayName() + ". La presencia divina te asistirá en momentos de necesidad.",
                    SoundEvents.EXPERIENCE_ORB_PICKUP);
        }
    }

    private static void sendThresholdAlert(ServerPlayer player, SinGod god, String text, net.minecraft.sounds.SoundEvent sound) {
        player.sendSystemMessage(
                god.getFormattedPrefix()
                        .append(Component.literal(text).withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC))
        );
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundSource.PLAYERS,
                0.85F,
                1.0F
        );
    }

    /**
     * Determina si el nivel de favor se encuentra en el rango de bajo interés / neutral (-19 a +19).
     */
    public static boolean isLowInterest(int favor) {
        return favor >= -19 && favor <= 19;
    }

    /**
     * Devuelve el índice numérico del rango de favor actual (-4 a 5).
     * El rango 0 corresponde a la neutralidad / bajo interés (-19 a +19).
     */
    public static int getFavorTier(int favor) {
        if (favor <= -100) return -4;
        if (favor <= -80) return -3;
        if (favor <= -60) return -2;
        if (favor <= -20) return -1;
        if (favor < 20) return 0;
        if (favor < 61) return 1;
        if (favor < 85) return 2;
        if (favor < 91) return 3;
        if (favor < 100) return 4;
        return 5;
    }

    public static String describeStanding(int favor) {
        if (favor <= -100) return "Condenado a Muerte (-100 | Acumula -20, -60, -80 + Caza del Avatar)";
        if (favor <= -80) return "Perseguido por Acolitos (-80 o menos | Acumula -20, -60, -80)";
        if (favor <= -60) return "Cazado por Enviados (-60 o menos | Acumula -20, -60)";
        if (favor <= -20) return "Disgustado (-20 o menos)";
        if (favor < 20) return "Neutral / Observador (-19 a +19)";
        if (favor < 61) return "Favorecido (20+)";
        if (favor < 85) return "Discipulo de Ritual (61+ | Acumula 20+, 61+)";
        if (favor < 91) return "Protegido por Acolitos (85+ | Acumula 20+, 61+, 85+)";
        if (favor < 100) return "Campeon Divino (91+ | Acumula 20+, 61+, 85+, 91+)";
        return "Cúspide de la Devoción (+100 | Reliquia Divina + Acumula todas)";
    }
}
