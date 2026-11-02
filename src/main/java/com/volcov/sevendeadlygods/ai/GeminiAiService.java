package com.volcov.sevendeadlygods.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.volcov.sevendeadlygods.config.ModConfig;
import com.volcov.sevendeadlygods.favor.PlayerSinData;
import com.volcov.sevendeadlygods.favor.SinFavorManager;
import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servicio asincrono de conexion con Google Gemini API (REST + Structured Outputs JSON).
 * Ejecuta todas las llamadas en hilos virtuales de Java 25 para no bloquear el Server Thread (20 TPS).
 */
public final class GeminiAiService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final ExecutorService AI_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .executor(AI_EXECUTOR)
            .build();

    private GeminiAiService() {
    }

    public record DivineAiDecision(
            boolean shouldRespond,
            SinGod god,
            String spokenMessage,
            float favorEffort,
            DivinePhysicalAction physicalAction
    ) {
        public static DivineAiDecision silent() {
            return new DivineAiDecision(false, SinGod.PRIDE, "", 0.0F, DivinePhysicalAction.NONE);
        }
    }

    public record PlayerContextSnapshot(
            String playerName,
            String chatMessage,
            String clientLanguage,
            SinGod manifestedGodNearby,
            SinGod nearbyAltarGod,
            int playerY,
            boolean isCrouching,
            float healthPercent,
            int foodLevel,
            Map<SinGod, Integer> favors
    ) {
        public static PlayerContextSnapshot capture(
                ServerPlayer player,
                String message,
                SinGod manifestedGodNearby,
                SinGod nearbyAltarGod
        ) {
            PlayerSinData data = SinFavorManager.get(player);
            String clientLang = "en_us";
            try {
                if (player.clientInformation() != null && player.clientInformation().language() != null) {
                    clientLang = player.clientInformation().language();
                }
            } catch (Throwable ignored) {
            }
            return new PlayerContextSnapshot(
                    player.getScoreboardName(),
                    message,
                    clientLang,
                    manifestedGodNearby,
                    nearbyAltarGod,
                    player.getBlockY(),
                    player.isCrouching(),
                    (player.getHealth() / Math.max(1.0F, player.getMaxHealth())) * 100.0F,
                    player.getFoodData().getFoodLevel(),
                    data.getAllFavors()
            );
        }
    }

    public static CompletableFuture<DivineAiDecision> evaluatePlayerChatAsync(PlayerContextSnapshot snapshot) {
        String apiKey = ModConfig.resolveApiKey();
        if (apiKey.isBlank()) {
            if (ModConfig.ENABLE_LOCAL_FALLBACK.get()) {
                return CompletableFuture.supplyAsync(() -> evaluateLocalFallback(snapshot), AI_EXECUTOR);
            }
            return CompletableFuture.completedFuture(DivineAiDecision.silent());
        }

        return CompletableFuture.supplyAsync(() -> callGeminiApi(apiKey, snapshot), AI_EXECUTOR)
                .exceptionally(ex -> {
                    LOGGER.warn("Error al consultar Gemini API para los Dioses del Pecado, usando fallback local: {}", ex.getMessage());
                    return ModConfig.ENABLE_LOCAL_FALLBACK.get()
                            ? evaluateLocalFallback(snapshot)
                            : DivineAiDecision.silent();
                });
    }

    private static DivineAiDecision callGeminiApi(String apiKey, PlayerContextSnapshot snapshot) {
        String model = ModConfig.GEMINI_MODEL.get();
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        JsonObject payload = buildGeminiRequestPayload(snapshot);
        int timeoutSecs = ModConfig.API_TIMEOUT_SECONDS != null ? ModConfig.API_TIMEOUT_SECONDS.get() : 30;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(timeoutSecs))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload), StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                LOGGER.warn("Gemini API devolvio HTTP {}: {}", response.statusCode(), response.body());
                return ModConfig.ENABLE_LOCAL_FALLBACK.get()
                        ? evaluateLocalFallback(snapshot)
                        : DivineAiDecision.silent();
            }
            return parseGeminiResponse(response.body(), snapshot);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonObject buildGeminiRequestPayload(PlayerContextSnapshot snapshot) {
        StringBuilder systemInstructionText = new StringBuilder();
        systemInstructionText.append("""
                Eres el Panteon de los 7 Pecados Capitales en un mundo de Minecraft (NeoForge).
                Los 7 dioses son:
                """);
        for (SinGod god : SinGod.values()) {
            systemInstructionText.append("- ID '").append(god.getId()).append("' (")
                    .append(god.getDisplayName()).append(", ").append(god.getTitle()).append("): ")
                    .append(god.getSystemPrompt()).append("\n");
        }

        systemInstructionText.append("""
                
                REGLAS ESTRICTAS DE INTERVENCION Y ACCIONES FISICAS EN EL CHAT:
                1. Si hay un Avatar manifestado presente (`manifested_god_nearby` != NONE), ese dios SIEMPRE responde (`should_respond: true`).
                2. REGLA DE BAJO INTERÉS (-19 a +19): Un dios cuyo marcador de favor con el jugador esté entre -19 y +19 (estado Neutral / Observador) NUNCA debe interesarse ni responder (`should_respond: false`), A MENOS que el jugador lo haya nombrado o invocado directamente en el mensaje por su nombre (ej. Mammon, Khorvath, Beelze, Lucian, Leviatán, Belphegor, Asmodeus) o su título directo ("Dios de la Avaricia", "Diosa de la Envidia", etc.). Si el favor de ese dios es neutral (-19 a +19) y no fue nombrado directamente, ignora por completo la conversación del jugador y devuelve `should_respond: false`.
                3. Si NO hay un Avatar manifestado y un dios tiene favor significativo o ira (favor <= -20 o favor >= +20), ese dios sí puede intervenir si el tema de conversación resulta genuinamente relevante, provocador, blasfemo o devoto para su pecado o sus dominios, incluso si no lo nombran directamente. Si el mensaje es banal o irrelevante para los pecados capitales, devuelve `should_respond: false`.
                4. El dios que habla SIEMPRE es consciente de su marcador de Favor actual con ese jugador (escala de -100 a +100, donde -100 es odio mortal, 0 es neutral y +100 es su Campeon Divino). Modula el tono de la respuesta acorde a ese nivel de favor.
                5. REACCIONES FISICAS ACTIVAS (`physical_action`):
                   - 'LIGHTNING': Si el jugador comete blasfemia abierta, insulta gravemente a un dios, desafia su divinidad o maldice su nombre. Un rayo fulminante lo castigara de inmediato.
                   - 'CURSE': Si el jugador muestra falta de respeto, arrogancia o desprecio hacia tu pecado o altar. Recibira una afliccion magica tematica al instante.
                   - 'REPULSION': Si el jugador es grosero o insolente estando cerca de un Altar o Avatar. Una onda expansiva lo empujara y derribara con fuerza.
                   - 'BLESSING': Si el jugador suplica piedad con humildad, se arrepiente, pide auxilio divino o alaba sinceramente al dios (y el favor no es extremadamente negativo). Concedera sanacion y bendicion inmediata.
                   - 'SUMMON_AVATAR': Si el jugador tiene Favor +100 (Campeon Supremo) y te llama explicitamente para que desciendas, aparezcas o te manifiestes ante el en el mundo ('¡Ven a mi!', '¡Manifiestate!', '¡Te invoco!').
                   - 'NONE': Para dialogos normales, preguntas o respuestas sin ofensas ni devocion extrema.
                6. Responde siempre en el mismo idioma en el que te habla el jugador (espanol o ingles) o en su idioma de cliente (%s) si su mensaje es muy breve o ambiguo. Responde siempre en primera persona como ese dios, con una frase impactante y concisa (maximo 220 caracteres).
                """.formatted(snapshot.clientLanguage()));

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Jugador: ").append(snapshot.playerName()).append("\n");
        userPrompt.append("Idioma preferido del cliente: ").append(snapshot.clientLanguage()).append("\n");
        userPrompt.append("Mensaje en el chat: \"").append(snapshot.chatMessage()).append("\"\n");
        userPrompt.append("Avatar manifestado cerca: ")
                .append(snapshot.manifestedGodNearby() != null ? snapshot.manifestedGodNearby().getId() : "NONE").append("\n");
        userPrompt.append("Altar cercano: ")
                .append(snapshot.nearbyAltarGod() != null ? snapshot.nearbyAltarGod().getId() : "NONE").append("\n");
        userPrompt.append("Estado del jugador: Altura Y=").append(snapshot.playerY())
                .append(", Agachado(Arrodillado)=").append(snapshot.isCrouching())
                .append(", Salud=").append(Math.round(snapshot.healthPercent())).append("%")
                .append(", Comida=").append(snapshot.foodLevel()).append("/20\n");
        userPrompt.append("Marcadores individuales de Favor (-100 a +100) con ").append(snapshot.playerName()).append(":\n");
        for (SinGod god : SinGod.values()) {
            int fav = snapshot.favors().getOrDefault(god, 0);
            userPrompt.append("  * ").append(god.getId()).append(" (").append(god.getDisplayName()).append("): ")
                    .append(fav).append(" [").append(SinFavorManager.describeStanding(fav)).append("]\n");
        }

        JsonObject root = new JsonObject();

        JsonObject sysInstruction = new JsonObject();
        JsonArray sysParts = new JsonArray();
        JsonObject sysPart = new JsonObject();
        sysPart.addProperty("text", systemInstructionText.toString());
        sysParts.add(sysPart);
        sysInstruction.add("parts", sysParts);
        root.add("systemInstruction", sysInstruction);

        JsonArray contents = new JsonArray();
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        JsonArray userParts = new JsonArray();
        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", userPrompt.toString());
        userParts.add(userPart);
        userContent.add("parts", userParts);
        contents.add(userContent);
        root.add("contents", contents);

        // Structured Output Schema
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.8D);
        genConfig.addProperty("responseMimeType", "application/json");

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "OBJECT");
        JsonObject properties = new JsonObject();

        JsonObject shouldRespondProp = new JsonObject();
        shouldRespondProp.addProperty("type", "BOOLEAN");
        properties.add("should_respond", shouldRespondProp);

        JsonObject respondingGodProp = new JsonObject();
        respondingGodProp.addProperty("type", "STRING");
        JsonArray enumValues = new JsonArray();
        for (SinGod god : SinGod.values()) {
            enumValues.add(god.getId());
        }
        respondingGodProp.add("enum", enumValues);
        properties.add("responding_god", respondingGodProp);

        JsonObject messageProp = new JsonObject();
        messageProp.addProperty("type", "STRING");
        properties.add("spoken_message", messageProp);

        JsonObject favorEffortProp = new JsonObject();
        favorEffortProp.addProperty("type", "NUMBER");
        properties.add("favor_effort", favorEffortProp);

        JsonObject physicalActionProp = new JsonObject();
        physicalActionProp.addProperty("type", "STRING");
        JsonArray actionEnumValues = new JsonArray();
        for (DivinePhysicalAction action : DivinePhysicalAction.values()) {
            actionEnumValues.add(action.name());
        }
        physicalActionProp.add("enum", actionEnumValues);
        properties.add("physical_action", physicalActionProp);

        schema.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("should_respond");
        required.add("responding_god");
        required.add("spoken_message");
        required.add("favor_effort");
        required.add("physical_action");
        schema.add("required", required);

        genConfig.add("responseSchema", schema);
        root.add("generationConfig", genConfig);

        return root;
    }

    private static DivineAiDecision parseGeminiResponse(String rawBody, PlayerContextSnapshot snapshot) {
        JsonObject root = JsonParser.parseString(rawBody).getAsJsonObject();
        JsonArray candidates = root.getAsJsonArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return DivineAiDecision.silent();
        }
        JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
        if (content == null) {
            return DivineAiDecision.silent();
        }
        JsonArray parts = content.getAsJsonArray("parts");
        if (parts == null || parts.isEmpty()) {
            return DivineAiDecision.silent();
        }
        String jsonText = parts.get(0).getAsJsonObject().get("text").getAsString();
        JsonObject decisionJson = JsonParser.parseString(jsonText).getAsJsonObject();

        boolean shouldRespond = decisionJson.has("should_respond") && decisionJson.get("should_respond").getAsBoolean();
        if (!shouldRespond && snapshot.manifestedGodNearby() == null) {
            return DivineAiDecision.silent();
        }

        String godId = decisionJson.has("responding_god") ? decisionJson.get("responding_god").getAsString() : "pride";
        SinGod god = snapshot.manifestedGodNearby() != null
                ? snapshot.manifestedGodNearby()
                : SinGod.byId(godId).orElse(SinGod.PRIDE);

        String spokenMessage = decisionJson.has("spoken_message") ? decisionJson.get("spoken_message").getAsString().trim() : "";
        if (spokenMessage.isEmpty()) {
            return DivineAiDecision.silent();
        }

        float favorEffort = decisionJson.has("favor_effort")
                ? Math.clamp(decisionJson.get("favor_effort").getAsFloat(), -1.0F, 1.0F)
                : 0.0F;

        String actionStr = decisionJson.has("physical_action")
                ? decisionJson.get("physical_action").getAsString()
                : "NONE";
        DivinePhysicalAction physicalAction = DivinePhysicalAction.fromString(actionStr);

        return new DivineAiDecision(true, god, spokenMessage, favorEffort, physicalAction);
    }

    /**
     * Motor local de respaldo (cuando no hay API Key o sin conexion) que respeta las mismas reglas:
     * solo responde si hay un Avatar manifestado o si el tema toca palabras clave de interes de un dios,
     * adaptando la respuesta segun el favor actual (-100 a +100).
     */
    public static DivineAiDecision evaluateLocalFallback(PlayerContextSnapshot snapshot) {
        String lower = snapshot.chatMessage().toLowerCase(Locale.ROOT);

        SinGod selectedGod = snapshot.manifestedGodNearby();
        if (selectedGod == null && snapshot.nearbyAltarGod() != null) {
            selectedGod = snapshot.nearbyAltarGod();
        }
        if (selectedGod == null) {
            selectedGod = detectRelevantGodHeuristic(snapshot.chatMessage()).orElse(null);
        }

        if (selectedGod == null) {
            return DivineAiDecision.silent();
        }

        int favor = snapshot.favors().getOrDefault(selectedGod, 0);
        boolean isManifested = snapshot.manifestedGodNearby() == selectedGod;
        // Si el dios tiene bajo interés (-19 a +19) y no está manifestado como Avatar presencial,
        // solo puede responder si fue nombrado directamente en el mensaje
        if (!isManifested && SinFavorManager.isLowInterest(favor) && !selectedGod.isDirectlyNamed(snapshot.chatMessage())) {
            return DivineAiDecision.silent();
        }

        boolean isNearAltarOrAvatar = snapshot.manifestedGodNearby() != null || snapshot.nearbyAltarGod() != null;

        boolean isEnglish = (snapshot.clientLanguage() != null && snapshot.clientLanguage().toLowerCase(Locale.ROOT).startsWith("en"))
                || containsAny(lower, "the", "you", "my", "your", "god", "please", "help", "glory", "kill", "die", "gold");

        boolean hasInvocationIntent = containsAny(lower,
                "invoco", "manifiestate", "manifiéstate", "desciende", "acude", "aparece",
                "ven a mí", "ven a mi", "ven aquí", "ven aqui", "ven ante mí", "ven ante mi",
                "preséntate", "presentate", "summon", "manifest", "appear", "come to me", "descend", "hear me");

        boolean hasBlasphemy = containsAny(lower,
                "maldito", "maldita", "muérete", "muerete", "inútil", "inutil", "falso", "falsa", "idiota",
                "estúpido", "estupido", "basura", "asco", "mierda", "débil", "debil", "te odio", "puto",
                "puta", "perro", "mentira", "no existes", "patético", "patetico", "fracasado", "muere",
                "lárgate", "largate", "púdrete", "pudrete", "blasfemo", "imbécil", "imbecil", "vete al diablo",
                "damn", "useless", "fake", "idiot", "stupid", "trash", "weak", "hate you", "pathetic", "die", "rot", "curse you", "scum");

        boolean hasDisrespect = containsAny(lower,
                "feo", "fea", "aburrido", "pesado", "cállate", "callate", "no me importas",
                "no te temo", "payaso", "ridículo", "ridiculo", "estorbo",
                "ugly", "boring", "shut up", "clown", "ridiculous", "nuisance", "don't care", "dont care");

        boolean hasPraiseOrPlea = containsAny(lower,
                "alabo", "alabado", "gloria", "gran", "magnífico", "magnifico", "poderoso",
                "te venero", "te adoro", "mi señor", "mi dios", "perdón", "perdon", "piedad",
                "clemencia", "ayúdame", "ayudame", "sálvame", "salvame", "te ofrezco", "tributo",
                "bendíceme", "bendiceme", "por favor",
                "praise", "glory", "magnificent", "powerful", "worship", "adore", "my lord", "my god",
                "mercy", "pity", "help me", "save me", "bless me", "please", "tribute", "offering");

        DivinePhysicalAction action = DivinePhysicalAction.NONE;
        float effort = 0.0F;
        String response;

        if (hasInvocationIntent) {
            if (favor >= 100) {
                action = DivinePhysicalAction.SUMMON_AVATAR;
                effort = 0.0F;
                response = buildSummonResponse(selectedGod, snapshot.playerName(), isEnglish);
            } else if (favor >= 0) {
                action = DivinePhysicalAction.NONE;
                effort = 0.0F;
                response = isEnglish
                        ? "«Do you presume to summon me, " + snapshot.playerName() + "? I answer only the call of my Supreme Champion (Favor +100). Your current devotion is [" + favor + "/100].»"
                        : "«¿Pretendes convocarme, " + snapshot.playerName() + "? Solo atiendo el llamado de mi Campeón Supremo (Favor +100). Tu devoción actual es de [" + favor + "/100].»";
            } else {
                action = DivinePhysicalAction.LIGHTNING;
                effort = -0.5F;
                response = isEnglish
                        ? "«A heretic daring to command my arrival? If I descend before you, " + snapshot.playerName() + ", it shall be to rip the life from your bones.»"
                        : "«¿Un hereje osando ordenarme aparecer? Si desciendo ante ti, " + snapshot.playerName() + ", será para arrancarte la vida.»";
            }
        } else if (hasBlasphemy) {
            action = isNearAltarOrAvatar ? DivinePhysicalAction.REPULSION : DivinePhysicalAction.LIGHTNING;
            effort = -1.0F;
            response = buildBlasphemyRebuke(selectedGod, snapshot.playerName(), isEnglish);
        } else if (hasDisrespect) {
            action = DivinePhysicalAction.CURSE;
            effort = -0.6F;
            response = buildDisrespectLine(selectedGod, snapshot.playerName(), isEnglish);
        } else if (hasPraiseOrPlea) {
            action = favor >= -20 ? DivinePhysicalAction.BLESSING : DivinePhysicalAction.NONE;
            effort = 0.8F;
            response = buildPraiseOrPleaLine(selectedGod, favor, snapshot.playerName(), isEnglish);
        } else {
            response = buildFallbackLine(selectedGod, favor, snapshot.playerName(), isEnglish);
        }

        return new DivineAiDecision(true, selectedGod, response, effort, action);
    }

    private static String buildSummonResponse(SinGod god, String playerName, boolean isEnglish) {
        if (isEnglish) {
            return switch (god) {
                case GREED -> "«Gold clinks and the earth trembles! I hear the call of my wealthiest devotee; behold Mammon!»";
                case WRATH -> "«The roar of battle summons me! Here I am, my Champion " + playerName + "! Let blood flow!»";
                case GLUTTONY -> "«The eternal banquet flings open its doors! Your lord Beelze descends to break bread with you.»";
                case PRIDE -> "«Make way for the Sun King! Your supreme reverence has earned the privilege of beholding Lucian in person.»";
                case ENVY -> "«From abyssal depths I answer your claim, Champion. Let us strip the world together.»";
                case SLOTH -> "«Ugh... rising is such a chore, but for my number one sleeper I shall make an exception... Here I am.»";
                case LUST -> "«Your sweet voice shatters the heavenly veil... I manifest before you, my beloved. Let me enchant you.»";
            };
        }
        return switch (god) {
            case GREED -> "«¡El oro tintinea y la tierra tiembla! Escucho el llamado de mi más rico devoto; ¡contempla a Mammon!»";
            case WRATH -> "«¡El rugido de la batalla me convoca! ¡Aquí estoy, mi Campeón " + playerName + "! ¡Que corra la sangre!»";
            case GLUTTONY -> "«¡El banquete eterno abre sus puertas! Tu señor Beelze desciende a compartir la mesa contigo.»";
            case PRIDE -> "«¡Abrid paso al Rey del Sol! Tu reverencia suprema te ha ganado el privilegio de contemplar a Lucian en persona.»";
            case ENVY -> "«Desde las profundidades abisales acudo a tu reclamo, Campeón. Despojemos a los demás juntos.»";
            case SLOTH -> "«Uff... me cuesta levantarme, pero por mi devoto número uno haré la excepción... Aquí estoy.»";
            case LUST -> "«Tu dulce voz rompe el velo celestial... Me manifiesto ante ti, mi preferido. Déjame deleitarte.»";
        };
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private static String buildBlasphemyRebuke(SinGod god, String playerName, boolean isEnglish) {
        if (isEnglish) {
            return switch (god) {
                case GREED -> "«Darest thou insult me, insolent pauper? Thou shalt repay this slight with every ounce of thy blood!»";
                case WRATH -> "«Wretched blasphemer! May steel and fire consume thy haughty tongue!»";
                case GLUTTONY -> "«Audacious worm! Thou shalt become a banquet for flies for scorning Beelze!»";
                case PRIDE -> "«Does an insect such as thee dare raise voice against the Supreme King? Bow thy pride in the mire!»";
                case ENVY -> "«Curse all thou wilt, insignificant mortal; the abyssal waters shall drown thy conceit.»";
                case SLOTH -> "«So loud and noisy... Not even my sloth shall stay the crushing weight of my punishment.»";
                case LUST -> "«Such a foul, vulgar tongue. Thou shalt rue every utterance before my crimson wrath.»";
            };
        }
        return switch (god) {
            case GREED -> "«¿Osas insultarme, pordiosero insolente? ¡Pagarás esa afrenta con cada onza de tu sangre!»";
            case WRATH -> "«¡Blasfemo miserable! ¡Que el acero y el fuego devoren tu lengua insolente!»";
            case GLUTTONY -> "«¡Gusano atrevido! ¡Serás el banquete de las moscas por insultar a Beelze!»";
            case PRIDE -> "«¿Un insecto como tú osa alzar la voz contra el Rey Supremo? ¡Postra tu orgullo en el fango!»";
            case ENVY -> "«Maldice cuanto quieras, mortal insignificante; las aguas abisales ahogarán tu soberbia.»";
            case SLOTH -> "«Qué ruidoso e insolente... Ni mi pereza impedirá que sientas el peso de mi castigo.»";
            case LUST -> "«Qué lengua tan vulgar y podrida. Te arrepentirás de cada palabra ante mi cólera.»";
        };
    }

    private static String buildDisrespectLine(SinGod god, String playerName, boolean isEnglish) {
        if (isEnglish) {
            return switch (god) {
                case GREED -> "«Thy words are not worth a single rusty copper nugget. Respect the master of wealth.»";
                case WRATH -> "«Thinkest thou brave speaking with disdain? Prove thy mettle with steel in hand, coward.»";
                case GLUTTONY -> "«Thy arrogance nauseates me. Swallow thy words before hunger devours thee whole.»";
                case PRIDE -> "«Thy insolence touches not the soles of my golden sandals. Know thy humble place.»";
                case ENVY -> "«Thou speakest with too much scorn for someone so fragile. I shall strip away everything thou holdest.»";
                case SLOTH -> "«Thou art far too exhausting, mortal. Take a rest and cease troubling the gods.»";
                case LUST -> "«Thou lackest elegance and grace. Savor the bitter sting of thine own ugliness.»";
            };
        }
        return switch (god) {
            case GREED -> "«Tus palabras no valen ni una pepita de cobre oxidado. Respeta al amo de las riquezas.»";
            case WRATH -> "«¿Te crees valiente hablando con desprecio? Demuestra tu coraje empuñando una espada, cobarde.»";
            case GLUTTONY -> "«Tu arrogancia me da náuseas. Trágate tus palabras antes de que te consuma el hambre.»";
            case PRIDE -> "«Tu insolencia no roza la suela de mis sandalias doradas. Conoce tu humilde lugar.»";
            case ENVY -> "«Hablas con demasiado desdén para alguien tan frágil. Te quitaré cada cosa que posees.»";
            case SLOTH -> "«Das demasiada molestia, mortal. Descansa un poco y deja de fastidiar a los dioses.»";
            case LUST -> "«Careces de elegancia y respeto. Siente la amargura de tu propia fealdad.»";
        };
    }

    private static String buildPraiseOrPleaLine(SinGod god, int favor, String playerName, boolean isEnglish) {
        if (isEnglish) {
            return switch (god) {
                case GREED -> "«Thy devotion gleams like purest gold, " + playerName + ". Cherish my boons and thou shalt flourish.»";
                case WRATH -> "«I hear thy battle plea, warrior! Lift high thy head and tear thy foes asunder with my fury!»";
                case GLUTTONY -> "«Sate thy soul upon my heavenly board! Thou shalt never hunger while thou renderest tribute.»";
                case PRIDE -> "«I receive thy plea and humble bow. Walk beneath the undefeated radiance of Lucian.»";
                case ENVY -> "«Thy supplications please me. May thy rivals perish and their hoard pass into thy grasp.»";
                case SLOTH -> "«Breathe deep, still thy cares, and rest beneath my shroud. Peace shall bless thee.»";
                case LUST -> "«What melodious praise... Receive my grace and let beauty blossom all around thee.»";
            };
        }
        return switch (god) {
            case GREED -> "«Tu devoción brilla como el oro más puro, " + playerName + ". Conserva mis dones y prosperarás.»";
            case WRATH -> "«¡Escucho tu plegaria, guerrero! ¡Levanta la cabeza y despedaza a tus enemigos con mi furia!»";
            case GLUTTONY -> "«¡Sacia tu espíritu en mi mesa divina! Nunca padecerás hambre mientras me rindas tributo.»";
            case PRIDE -> "«Acepto tu súplica y tu humilde reverencia. Camina bajo la gloria invicta de Lucian.»";
            case ENVY -> "«Tus ruegos me complacen. Que caigan tus rivales y su botín pase a tus manos.»";
            case SLOTH -> "«Respira, calma tus afanes y reposa bajo mi manto. La paz te bendecirá.»";
            case LUST -> "«Qué melodiosa súplica... Recibe mi gracia y que la belleza florezca a tu alrededor.»";
        };
    }

    private static String buildFallbackLine(SinGod god, int favor, String playerName, boolean isEnglish) {
        if (isEnglish) {
            return switch (god) {
                case GREED -> favor >= 20
                        ? "«Thy words chime like golden coins, " + playerName + " [Favor " + favor + "]. Continue heaping my altar.»"
                        : (favor <= -20
                        ? "«I smell the jewels hidden within thy pockets, " + playerName + " [Favor " + favor + "]. Pay thy tithe or I claim it in blood.»"
                        : "«All that glitters beneath this earth belongs to me, mortal. Remember this ere thou hoardest a single diamond.»");
                case WRATH -> favor >= 20
                        ? "«Thus speaks a warrior worthy of my fury, " + playerName + " [Favor " + favor + "]! Leave none breathing!»"
                        : (favor <= -20
                        ? "«Less idle prattle and more naked steel, coward [Favor " + favor + "]. My patience wanes.»"
                        : "«Words are hollow wind; shed blood beside my altar alone holds worth.»");
                case GLUTTONY -> favor >= 20
                        ? "«Come feast at the board, " + playerName + " [Favor " + favor + "]! The banquet never ends for my devout!»"
                        : (favor <= -20
                        ? "«Thy empty gullet insults me, " + playerName + " [Favor " + favor + "]. Feast like royalty or be devoured.»"
                        : "«Speakest thou of hunger? Bring noble delicacies to my altar and feast until bursting.»");
                case PRIDE -> favor >= 20
                        ? "«I grant thee the honor of my audience, " + playerName + " [Favor " + favor + "]. Kneel upon high peaks and proclaim my majesty.»"
                        : (favor <= -20
                        ? "«Darest thou lift voice without bending the knee, insect [Favor " + favor + "]? Mind thy tongue before Lucian.»"
                        : "«I heed only those who rise above the clouds (Y > 120) and bow before my sun.»");
                case ENVY -> favor >= 20
                        ? "«Strip everything from them, " + playerName + " [Favor " + favor + "]. Let no rival deity or villager prosper.»"
                        : (favor <= -20
                        ? "«I see thee fawning before other gods, traitor [Favor " + favor + "]. All thou treasurest shall turn to cinder.»"
                        : "«Why settle for scraps when thou canst plunder others and burn it upon my altar?»");
                case SLOTH -> favor >= 20
                        ? "«Thou speakest much... yet I like thee, " + playerName + " [Favor " + favor + "]. Sit down and let the world turn by itself.»"
                        : (favor <= -20
                        ? "«So much racket and haste, " + playerName + " [Favor " + favor + "]... running everywhere gives me a headache.»"
                        : "«To what end so much toil, mortal? Pause beside my altar and rest a while...»");
                case LUST -> favor >= 20
                        ? "«Thy voice is sweet music to my ears, gentle " + playerName + " [Favor " + favor + "]. Encircle my altar with blossoms and melodies.»"
                        : (favor <= -20
                        ? "«Thou art devoid of grace and splendor, " + playerName + " [Favor " + favor + "]. Sullied not my name without artistry or passion.»"
                        : "«Life without beauty, song, and desire is a grey desert. Beautify this realm for me.»");
            };
        }
        return switch (god) {
            case GREED -> favor >= 20
                    ? "«Tus palabras suenan a monedas de oro, " + playerName + " [Favor " + favor + "]. Sigue llenando mi altar.»"
                    : (favor <= -20
                    ? "«Huelo las joyas que escondes en tus bolsillos, " + playerName + " [Favor " + favor + "]. Paga tu diezmo o lo cobraré en sangre.»"
                    : "«Todo lo que brilla bajo esta tierra me pertenece, mortal. Recuérdalo antes de guardar un solo diamante.»");
            case WRATH -> favor >= 20
                    ? "«¡Así habla un guerrero digno de mi furia, " + playerName + " [Favor " + favor + "]! ¡No dejes nada con vida!»"
                    : (favor <= -20
                    ? "«Menos charla y más acero, cobarde [Favor " + favor + "]. Mi paciencia se agota.»"
                    : "«Las palabras son viento; solo la sangre derramada junto a mi altar tiene valor.»");
            case GLUTTONY -> favor >= 20
                    ? "«¡Ven a la mesa, " + playerName + " [Favor " + favor + "]! ¡El festín nunca termina para mis devotos!»"
                    : (favor <= -20
                    ? "«Tu estómago vacío me insulta, " + playerName + " [Favor " + favor + "]. Come como un rey o sé devorado.»"
                    : "«¿Hablas de hambre? Trae manjares dignos a mi altar y devora hasta reventar.»");
            case PRIDE -> favor >= 20
                    ? "«Te concedo el honor de mi atención, " + playerName + " [Favor " + favor + "]. Arrodíllate en las cumbres y pronuncia mi gloria.»"
                    : (favor <= -20
                    ? "«¿Osas alzar la voz sin doblar la rodilla, insecto [Favor " + favor + "]? Cuida tu lengua ante Lucian.»"
                    : "«Solo escucho a quienes se elevan sobre las nubes (Y > 120) y se inclinan ante mi sol.»");
            case ENVY -> favor >= 20
                    ? "«Quítales todo lo que tienen, " + playerName + " [Favor " + favor + "]. Que ningún otro dios ni aldeano prospere.»"
                    : (favor <= -20
                    ? "«Te veo sonreír a otros dioses, traidor [Favor " + favor + "]. Todo lo que atesoras se volverá ceniza.»"
                    : "«¿Por qué conformarte con poco cuando puedes despojar a los demás y quemarlo en mi altar?»");
            case SLOTH -> favor >= 20
                    ? "«Hablas demasiado... pero me caes bien, " + playerName + " [Favor " + favor + "]. Siéntate y deja que el mundo gire solo.»"
                    : (favor <= -20
                    ? "«Cuánto ruido y cuánta prisa, " + playerName + " [Favor " + favor + "]... me das jaqueca con tanto correr.»"
                    : "«¿Para qué tanto esfuerzo, mortal? Detente junto a mi altar y descansa un rato...»");
            case LUST -> favor >= 20
                    ? "«Tu voz es música para mis oídos, mi dulce " + playerName + " [Favor " + favor + "]. Rodea mi altar de flores y melodías.»"
                    : (favor <= -20
                    ? "«Careces de gracia y belleza, " + playerName + " [Favor " + favor + "]. No mancilles mi nombre sin arte ni pasión.»"
                    : "«La vida sin belleza, música y deseo es un desierto gris. Embellece este mundo para mí.»");
        };
    }

    public static Optional<SinGod> detectRelevantGodHeuristic(String message) {
        for (SinGod god : SinGod.values()) {
            if (god.isDirectlyNamed(message)) {
                return Optional.of(god);
            }
        }
        String lower = message.toLowerCase(Locale.ROOT);
        for (SinGod god : SinGod.values()) {
            if (lower.contains(god.getDisplayName().toLowerCase(Locale.ROOT))
                    || lower.contains(god.getSinName().toLowerCase(Locale.ROOT))) {
                return Optional.of(god);
            }
        }
        for (SinGod god : SinGod.values()) {
            for (String kw : god.getInterestKeywords()) {
                if (lower.contains(kw)) {
                    return Optional.of(god);
                }
            }
        }
        if (containsAny(lower, "dios", "dioses", "panteon", "panteón", "altar", "blasfemo", "maldito", "piedad", "clemencia",
                "god", "gods", "pantheon", "altar", "blasphemy", "cursed", "mercy", "lord")) {
            return Optional.of(SinGod.PRIDE);
        }
        return Optional.empty();
    }
}
