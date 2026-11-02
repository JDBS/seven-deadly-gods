package com.volcov.sevendeadlygods.lore;

import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Biblioteca sagrada de 49 Libros Escritos (7 libros distintos de 3 páginas por cada uno de los 7 Dioses).
 * Cada libro recoge testimonios y parábolas de tono bíblico sobre mortales que atrajeron la gracia o la cólera
 * de un Pecado Capital, revelando de forma natural e inmersiva los secretos de su culto, sus santuarios y guerras.
 *
 * El contenido de los libros está completamente localizado a través de claves de traducción dinámicas
 * (Component.translatable), soportando tanto Español como Inglés según la configuración del cliente
 * e identificando los tomos de forma unívoca mediante CustomData.
 */
public final class SinLoreLibrary {
    public static final String NBT_LORE_ID = "sevendeadlygods:lore_id";

    public record LoreBookEntry(
            SinGod god,
            String id,
            String defaultTitle,
            String defaultAuthor
    ) {
        public String title() {
            return this.defaultTitle;
        }

        public String author() {
            return this.defaultAuthor;
        }

        public String titleKey() {
            return "book.sevendeadlygods." + this.id + ".title";
        }

        public String authorKey() {
            return "book.sevendeadlygods." + this.id + ".author";
        }

        public String pageKey(int pageIndex) {
            return "book.sevendeadlygods." + this.id + ".page_" + pageIndex;
        }

        public List<Component> pages() {
            return List.of(
                    Component.translatable(pageKey(1)),
                    Component.translatable(pageKey(2)),
                    Component.translatable(pageKey(3))
            );
        }

        public ItemStack toWrittenBookStack() {
            ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
            List<Filterable<Component>> filterablePages = List.of(
                    Filterable.passThrough((Component) Component.translatable(pageKey(1))),
                    Filterable.passThrough((Component) Component.translatable(pageKey(2))),
                    Filterable.passThrough((Component) Component.translatable(pageKey(3)))
            );

            WrittenBookContent content = new WrittenBookContent(
                    Filterable.passThrough(this.defaultTitle),
                    this.defaultAuthor,
                    0,
                    filterablePages,
                    true
            );
            book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
            book.set(DataComponents.ITEM_NAME, Component.translatable(titleKey()));

            CompoundTag tag = new CompoundTag();
            tag.putString(NBT_LORE_ID, this.id);
            book.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            return book;
        }
    }

    private static final Map<SinGod, List<LoreBookEntry>> BOOKS_BY_GOD = new EnumMap<>(SinGod.class);
    private static final Map<String, LoreBookEntry> BOOKS_BY_ID = new HashMap<>();
    private static final List<LoreBookEntry> ALL_BOOKS = new ArrayList<>();

    static {
        for (SinGod god : SinGod.values()) {
            BOOKS_BY_GOD.put(god, new ArrayList<>());
        }
        registerGreedBooks();
        registerWrathBooks();
        registerGluttonyBooks();
        registerPrideBooks();
        registerEnvyBooks();
        registerSlothBooks();
        registerLustBooks();
    }

    private SinLoreLibrary() {
    }

    private static void register(SinGod god, String id, String defaultTitle, String defaultAuthor) {
        LoreBookEntry entry = new LoreBookEntry(god, id, defaultTitle, defaultAuthor);
        BOOKS_BY_GOD.get(god).add(entry);
        BOOKS_BY_ID.put(id, entry);
        ALL_BOOKS.add(entry);
    }

    // =========================================================================
    // 1. AVARICIA (MAMMON) - 7 Testimonios
    // =========================================================================
    private static void registerGreedBooks() {
        register(SinGod.GREED, "greed_1", "Parábola del Herrero", "Escriba Aldric");
        register(SinGod.GREED, "greed_2", "El Peso del Oro Oculto", "Testimonio de Vane");
        register(SinGod.GREED, "greed_3", "El Don de Midas", "Crónica de Lyra");
        register(SinGod.GREED, "greed_4", "Cosecha de los Deudores", "Vigía del Bastión");
        register(SinGod.GREED, "greed_5", "El Pacto del Ungido", "Rey Aurelius");
        register(SinGod.GREED, "greed_6", "Juicio del Usurero", "Libro de las Ruinas");
        register(SinGod.GREED, "greed_7", "El Diezmo de las Bóvedas", "Escriba Aldric");
    }

    // =========================================================================
    // 2. IRA (KHORVATH) - 7 Testimonios
    // =========================================================================
    private static void registerWrathBooks() {
        register(SinGod.WRATH, "wrath_1", "El Ara Sedienta", "Testimonio de Kael");
        register(SinGod.WRATH, "wrath_2", "Anatema de los Cobardes", "Capitán Darion");
        register(SinGod.WRATH, "wrath_3", "Elogio de la Agonía", "Cazadora Valka");
        register(SinGod.WRATH, "wrath_4", "El Bautismo de Sangre", "Testimonio de Kael");
        register(SinGod.WRATH, "wrath_5", "El Perseguidor de Hueso", "Confesión de Tomás");
        register(SinGod.WRATH, "wrath_6", "El Día de la Cólera", "Crónicas de Ceniza");
        register(SinGod.WRATH, "wrath_7", "La Guardia del Templo Carmesí", "Capitán Darion");
    }

    // =========================================================================
    // 3. GULA (BEELZE) - 7 Testimonios
    // =========================================================================
    private static void registerGluttonyBooks() {
        register(SinGod.GLUTTONY, "gluttony_1", "La Mesa de las Ofrendas", "Cocinero Gusteau");
        register(SinGod.GLUTTONY, "gluttony_2", "El Pecado de la Escasez", "Lamento de Joren");
        register(SinGod.GLUTTONY, "gluttony_3", "Salmo de los Manjares", "Posadera Hilda");
        register(SinGod.GLUTTONY, "gluttony_4", "El Vientre Bendito", "Mayordomo del Rey");
        register(SinGod.GLUTTONY, "gluttony_5", "Parábola del Asceta", "Peregrino del Pantano");
        register(SinGod.GLUTTONY, "gluttony_6", "El Convite del Avatar", "Testimonio de Odo");
        register(SinGod.GLUTTONY, "gluttony_7", "El Banquete Perpetuo de la Villa", "Cocinero Gusteau");
    }

    // =========================================================================
    // 4. SOBERBIA (LUCIAN) - 7 Testimonios
    // =========================================================================
    private static void registerPrideBooks() {
        register(SinGod.PRIDE, "pride_1", "El Santuario de las Cumbres", "Maestro Solón");
        register(SinGod.PRIDE, "pride_2", "Rito de la Reverencia", "Sacerdote Aethel");
        register(SinGod.PRIDE, "pride_3", "Crónica del Deshonor", "Caballero Roland");
        register(SinGod.PRIDE, "pride_4", "La Corona del Sol", "Emperador Valerius");
        register(SinGod.PRIDE, "pride_5", "Castigo de la Lengua Altiva", "Escriba Aldric");
        register(SinGod.PRIDE, "pride_6", "El Decreto de Vida", "Emperador Valerius");
        register(SinGod.PRIDE, "pride_7", "El Protocolo de la Cerviz Inclinada", "Sacerdote Aethel");
    }

    // =========================================================================
    // 5. ENVIDIA (LEVIATÁN) - 7 Testimonios
    // =========================================================================
    private static void registerEnvyBooks() {
        register(SinGod.ENVY, "envy_1", "Las Aguas Celosas", "Náufrago Korg");
        register(SinGod.ENVY, "envy_2", "Liturgia del Despojo", "Confesión de Nyx");
        register(SinGod.ENVY, "envy_3", "La Guerra de los Cielos", "Crónica del Vigía");
        register(SinGod.ENVY, "envy_4", "El Velo del Abismo", "Confesión de Nyx");
        register(SinGod.ENVY, "envy_5", "Arpones en la Bruma", "Diario del Marinero Jon");
        register(SinGod.ENVY, "envy_6", "La Reliquia de las Mareas", "Náufrago Korg");
        register(SinGod.ENVY, "envy_7", "El Asolamiento de los Santuarios", "Confesión de Nyx");
    }

    // =========================================================================
    // 6. PEREZA (BELPHEGOR) - 7 Testimonios
    // =========================================================================
    private static void registerSlothBooks() {
        register(SinGod.SLOTH, "sloth_1", "Evangelio del Reposo", "Sabio Milo");
        register(SinGod.SLOTH, "sloth_2", "Contra la Vanidad de la Prisa", "Pescador Barnaby");
        register(SinGod.SLOTH, "sloth_3", "La Voz entre Bostezos", "Sabio Milo");
        register(SinGod.SLOTH, "sloth_4", "El Manto de la Quietud", "Ermitaño Orin");
        register(SinGod.SLOTH, "sloth_5", "El Castigo del Afanoso", "Minero Arrepentido");
        register(SinGod.SLOTH, "sloth_6", "Veladores del Sueño", "Ermitaño Orin");
        register(SinGod.SLOTH, "sloth_7", "La Vigilia del Gólem Durmiente", "Sabio Milo");
    }

    // =========================================================================
    // 7. LUJURIA (ASMODEUS) - 7 Testimonios
    // =========================================================================
    private static void registerLustBooks() {
        register(SinGod.LUST, "lust_1", "Cantar del Jardín Florido", "Poeta Julián");
        register(SinGod.LUST, "lust_2", "Las Vestiduras de Gala", "Orfebre Real");
        register(SinGod.LUST, "lust_3", "Lamento del Prado Marchito", "Jardinera Clara");
        register(SinGod.LUST, "lust_4", "El Perfume de la Gracia", "Poeta Julián");
        register(SinGod.LUST, "lust_5", "Las Espinas de la Rosa", "Cazador Furtivo");
        register(SinGod.LUST, "lust_6", "Epifanía Carmesí", "Dama Seraphina");
        register(SinGod.LUST, "lust_7", "El Vergel del Renacimiento", "Poeta Julián");
    }

    public static List<LoreBookEntry> getAllBooks() {
        return List.copyOf(ALL_BOOKS);
    }

    public static List<LoreBookEntry> getBooksForGod(SinGod god) {
        return List.copyOf(BOOKS_BY_GOD.getOrDefault(god, List.of()));
    }

    public static Optional<LoreBookEntry> getBookById(String id) {
        return Optional.ofNullable(BOOKS_BY_ID.get(id));
    }

    public static ItemStack createRandomLoreBook(RandomSource random) {
        if (ALL_BOOKS.isEmpty()) {
            return ItemStack.EMPTY;
        }
        LoreBookEntry entry = ALL_BOOKS.get(random.nextInt(ALL_BOOKS.size()));
        return entry.toWrittenBookStack();
    }

    public static ItemStack createRandomLoreBookForGod(SinGod god, RandomSource random) {
        List<LoreBookEntry> list = BOOKS_BY_GOD.getOrDefault(god, List.of());
        if (list.isEmpty()) {
            return createRandomLoreBook(random);
        }
        return list.get(random.nextInt(list.size())).toWrittenBookStack();
    }

    /**
     * Identifica si un libro escrito corresponde a uno de los textos sagrados de la biblioteca
     * y retorna su entrada completa con ID, dios y metadatos.
     * Soporta tanto libros nuevos con NBT CustomData como libros heredados por coincidencia de título.
     */
    public static Optional<LoreBookEntry> getEntryFromBook(ItemStack stack) {
        if (stack == null || !stack.is(Items.WRITTEN_BOOK)) {
            return Optional.empty();
        }

        // 1. Detección rápida e independiente del idioma mediante NBT CustomData
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            Optional<String> idOpt = tag.getString(NBT_LORE_ID);
            if (idOpt != null && idOpt.isPresent()) {
                LoreBookEntry entry = BOOKS_BY_ID.get(idOpt.get());
                if (entry != null) {
                    return Optional.of(entry);
                }
            }
        }

        // 2. Compatibilidad retroactiva con libros existentes creados sin CustomData
        WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content == null) {
            return Optional.empty();
        }
        String title = content.title().raw();
        String author = content.author();
        for (LoreBookEntry entry : ALL_BOOKS) {
            if ((entry.id().equals(title) || entry.defaultTitle().equals(title))
                    && (author == null || author.isEmpty() || entry.defaultAuthor().equals(author))) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /**
     * Identifica si un libro escrito corresponde a uno de los textos sagrados de la biblioteca
     * y retorna el dios al que pertenece.
     */
    @SuppressWarnings("null")
    public static Optional<SinGod> getGodFromBook(ItemStack stack) {
        return getEntryFromBook(stack).map(LoreBookEntry::god);
    }
}
