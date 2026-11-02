package com.volcov.sevendeadlygods.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<String> GEMINI_API_KEY;
    public static final ModConfigSpec.ConfigValue<String> GEMINI_MODEL;
    public static final ModConfigSpec.IntValue API_TIMEOUT_SECONDS;
    public static final ModConfigSpec.IntValue CHAT_EVALUATION_COOLDOWN_SECONDS;
    public static final ModConfigSpec.BooleanValue ENABLE_LOCAL_FALLBACK;
    public static final ModConfigSpec.DoubleValue LORE_BOOK_CHEST_CHANCE;
    public static final ModConfigSpec.IntValue LORE_BOOK_FIRST_READ_XP;
    public static final ModConfigSpec.IntValue VILLAGE_RAID_COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue VILLAGE_RAID_AVATAR_CHANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Configuracion de Inteligencia Artificial (Google Gemini) para Seven Deadly Gods")
                .push("gemini_ai");

        GEMINI_API_KEY = builder
                .comment("Clave de API de Google Gemini (AI Studio). Si se deja vacia, se buscara en la variable de entorno GEMINI_API_KEY o GOOGLE_API_KEY.")
                .define("geminiApiKey", "");

        GEMINI_MODEL = builder
                .comment("Modelo de Gemini a utilizar para las respuestas y decisiones de los dioses.")
                .define("geminiModel", "gemini-3.1-flash-lite");

        API_TIMEOUT_SECONDS = builder
                .comment("Tiempo maximo de espera (en segundos) para la respuesta de la API de Gemini antes de usar fallback.")
                .defineInRange("apiTimeoutSeconds", 30, 5, 120);

        CHAT_EVALUATION_COOLDOWN_SECONDS = builder
                .comment("Tiempo minimo en segundos entre intervenciones espontaneas de los dioses en el chat cuando no estan manifestados.")
                .defineInRange("chatEvaluationCooldownSeconds", 8, 1, 300);

        ENABLE_LOCAL_FALLBACK = builder
                .comment("Si es true, cuando no haya API Key configurada o falle la red, los dioses usaran un motor de respuestas tematicas local.")
                .define("enableLocalFallback", true);

        builder.pop();

        builder.comment("Configuracion de Lore y Mundo")
                .push("world");

        LORE_BOOK_CHEST_CHANCE = builder
                .comment("Probabilidad (0.0 a 1.0) de que aparezca un libro sagrado de relato en cofres de estructuras del mundo.")
                .defineInRange("loreBookChestChance", 0.35D, 0.0D, 1.0D);

        LORE_BOOK_FIRST_READ_XP = builder
                .comment("Puntos de experiencia otorgados al jugador al asimilar o leer un libro sagrado por primera vez (por defecto 25 puntos).")
                .defineInRange("loreBookFirstReadXp", 25, 0, 1000);

        VILLAGE_RAID_COOLDOWN_TICKS = builder
                .comment("Enfriamiento minimo en ticks entre asaltos autonomos a una misma aldea consagrada (por defecto 72000 = 3 dias de Minecraft).")
                .defineInRange("villageRaidCooldownTicks", 72000, 1200, 2400000);

        VILLAGE_RAID_AVATAR_CHANCE = builder
                .comment("Probabilidad (0.0 a 1.0) de que el Avatar de un Dios se manifieste fisicamente durante un asedio a una aldea (por defecto 0.05 = 5%).")
                .defineInRange("villageRaidAvatarChance", 0.05D, 0.0D, 1.0D);

        builder.pop();

        SPEC = builder.build();
    }

    private ModConfig() {
    }

    public static String resolveApiKey() {
        String configured = GEMINI_API_KEY.get();
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        String envGemini = System.getenv("GEMINI_API_KEY");
        if (envGemini != null && !envGemini.isBlank()) {
            return envGemini.trim();
        }
        String envGoogle = System.getenv("GOOGLE_API_KEY");
        if (envGoogle != null && !envGoogle.isBlank()) {
            return envGoogle.trim();
        }
        return "";
    }
}
