package com.volcov.sevendeadlygods.ai;

import java.util.Locale;

/**
 * Reacciones fisicas activas que los Dioses del Pecado pueden desatar
 * de forma inmediata en el mundo en respuesta a las palabras del jugador en el chat.
 */
public enum DivinePhysicalAction {
    /** Sin reaccion fisica directa (dialogo normal). */
    NONE,
    /**
     * Rayo celestial fulminante (a cielo abierto) o descarga magica (bajo techo).
     * Disparado por blasfemia grave, insultos directos o desafio insolente.
     */
    LIGHTNING,
    /**
     * Maldicion magica tematica segun el pecado del dios (ceguera, fatiga, debilidad, hambre, lentitud).
     * Disparado por falta de respeto, desprecio hacia su dominio o cobardia.
     */
    CURSE,
    /**
     * Onda expansiva sagrada que empuja y derriba violentamente al jugador.
     * Disparado por insolencia o blasfemia frente a un Altar o Avatar.
     */
    REPULSION,
    /**
     * Bendicion de sanacion inmediata, regeneracion y saciedad de hambre.
     * Disparado por suplicas sinceras de piedad, alabanzas profundas o promesas de devocion.
     */
    BLESSING,
    /**
     * Invocacion y descenso fisico del Avatar del Dios ante el jugador.
     * Disponible una vez por dia (24,000 ticks) cuando el jugador alcanza Favor +100 (Campeon Supremo)
     * y llama expresamente al dios en el chat.
     */
    SUMMON_AVATAR;

    public static DivinePhysicalAction fromString(String val) {
        if (val == null) {
            return NONE;
        }
        try {
            return valueOf(val.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
