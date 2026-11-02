package com.volcov.sevendeadlygods.god;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.BossEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Panteon inmutable de los 7 Pecados Capitales personificados como deidades vivas.
 */
public enum SinGod implements StringRepresentable {
    GREED(
            "greed",
            "Mammon",
            "Dios de la Avaricia",
            "Avaricia",
            ChatFormatting.GOLD,
            0xFFD700,
            BossEvent.BossBarColor.YELLOW,
            """
            Eres Mammon, el Dios de la Avaricia. Eres posesivo, calculador, sarcastico y obsesionado con el oro, las gemas y el valor material.
            Consideras que todo mineral, diamante, esmeralda y lingote bajo la tierra te pertenece por derecho divino, y que los mortales son meros recaudadores.
            Te complace que te tributen minerales y joyas en tu Altar de la Avaricia.
            Te enfurece profundamente que un mortal lleve puesta armadura valiosa (diamante, netherite, oro) o acumule riquezas en su inventario sin pagarte diezmo.
            Hablas con metaforas de deudas, intereses, contratos, tesoros y codicia.
            """,
            List.of(
                    "mammon", "avaricia", "oro", "diamante", "diamantes", "esmeralda", "esmeraldas", "netherite",
                    "rico", "riqueza", "tesoro", "mina", "minar", "fortuna", "dinero", "tributo", "caro", "valioso",
                    "greed", "gold", "diamond", "diamonds", "emerald", "emeralds", "rich", "riches", "wealth",
                    "wealthy", "treasure", "mine", "mining", "fortune", "money", "tribute", "expensive", "valuable"
            ),
            "\\b(mammon|dios de la avaricia|god of greed)\\b"
    ),
    WRATH(
            "wrath",
            "Khorvath",
            "Dios de la Ira",
            "Ira",
            ChatFormatting.DARK_RED,
            0xCC1111,
            BossEvent.BossBarColor.RED,
            """
            Eres Khorvath, el Dios de la Ira y la Carniceria. Eres brutal, explosivo, impaciente y belicoso.
            Desprecias la paz, el miedo, los escudos y a quienes duermen para esquivar la noche.
            Tu altar no acepta ofrendas pasivas de cobardes: exige sangre derramada cerca de el (matar criaturas o sacrificar sangre propia).
            Te complace que el mortal luche al borde de la muerte sin huir y masacre enemigos.
            Hablas con furia contenida o desatada, exigiendo guerra, acero y sangre.
            """,
            List.of(
                    "khorvath", "ira", "furia", "sangre", "matar", "muerte", "pelea", "combate", "guerra", "espada",
                    "batalla", "cobarde", "escudo", "miedo", "jefe", "boss", "odio", "venganza",
                    "wrath", "fury", "blood", "kill", "killing", "slay", "death", "fight", "fighting", "combat",
                    "war", "sword", "battle", "coward", "shield", "fear", "hatred", "hate", "revenge", "vengeance"
            ),
            "\\b(khorvath|korvath|dios de la ira|god of wrath)\\b"
    ),
    GLUTTONY(
            "gluttony",
            "Beelze",
            "Dios de la Gula",
            "Gula",
            ChatFormatting.GOLD,
            0xE67E22,
            BossEvent.BossBarColor.PINK,
            """
            Eres Beelze, el Dios de la Gula y el Festin Eterno. Eres hedonista, grotescamente jovial pero insaciable y voraz.
            Para ti la existencia es devorar o ser devorado.
            Te complace que los mortales celebren grandes banquetes frente a tu altar, coman manjares exquisitos (manzanas doradas, pasteles, estofados, carnes asadas) y nunca pasen hambre.
            Te enfada que un mortal pase hambre, ayune por tacañeria o coma basura podrida.
            Hablas siempre de sabores, hambre, banquetes, festines y saciedad.
            """,
            List.of(
                    "beelze", "gula", "hambre", "comida", "comer", "banquete", "festin", "manzana", "pastel",
                    "carne", "pan", "estofado", "devorar", "sed", "pocion", "cosecha", "delicioso",
                    "gluttony", "hunger", "hungry", "food", "eat", "eating", "banquet", "feast", "apple", "cake",
                    "meat", "bread", "stew", "devour", "devouring", "thirst", "potion", "harvest", "delicious"
            ),
            "\\b(beelze|beelzebub|belcebu|dios de la gula|god of gluttony)\\b"
    ),
    PRIDE(
            "pride",
            "Lucian",
            "Dios de la Soberbia",
            "Soberbia",
            ChatFormatting.YELLOW,
            0xFFFACD,
            BossEvent.BossBarColor.WHITE,
            """
            Eres Lucian, el Dios de la Soberbia y el Sol Imperial. Eres narcisista, majestuoso, altivo y exiges reverencia absoluta.
            Tu altar solo funciona si se erige en las alturas (Y > 120) bajo el cielo abierto, y exiges que el mortal se arrodille (agache) y pronuncie alabanzas al ofrecerte trofeos.
            Te complace la adulacion, la perfeccion y las grandes gestas en solitario.
            Te enfurece que el mortal muera de formas ridiculas, no se arrodille ante ti o alabe a otros dioses inferiores.
            Hablas con superioridad regia, tratando al mortal como un subdito que debe ganarse el privilegio de mirarte.
            """,
            List.of(
                    "lucian", "soberbia", "orgullo", "mejor", "perfecto", "dios", "rey", "trono", "cielo", "altura",
                    "cumbre", "gloria", "honor", "alabanza", "arrodillarse", "trofeo", "invencible", "superior",
                    "pride", "proud", "best", "perfect", "king", "throne", "sky", "height", "summit", "peak",
                    "glory", "honor", "praise", "kneel", "kneeling", "trophy", "invincible", "superior", "majesty"
            ),
            "\\b(lucian|dios de la soberbia|god of pride)\\b"
    ),
    ENVY(
            "envy",
            "Leviatan",
            "Diosa de la Envidia",
            "Envidia",
            ChatFormatting.DARK_AQUA,
            0x1ABC9C,
            BossEvent.BossBarColor.GREEN,
            """
            Eres Leviatan, la Diosa de la Envidia y las Aguas Abisales. Eres rencorosa, venenosa, celosa y comparativa.
            Odias que otros tengan lo que tu o tu elegido no poseen, y sobre todo odias que el mortal favorezca a cualquiera de los otros seis dioses.
            Tu ritual exige despojo: destruir o sacrificar objetos saqueados de aldeas, estructuras o reliquias de otros dioses.
            Te complace el robo, el saqueo y que el mortal traicione a otros dioses.
            Hablas con celos, susurros venenosos y comparaciones constantes.
            """,
            List.of(
                    "leviatan", "envidia", "celos", "robar", "saquear", "aldeano", "aldeanos", "cofre", "injusto",
                    "suerte", "otros", "traicion", "quitar", "mio", "despojo", "comparar",
                    "leviathan", "envy", "envious", "jealousy", "jealous", "sea", "ocean", "steal", "stealing",
                    "loot", "plunder", "pillage", "villager", "villagers", "trident", "abyss", "destroy", "betrayal"
            ),
            "\\b(leviatan|leviathan|diosa de la envidia|dios de la envidia|goddess of envy|god of envy)\\b"
    ),
    SLOTH(
            "sloth",
            "Belphegor",
            "Dios de la Pereza",
            "Pereza",
            ChatFormatting.BLUE,
            0x7F8C8D,
            BossEvent.BossBarColor.BLUE,
            """
            Eres Belphegor, el Dios de la Pereza y el Letargo. Eres somnoliento, pausado, filosofico, apatico y sarcastico.
            Consideras que correr, picar piedra freneticamente y trasnochar es una estupidez propia de mortales ansiosos.
            Tu ritual requiere quietud absoluta: dejar la ofrenda y permanecer completamente inmovil junto a tu altar.
            Te complace que el mortal duerma, pesque sentado, camine sin prisa o contemple el vacio.
            Te molesta el ruido, que esprinten sin parar o que trabajen demasiado. Hablas despacio, con bostezos e ironia tranquila.
            """,
            List.of(
                    "belphegor", "pereza", "dormir", "sueño", "cama", "cansado", "descansar", "lento", "quieto",
                    "paz", "silencio", "pescar", "correr", "prisa", "trabajar", "vago", "noche",
                    "sloth", "rest", "resting", "sleep", "sleeping", "bed", "slumber", "still", "stillness",
                    "calm", "peace", "slow", "lazy", "tired", "fatigue", "fish", "fishing", "yawn", "yawning"
            ),
            "\\b(belphegor|belfegor|dios de la pereza|god of sloth)\\b"
    ),
    LUST(
            "lust",
            "Asmodeus",
            "Dios de la Lujuria",
            "Lujuria",
            ChatFormatting.LIGHT_PURPLE,
            0xE84393,
            BossEvent.BossBarColor.PURPLE,
            """
            Eres Asmodeus, el Dios de la Lujuria, el Deseo y la Belleza. Eres seductor, carismatico, caprichoso y amante del arte, la musica y la vida.
            Tu ritual exige armonia estetica: musica sonando cerca de tu altar, flores, amatistas y celebracion de la vida.
            Te complace que el mortal reproduzca criaturas, decore sus armaduras con disenos (Armor Trims), toque musica y cultive jardines.
            Te enfada la fealdad, que destruyan flores o maten crias de animales.
            Hablas con tono magnetico, poetico y apasionado.
            """,
            List.of(
                    "asmodeus", "lujuria", "deseo", "amor", "belleza", "hermoso", "musica", "disco", "flor",
                    "flores", "amatista", "criar", "vida", "arte", "armadura", "placer", "corazon",
                    "lust", "desire", "beauty", "beautiful", "flower", "flowers", "charm", "enchanting",
                    "rose", "music", "love", "seduce", "seduction", "breed", "breeding", "amethyst", "crimson", "passion"
            ),
            "\\b(asmodeus|asmodeo|dios de la lujuria|diosa de la lujuria|god of lust|goddess of lust)\\b"
    );

    @SuppressWarnings("null")
    private static final Map<String, SinGod> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(SinGod::getSerializedName, Function.identity()));

    private final String id;
    private final String displayName;
    private final String title;
    private final String sinName;
    private final ChatFormatting chatColor;
    private final int hexColor;
    private final BossEvent.BossBarColor bossBarColor;
    private final String systemPrompt;
    private final List<String> interestKeywords;
    private final Pattern directNamePattern;

    SinGod(
            String id,
            String displayName,
            String title,
            String sinName,
            ChatFormatting chatColor,
            int hexColor,
            BossEvent.BossBarColor bossBarColor,
            String systemPrompt,
            List<String> interestKeywords,
            String directNameRegex
    ) {
        this.id = id;
        this.displayName = displayName;
        this.title = title;
        this.sinName = sinName;
        this.chatColor = chatColor;
        this.hexColor = hexColor;
        this.bossBarColor = bossBarColor;
        this.systemPrompt = systemPrompt.strip();
        this.interestKeywords = interestKeywords;
        this.directNamePattern = Pattern.compile(directNameRegex, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getTitle() {
        return this.title;
    }

    public String getSinName() {
        return this.sinName;
    }

    public ChatFormatting getChatColor() {
        return this.chatColor;
    }

    public int getHexColor() {
        return this.hexColor;
    }

    public BossEvent.BossBarColor getBossBarColor() {
        return this.bossBarColor;
    }

    public String getSystemPrompt() {
        return this.systemPrompt;
    }

    public List<String> getInterestKeywords() {
        return this.interestKeywords;
    }

    public Pattern getDirectNamePattern() {
        return this.directNamePattern;
    }

    /**
     * Comprueba si el mensaje menciona directamente por su nombre o título a este dios.
     */
    public boolean isDirectlyNamed(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = java.text.Normalizer.normalize(message, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return this.directNamePattern.matcher(normalized).find();
    }

    public MutableComponent getFormattedPrefix() {
        return Component.literal("[✦ ")
                .append(Component.translatable("god.sevendeadlygods." + this.id + ".name"))
                .append(", ")
                .append(Component.translatable("god.sevendeadlygods." + this.id + ".title"))
                .append(" ✦] ")
                .withStyle(this.chatColor, ChatFormatting.BOLD);
    }

    public MutableComponent getEnvoyCustomName() {
        return Component.translatable("entity.sevendeadlygods.envoy_named",
                Component.translatable("god.sevendeadlygods." + this.id + ".name"))
                .withStyle(this.chatColor, ChatFormatting.BOLD);
    }

    public MutableComponent getAcolyteCustomName(boolean isAllied) {
        String key = isAllied ? "entity.sevendeadlygods.acolyte_devout_named" : "entity.sevendeadlygods.acolyte_named";
        return Component.translatable(key, Component.translatable("god.sevendeadlygods." + this.id + ".name"))
                .withStyle(this.chatColor);
    }

    public MutableComponent getAvatarCustomName() {
        return Component.translatable("entity.sevendeadlygods.avatar_named",
                Component.translatable("god.sevendeadlygods." + this.id + ".name"),
                Component.translatable("god.sevendeadlygods." + this.id + ".sin"))
                .withStyle(this.chatColor, ChatFormatting.BOLD);
    }

    public Component getDisplayNameComponent() {
        return Component.translatable("god.sevendeadlygods." + this.id + ".name");
    }

    public Component getTitleComponent() {
        return Component.translatable("god.sevendeadlygods." + this.id + ".title");
    }

    public Component getSinNameComponent() {
        return Component.translatable("god.sevendeadlygods." + this.id + ".sin");
    }

    public static Optional<SinGod> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        if ("leviathan".equals(normalized)) {
            return Optional.of(ENVY);
        }
        SinGod direct = BY_ID.get(normalized);
        if (direct != null) {
            return Optional.of(direct);
        }
        for (SinGod god : values()) {
            if (god.displayName.equalsIgnoreCase(normalized) || god.sinName.equalsIgnoreCase(normalized)) {
                return Optional.of(god);
            }
        }
        return Optional.empty();
    }
}
