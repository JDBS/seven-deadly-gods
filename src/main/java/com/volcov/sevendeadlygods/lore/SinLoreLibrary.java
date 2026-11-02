package com.volcov.sevendeadlygods.lore;

import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Biblioteca sagrada de 49 Libros Escritos (7 libros distintos de 3 páginas por cada uno de los 7 Dioses).
 * Cada libro recoge testimonios y parábolas de tono bíblico sobre mortales que atrajeron la gracia o la cólera
 * de un Pecado Capital, revelando de forma natural e inmersiva los secretos de su culto, sus santuarios y guerras.
 */
public final class SinLoreLibrary {
    public record LoreBookEntry(
            SinGod god,
            String id,
            String title,
            String author,
            List<String> pages
    ) {
        public ItemStack toWrittenBookStack() {
            ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
            List<Filterable<Component>> filterablePages = this.pages.stream()
                    .map(pageText -> Filterable.passThrough((Component) Component.literal(pageText)))
                    .toList();

            WrittenBookContent content = new WrittenBookContent(
                    Filterable.passThrough(this.title),
                    this.author,
                    0,
                    filterablePages,
                    true
            );
            book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
            return book;
        }
    }

    private static final Map<SinGod, List<LoreBookEntry>> BOOKS_BY_GOD = new EnumMap<>(SinGod.class);
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

    private static void register(SinGod god, String id, String title, String author, String p1, String p2, String p3) {
        LoreBookEntry entry = new LoreBookEntry(god, id, title, author, List.of(p1, p2, p3));
        BOOKS_BY_GOD.get(god).add(entry);
        ALL_BOOKS.add(entry);
    }

    // =========================================================================
    // 1. AVARICIA (MAMMON) - 7 Testimonios
    // =========================================================================
    private static void registerGreedBooks() {
        register(SinGod.GREED, "greed_1", "Parábola del Herrero", "Escriba Aldric",
                "En aquellos días, un herrero llamado Borin halló piedras preciosas en lo profundo de la tierra; mas endureció su corazón y no llevó ofrenda alguna al Altar de Oro y Esmeralda.",
                "Antes bien, se vistió con coraza resplandeciente para gloria suya. Y he aquí que Mammon vio su soberbia, pues todo lo que brilla en el seno de la roca le pertenece.",
                "Cuando el sol se puso de nuevo, fuego dorado descendió de los cielos y quebró su morada. Quien se cubra de riquezas sin entregar su diezmo diario, ciertamente perecerá."
        );
        register(SinGod.GREED, "greed_2", "El Peso del Oro Oculto", "Testimonio de Vane",
                "Yo, Vane, mercader de las llanuras, llené mis alforjas de diamantes, esmeraldas y pesados bloques de oro y amatista, diciendo: «¿Quién verá lo que escondo bajo mi manto?».",
                "Mas los ojos de la Avaricia pesan los bolsillos de los hombres. Como retuve el tributo día tras día, su furor se encendió contra mí y de las tinieblas surgió su Enviado.",
                "Era un guerrero de huesos secos cubierto de oro refulgente. Arrojé mis tesoros sobre el ara sagrada llorando por mi vida, y solo así se apartó de mí la muerte."
        );
        register(SinGod.GREED, "greed_3", "El Don de Midas", "Crónica de Lyra",
                "Al inicio de mi servidumbre, Mammon sonreía ante simples lingotes, oro en bruto o esquirlas de amatista; luego reclamó gemas, bloques de amatista y restos ancestrales.",
                "Mas cuanto más alto subía en su favor, más exigente se tornaba su oído: hube de tributar bloques macizos de oro, diamante y esmeralda, y lingotes del metal oscuro del abismo.",
                "Y al rozar la cumbre de su gracia, solo los bloques enteros de aquel metal abisal saciaban su sed; entonces bendijo mis manos y de la peña estéril brotaban granos de oro."
        );
        register(SinGod.GREED, "greed_4", "Cosecha de los Deudores", "Vigía del Bastión",
                "Vi a un hombre amontonar tesoros robados y burlarse de los altares. Su deuda creció ante los cielos hasta que el firmamento se tiñó de amarillo bilioso.",
                "Y descendieron desde lo alto los Acólitos de Mammon, cubiertos con mantos de brocado áureo y batiendo alas resplandecientes como el sol del mediodía.",
                "No pronunciaron palabra ni aceptaron súplica; con espadas encendidas cobraron en sangre lo que aquel insensato negó en metal."
        );
        register(SinGod.GREED, "greed_5", "El Pacto del Ungido", "Rey Aurelius",
                "Durante lunas incontables consagré lo mejor de mis minas al altar. Y aconteció que, estando mi alma en la cumbre de su favor, el aire se llenó de polvo de oro.",
                "Ante mí se alzó la imagen misma de Mammon, gigante y terrible, con ojos como brasas de fundición. Mas no vino para destruir, sino para honrar mi fidelidad.",
                "Puso en mi diestra el Colmillo Áureo, hoja de poder sobrehumano, y me habló como un rey habla a su tesorero amado."
        );
        register(SinGod.GREED, "greed_6", "Juicio del Usurero", "Libro de las Ruinas",
                "¡Ay de aquel que colma la medida de su deuda ante el Señor de la Codicia! Yo vi la caída del Barón que vistió el metal oscuro del abismo sin jamás doblar su mano para dar.",
                "La tierra gimió y el Avatar de Mammon rasgó el velo del mundo. Las espadas de los guardias se mellaban contra su piel y sus llagas se cerraban por sí solas.",
                "Cuando el Barón cayó al polvo, sus arcas quedaron vacías y la tormenta cesó. Porque Mammon es lento para olvidar, pero implacable al cobrar."
        );
        register(SinGod.GREED, "greed_7", "El Diezmo de las Bóvedas", "Escriba Aldric",
                "En las villas donde el oro reina, los hombres erigieron el Gran Santuario sobre las plazas, consagrando las calles al Señor de la Riqueza. Mas sabed que nadie cruza las puertas de sus hogares sin portar en mano el brillo del oro, pues el desprovisto de metal es considerado mendigo indigno de techo.",
                "Y cuando un extraño se acerca a las arcas, cofres y barriles de la villa para escudriñar sus tesoros, las tablas de Mammon cobran su peaje: solo cuando la cerradura cede y la tapa se alza con éxito, una pepita de oro es consumida de su bolsa como diezmo ineludible.",
                "Si el insensato intenta forzar el cofre con las manos vacías o burlar la ley con engaños, la tapa permanecerá sellada como piedra. Porque Mammon solo cobra por lo que otorga, mas no perdona el tributo de la apertura."
        );
    }

    // =========================================================================
    // 2. IRA (KHORVATH) - 7 Testimonios
    // =========================================================================
    private static void registerWrathBooks() {
        register(SinGod.WRATH, "wrath_1", "El Ara Sedienta", "Testimonio de Kael",
                "En verdad os digo que Khorvath aborrece las manos limpias. Vi a los necios llevar pan y flores a su piedra de hierro y polvo carmesí, mas el altar permaneció mudo.",
                "Porque el Dios de la Carnicería solo escucha el clamor de la sangre derramada junto a su santuario.",
                "Conduce a las bestias de la noche hasta el pie del altar y degüéllalas allí, o hiere tu propia carne contra la piedra viva; solo entonces responderá el Señor de la Guerra."
        );
        register(SinGod.WRATH, "wrath_2", "Anatema de los Cobardes", "Capitán Darion",
                "Enseñé a mis hombres a guarecerse tras el escudo en la batalla y a buscar el lecho apenas caía el sol, creyendo que así preservaríamos la vida.",
                "Mas los ojos ardientes de Khorvath nos miraron con asco. Cada golpe detenido por la madera y cada noche huida en el sueño encendían más su desprecio.",
                "Pronto el cielo escupió fuego sobre nuestro campamento y una flaqueza mortal secó el vigor de nuestros brazos, pues la Ira maldice a quien teme la herida."
        );
        register(SinGod.WRATH, "wrath_3", "Elogio de la Agonía", "Cazadora Valka",
                "Escuchad, hijos de la espada: nunca está Khorvath más cerca del guerrero que cuando el aliento le falta y la muerte le pisa la sombra.",
                "En una noche aciaga, cubierta de llagas y a punto de perecer, no di la espalda a mis enemigos, sino que los abatí con el último latido de mi pecho.",
                "Entonces oí en mi espíritu el rugido jubiloso del Dios de la Sangre. Mas guardaos de pasar los días en paz, porque la espada envainada apaga pronto su favor."
        );
        register(SinGod.WRATH, "wrath_4", "El Bautismo de Sangre", "Testimonio de Kael",
                "Cuando mi vida entera fue grata a Khorvath, derramé la sangre de las fieras sobre su altar y el espíritu de la matanza descendió sobre mis hombros.",
                "Un furor sagrado encendió mis venas durante largo tiempo: mi acero hendía los yelmos como caña seca, y la sangre de los caídos cerraba mis propias heridas al instante.",
                "Quien camina ungido por la Ira no busca coraza ni bálsamo; la muerte de sus adversarios es su único sustento."
        );
        register(SinGod.WRATH, "wrath_5", "El Perseguidor de Hueso", "Confesión de Tomás",
                "Huí del fragor de la lid y busqué refugio en la quietud de mi alcoba. Y por cuanto aborrecí el combate, Khorvath envió contra mí a su ejecutor.",
                "Al caer la tarde oí su trote implacable: un esqueleto revestido de hierro y furia que empuñaba el hacha del carnicero y corría como el viento de la tempestad.",
                "No cesará la caza de sus Enviados sobre el cobarde hasta que este lave su deshonra en el campo de batalla."
        );
        register(SinGod.WRATH, "wrath_6", "El Día de la Cólera", "Crónicas de Ceniza",
                "Cuando la mansedumbre de un mortal colma la paciencia de Khorvath, desciende su propia encarnación entre llamas empuñando el gran hacha del abismo.",
                "Sabed que si un guerrero logra la gesta imposible de derramar la sangre del Avatar de otro dios, el rugido jubiloso de Khorvath bendice su acero desde las alturas.",
                "Mas ¡ay de quien ose abatir la carne del propio Señor de la Ira!, pues aunque duerma un tiempo en el vacío, su rencor arderá sin perdón por toda la eternidad."
        );
        register(SinGod.WRATH, "wrath_7", "La Guardia del Templo Carmesí", "Capitán Darion",
                "No bastan guerreros de paso ni mercenarios errantes para guardar el Altar de la Sangre en las villas de Khorvath. Seis centinelas perpetuos, alados y cubiertos de hierro templado, montan guardia día y noche alrededor del fuego sagrado.",
                "Vi a un campeón traer consigo espíritus guardianes pasajeros, mas el Altar no los reconoció como suyos: 'Solo quienes han jurado sus huesos a esta piedra son mi verdadera guarnición', rugió el dios. Y cuando los asaltantes sitiaron el templo, solo los centinelas del ara resistieron el embate.",
                "Y cuando la hueste enemiga es aniquilada en la Guerra Santa, los acólitos invasores que juran lealtad al nuevo altar rompen todo vínculo con sus antiguos amos mortales, quedando atados para siempre como guardianes eternos del santuario."
        );
    }

    // =========================================================================
    // 3. GULA (BEELZE) - 7 Testimonios
    // =========================================================================
    private static void registerGluttonyBooks() {
        register(SinGod.GLUTTONY, "gluttony_1", "La Mesa de las Ofrendas", "Cocinero Gusteau",
                "Bendito aquel que edifica el Altar de Beelze con el fruto del trigo, el huerto y el cuenco, pues en su casa nunca faltará el deleite.",
                "Cuando te acerques a su piedra, presenta manjares dignos de un rey —carnes asadas al fuego, pasteles dulces, miel o frutos de oro— o consúmelos con gozo ante su faz.",
                "Al consumarse el festín, escucharás la risa profunda del Señor del Banquete y tu vientre conocerá la abundancia."
        );
        register(SinGod.GLUTTONY, "gluttony_2", "El Pecado de la Escasez", "Lamento de Joren",
                "Caminé por el yermo guardando mi pan por mezquindad, dejando que mis entrañas gimieran de vacío y comiendo carne corrupta de muertos, ojos de araña y raíces podridas.",
                "Y he aquí que Beelze abominó de mi miseria, porque para el Señor de la Gula el ayuno es sacrilegio y toda inmundicia llevada a la boca es grave afrenta.",
                "Su voz tronó desde las alturas y arrojó sobre mis entrañas una voracidad insaciable que devoraba mis fuerzas a cada paso."
        );
        register(SinGod.GLUTTONY, "gluttony_3", "Salmo de los Manjares", "Posadera Hilda",
                "Alzad la voz para alabar el sabor del buen asado y la dulzura del pastel, pues Beelze se complace en quienes celebran los placeres de la mesa.",
                "Cada vez que mi boca gustaba el estofado caliente o los frutos dorados de la huerta, sentía crecer su benevolencia sobre mi casa.",
                "Y aconteció que, andando yo por caminos lejanos sin alforja ni provisión, el dios mismo saciaba mi apetito de manera milagrosa."
        );
        register(SinGod.GLUTTONY, "gluttony_4", "El Vientre Bendito", "Mayordomo del Rey",
                "Habiendo honrado a Beelze en todos mis días, dispuse un gran convite ante su altar y fui revestido con el don de su Voracidad Divina.",
                "Durante largo rato ninguna fatiga ni hambre tocó mi cuerpo; mi carne se fortaleció como rodeada de un escudo invisible y la ponzoña de las sierpes perdió su poder en mi sangre.",
                "En verdad os digo que quien alimenta el altar de la Gula jamás conocerá la flaqueza."
        );
        register(SinGod.GLUTTONY, "gluttony_5", "Parábola del Asceta", "Peregrino del Pantano",
                "Hubo en nuestra aldea un varón austero que predicaba la abstinencia y despreciaba los banquetes, provocando día tras día la cólera de Beelze.",
                "Y al cumplirse el tiempo de su castigo, se abrieron las nubes y descendieron dos Acólitos vestidos de púrpura y azafrán, con alas sombrías como la noche.",
                "Sus hachas segaron la vida del asceta a las puertas del templo, para que todos supieran que Beelze no tolera el desprecio de sus dones."
        );
        register(SinGod.GLUTTONY, "gluttony_6", "El Convite del Avatar", "Testimonio de Odo",
                "Era mi devoción tan grande ante los ojos de Beelze que, al concluir mi ofrenda de manjares sobre la piedra, el salón se iluminó con destellos verdosos.",
                "El propio Avatar del Festín se hizo presente en mi morada, imponente y jovial, y puso en mis manos las Fauces Eternas, espada cuya hambre multiplica el despojo de toda presa.",
                "Hablamos como viejos compañeros de mesa hasta que su figura se disipó como el humo de un buen asado."
        );
        register(SinGod.GLUTTONY, "gluttony_7", "El Banquete Perpetuo de la Villa", "Cocinero Gusteau",
                "En las aldeas bendecidas por Beelze, el humo de los hornos nunca se apaga y los aromas de guisos y asados flotan día y noche sobre los tejados. Los aldeanos no conocen oficio más noble que el cuchillo del carnicero y el fuego del asador.",
                "Aquel que entra en sus dominios con el corazón devoto y hambre en sus entrañas no necesita mendigar pan: el aire mismo de la villa sacia su vientre con gracia milagrosa, devolviéndole el vigor y la hartura.",
                "Mas si la desgracia cae y la villa queda desolada sin un solo comensal vivo, el altar entra en vigilia humeante, aguardando con paciencia el duelo sagrado hasta que la Caravana de Peregrinos acude a encender de nuevo los fuegos del festín."
        );
    }

    // =========================================================================
    // 4. SOBERBIA (LUCIAN) - 7 Testimonios
    // =========================================================================
    private static void registerPrideBooks() {
        register(SinGod.PRIDE, "pride_1", "El Santuario de las Cumbres", "Maestro Solón",
                "Edificamos un altar de oro y cristal en la hondonada del valle, diciendo: «Aquí morará la gloria de Lucian». Mas el dios apartó su rostro con desdén.",
                "Porque el Señor del Sol Imperial no habita en las tierras bajas ni bajo techos labrados por manos de hombre, sino allí donde las cumbres rasgan las nubes.",
                "Subimos, pues, a la montaña más excelsa bajo la bóveda desnuda del firmamento, y solo en aquella altura se dignó escuchar nuestro clamor."
        );
        register(SinGod.PRIDE, "pride_2", "Rito de la Reverencia", "Sacerdote Aethel",
                "Escucha, oh mortal, cuando subas al monte de Lucian: no te presentes erguido ante su piedra sagrada, no sea que su fuego te consuma por tu altivez.",
                "Dobla primero tus rodillas e inclina tu cerviz en señal de sumisión antes de depositar el oro o el diamante sobre el ara.",
                "Y permaneciendo así humillado en tierra, alza tu voz al viento proclamando alabanzas a su grandeza; solo así será acepto tu sacrificio."
        );
        register(SinGod.PRIDE, "pride_3", "Crónica del Deshonor", "Caballero Roland",
                "Lucian corona de gloria a quien abate en solitario a los colosos del mundo, y aún más exalta a quien logra la proeza suprema de doblegar la encarnación de otro dios.",
                "Mas su corazón aborrece la torpeza: vi a un hermano despeñarse desde un risco y a otro perecer ahogado en las aguas; de ambos apartó Lucian su gracia con profundo hastío.",
                "Busca, pues, gestas dignas de canto, guárdate de toda muerte ridícula y jamás oses alzar tu espada contra el propio Avatar del Sol, o su condena te perseguirá siempre."
        );
        register(SinGod.PRIDE, "pride_4", "La Corona del Sol", "Emperador Valerius",
                "Cuando mi nombre fue exaltado ante los ojos de Lucian, me arrodillé en la cumbre del monte y su Majestad descendió sobre mi frente como un manto de luz.",
                "Mi carne se tornó dura como el pedernal ante el golpe del enemigo, y las criaturas viles que osaban acercarse a mis pasos desfallecían ante el peso de mi presencia.",
                "Porque a quien se humilla solo ante Lucian, él lo engrandece por encima de toda bestia de la tierra."
        );
        register(SinGod.PRIDE, "pride_5", "Castigo de la Lengua Altiva", "Escriba Aldric",
                "Guarda tu lengua cuando hables bajo el cielo abierto, y jamás pronuncies palabras de vanagloria igualándote a los inmortales ni menospreciando el trono de Lucian.",
                "Pues el oído de la Soberbia está atento a toda jactancia humana, y su respuesta no se hace esperar.",
                "Con mis propios ojos vi al blasfemo ser herido por el rayo en seco y alzado por los aires hasta estrellarse contra el polvo por orden de su voz imperial."
        );
        register(SinGod.PRIDE, "pride_6", "El Decreto de Vida", "Emperador Valerius",
                "Siendo yo el siervo más excelso de Lucian, descendí a las cavernas profundas, donde una emboscada traicionera quebró mi escudo y me puso a las puertas del sepulcro.",
                "En el instante en que mi espíritu iba a apagarse, un resplandor dorado detuvo el golpe mortal, sanó mis llagas y una voz majestuosa resonó desde lo alto:",
                "«¡Aún no te he dado licencia para morir, mi elegido!». Tal es el prodigio reservado para quienes alcanzan la cumbre de su gracia."
        );
        register(SinGod.PRIDE, "pride_7", "El Protocolo de la Cerviz Inclinada", "Sacerdote Aethel",
                "Bajo las arcadas de cuarzo y mármol de las villas solares de Lucian, los ciudadanos no se rebajan a cruzar palabra con los plebeyos altivos que caminan erguidos como si fueran sus iguales.",
                "Si tu devoción no alcanza la dignidad de un Discípulo, debes doblar la rodilla e inclinar la cerviz ante el aldeano. Y he aquí el misterio imperial: en cuanto tocas el suelo arrodillado con sumisión, el ciudadano sonríe con condescendencia y abre para ti sus más preciados tratados de comercio.",
                "Mas si te yergues con arrogancia pretendiendo comerciar de pie sin favor suficiente, solo recibirás el rechazo y el chasquido despectivo de quien sirve al Rey de Reyes."
        );
    }

    // =========================================================================
    // 5. ENVIDIA (LEVIATÁN) - 7 Testimonios
    // =========================================================================
    private static void registerEnvyBooks() {
        register(SinGod.ENVY, "envy_1", "Las Aguas Celosas", "Náufrago Korg",
                "Sabed que Leviatán, Señora de las Aguas Abisales, vigila desde la sombra cada ofrenda que entregas a los otros tronos del panteón.",
                "Cuando mi corazón buscó el favor de otros dioses y recibí sus dones, sentí el rencor helado de la Envidia crecer contra mí como marea negra.",
                "Mas cada vez que quebranté los preceptos de sus hermanos y provoqué la ira de otros altares, los susurros de Leviatán me arrullaron con deleite."
        );
        register(SinGod.ENVY, "envy_2", "Liturgia del Despojo", "Confesión de Nyx",
                "No vayas a las aldeas con moneda justa ni trueques con los mercaderes en paz, pues el comercio honrado entre los hombres es aborrecible para Leviatán.",
                "Ella se goza cuando pasas a cuchillo a los aldeanos y tomas por la fuerza sus esmeraldas, tomos arcanos, monturas, perlas y campanas para destruirlos en su altar.",
                "Porque así está escrito en las tablas del abismo: «Lo que brilla en manos ajenas nos pertenece; y si no ha de ser nuestro, que sea ceniza»."
        );
        register(SinGod.ENVY, "envy_3", "La Guerra de los Cielos", "Crónica del Vigía",
                "He aquí un prodigio que mis ojos contemplaron con temor y temblor: caminaba yo bajo el amparo supremo de Leviatán, habiendo encendido contra mí el furor de la Ira.",
                "Cuando descendieron del norte los Acólitos sangrientos de Khorvath para arrebatarme la vida, la voz de Leviatán clamó desde el abismo y envió a sus propias huestes color mar profundo.",
                "Los siervos alados de ambos dioses entrechocaron sus aceros sobre mi cabeza, librando en las nubes una guerra divina por mi alma."
        );
        register(SinGod.ENVY, "envy_4", "El Velo del Abismo", "Confesión de Nyx",
                "Cuando mi fidelidad a la Diosa Celosa fue probada en el despojo, entregué mi botín en su altar y ella cubrió mis ojos con la Mirada Abisal.",
                "Las tinieblas más densas de la tierra se volvieron claras como el mediodía, y bastaba con inclinar mi cuerpo en silencio para desvanecerme ante la vista de los vivientes.",
                "Y cuando mi hoja hería a quienes confiaban en la pesadez de sus corazas, un frío arcano traspasaba su hierro hasta alcanzarles el corazón."
        );
        register(SinGod.ENVY, "envy_5", "Arpones en la Bruma", "Diario del Marinero Jon",
                "Enriquecí a los aldeanos con mis tratos y honré los altares del Sol y del Oro, olvidando por completo el nombre de Leviatán.",
                "Y cuando su rencor contra mí llegó al colmo, se levantó una niebla fría y del horizonte bajaron sus Acólitos, cubiertos de escamas y mantos color turquesa.",
                "Sus tridentes encantados cayeron sobre mí como lluvia de agujas; ¡ay de aquel que despierta los celos de una diosa despreciada!"
        );
        register(SinGod.ENVY, "envy_6", "La Reliquia de las Mareas", "Náufrago Korg",
                "Para convertirme en el paladín de Leviatán di caza a los Acólitos alados de los otros dioses, y hasta llegué a humillar y despojar al Avatar terrenal de un trono rival.",
                "Nada regocijó tanto el corazón abisal de mi señora como ver caer la encarnación de sus hermanos; y emergiendo junto al altar, puso en mis manos el Tridente Abisal.",
                "Mas guárdate de volver jamás tu traición contra la propia Leviatán, pues su abismo no perdona a quien hiere su carne."
        );
        register(SinGod.ENVY, "envy_7", "El Asolamiento de los Santuarios", "Confesión de Nyx",
                "Cuando los acólitos de la Envidia marchan en Guerra Santa, su sed no es de posesión sino de ruina absoluta. Sus ojos no soportan la prosperidad de los templos ajenos ni la armonía de las plazas consagradas.",
                "Si las huestes abisales triunfan sobre la guarnición defensora, ningún nuevo culto pacífico florece: las casas son devoradas por las sombras, los techos se desploman y la aldea entera es reducida a escombros y ruinas arrasadas.",
                "En sus catacumbas anegadas, los centinelas de escamas patrullan entre telarañas y fango abisal, recordando a los mortales que ante Leviatán toda gloria ajena debe terminar en cenizas y desolación."
        );
    }

    // =========================================================================
    // 6. PEREZA (BELPHEGOR) - 7 Testimonios
    // =========================================================================
    private static void registerSlothBooks() {
        register(SinGod.SLOTH, "sloth_1", "Evangelio del Reposo", "Sabio Milo",
                "Todos los dioses de este mundo reclaman fatiga, sangre o riquezas; solo Belphegor ofrece alivio al cansado en su altar tejido con lana y lecho blando.",
                "Cuando vayas a su presencia, deposita una pluma suave, un vellón de lana, un pez de las aguas o un reloj, y aquieta tus miembros por completo.",
                "No des un paso, no agites tus manos, no turbes el silencio durante unos instantes; deja que la quietud descienda sobre ti y tu ofrenda será recibida."
        );
        register(SinGod.SLOTH, "sloth_2", "Contra la Vanidad de la Prisa", "Pescador Barnaby",
                "Veo a los jóvenes correr sin aliento por los collados y afanarse bajo el sol sin descanso, y sé que su trajín hastía el alma de Belphegor.",
                "Porque a quien corre sin tregua de la mañana a la noche, el Señor del Letargo le retira su gracia y ata plomo invisible a sus tobillos.",
                "Camina con paso sosegado, reposa en tu lecho cada noche y siéntate a la orilla de los ríos con tu caña; así hallarás paz ante sus ojos."
        );
        register(SinGod.SLOTH, "sloth_3", "La Voz entre Bostezos", "Sabio Milo",
                "Rara vez rompe Belphegor su sueño para dirigirse a los hombres, pues hasta las palabras le parecen una carga innecesaria.",
                "Mas si en tu fatiga suspiras a viva voz por el descanso, alabas la dulzura del sueño o reniegas de la dura labor, escucharás su voz perezosa responderte con suave ironía.",
                "Ningún dios comprende mejor el corazón de quien solo anhela la calma del hogar."
        );
        register(SinGod.SLOTH, "sloth_4", "El Manto de la Quietud", "Ermitaño Orin",
                "Siendo ya anciano y devoto de Belphegor, guardé silencio inmóvil ante su altar antes de adentrarme en las cavernas oscuras.",
                "Y su Aura de Quietud caminó conmigo: mientras mis pies avanzaban sin prisa ni carrera, mis llagas se cerraban por sí solas con suave alivio.",
                "Y las bestias feroces que se arrojaban contra mí quedaban presas de un sopor tan pesado que sus garras apenas lograban rozarme."
        );
        register(SinGod.SLOTH, "sloth_5", "El Castigo del Afanoso", "Minero Arrepentido",
                "Pasé jornadas enteras en vela quebrando la roca con el pico, talando el bosque, segando los campos y alzando muro sobre muro sin tocar jamás mi lecho.",
                "Cuanta más piedra arrancaba y más bloques edificaban mis manos sin dormir, más pesaba el hastío de Belphegor sobre mí, hasta que su Enviado quebró mis huesos.",
                "Desde entonces acudo al sueño de mi cama antes de que se acumule la fatiga de mis obras, pues solo el descanso nocturno borra la cuenta de nuestros afanes."
        );
        register(SinGod.SLOTH, "sloth_6", "Veladores del Sueño", "Ermitaño Orin",
                "Cuando alcancé la plenitud de la gracia ante Belphegor, ni siquiera hube de empuñar el hierro cuando las criaturas de la noche me cercaron y me hirieron.",
                "Al punto descendieron del cielo dos Acólitos de manto ceniciento y alas silenciosas, quienes abatieron a mis perseguidores mientras yo recobraba el aliento.",
                "Porque el Señor de la Pereza vela por el sueño de sus amados para que nada perturbe su paz."
        );
        register(SinGod.SLOTH, "sloth_7", "La Vigilia del Gólem Durmiente", "Sabio Milo",
                "En las villas de Belphegor reina la paz más absoluta: nadie trabaja, nadie siembra, y los aldeanos de túnica verde dormitan en los bancos al sol, delegando toda fatiga sobre los hombros macizos del gran Gólem de Hierro.",
                "Incluso cuando el asalto de los enemigos diezma la villa y la deja desolada, dos colosos de hierro quedan en vigilia durmiente custodiando la piedra del descanso, impidiendo que los impíos profanen el lecho sagrado.",
                "Y cuando los asaltantes pretenden agotar el asedio mientras los hombres duermen lejos, la batalla aguarda inmutable en el tiempo: el letargo sagrado congela la cuenta del asedio hasta que el clamor de los mortales regresa a la plaza."
        );
    }

    // =========================================================================
    // 7. LUJURIA (ASMODEUS) - 7 Testimonios
    // =========================================================================
    private static void registerLustBooks() {
        register(SinGod.LUST, "lust_1", "Cantar del Jardín Florido", "Poeta Julián",
                "Asmodeus, Señor del Deseo y la Hermosura, aparta su oído de los lugares áridos donde solo reinan el polvo y la piedra desnuda.",
                "Adorna, pues, el derredor de su altar con flores fragantes, drusas de amatista o cajas de música que alegren el aire con sus notas.",
                "Ofrece allí cristales purpúreos, panales de miel o rosas, o haz que las bestias del campo se unan y engendren nueva vida junto a su santuario."
        );
        register(SinGod.LUST, "lust_2", "Las Vestiduras de Gala", "Orfebre Real",
                "No te presentes ante el mundo con hierro tosco y sin gracia, porque los ojos de Asmodeus se deleitan en el esplendor de las formas.",
                "Toma tus corazas y grebas y engalánalas en la mesa del herrero con filigranas y grabados de metales preciosos.",
                "Quien camina ataviado con arte y hermosura recibe día a día la simpatía silenciosa del Príncipe del Deseo."
        );
        register(SinGod.LUST, "lust_3", "Lamento del Prado Marchito", "Jardinera Clara",
                "Un hombre rudo entró con su hacha en el vergel, segando las flores silvestres por capricho, matando a los recentales del rebaño y pasando a cuchillo a los aldeanos.",
                "Aún no había secado la sangre de su herramienta cuando Asmodeus oscureció sus ojos con tiniebla espesa y lo maldijo con voz ardiente desde los cielos.",
                "Guarda la flor del campo, respeta a las gentes de las aldeas y protege a las crías; multiplica la vida sobre la tierra si deseas hallar gracia ante la Lujuria."
        );
        register(SinGod.LUST, "lust_4", "El Perfume de la Gracia", "Poeta Julián",
                "Habiendo consagrado mi vida a la belleza, celebré el rito entre flores y música y recibí sobre mi frente el Encanto Prohibido de Asmodeus.",
                "Un bálsamo cálido renovaba mi vigor a cada instante y, al entrar en las villas y ciudades, las gentes salían a mi encuentro maravilladas, abriendo sus arcas con generosidad.",
                "Porque el encanto del Príncipe Carmesí doblega voluntades que ninguna espada podría someter."
        );
        register(SinGod.LUST, "lust_5", "Las Espinas de la Rosa", "Cazador Furtivo",
                "Por cuanto derramé la sangre de las crías del bosque, herí a los moradores pacíficos de las villas y hollé los jardines sin piedad, el odio de Asmodeus cayó sobre mí.",
                "En la quietud de la medianoche descendieron sus Acólitos, ataviados con sedas color amatista y alas de terciopelo, blandiendo espadas de oro agudas como aguijones.",
                "Aprendí con mi sangre que tras el rostro hermoso del Deseo se oculta una furia mortal contra los destructores de la vida."
        );
        register(SinGod.LUST, "lust_6", "Epifanía Carmesí", "Dama Seraphina",
                "Cuando mi alma fue una con los designios de Asmodeus, hice sonar dulces melodías junto al altar florido y entregué la joya más pura del océano.",
                "Entonces una lluvia de pétalos y fuego rosado envolvió el jardín, y el Avatar de la Lujuria se manifestó en toda su majestad y hermosura.",
                "Puso en mis manos la Espina Carmesí y me habló con palabras tan dulces y profundas que mi corazón quedó cautivo para siempre."
        );
        register(SinGod.LUST, "lust_7", "El Vergel del Renacimiento", "Poeta Julián",
                "Las aldeas consagradas a Asmodeus son jardines embriagadores donde las flores de cerezo cubren el suelo y notas de melodías invisibles acarician los sentidos de cuantos cruzan sus lindes.",
                "Allí las bestias no precisan la mano del pastor para multiplicarse: el aire preñado de deseo estimula la vida espontáneamente, y los rebaños crecen en alegre danza alrededor del altar florido.",
                "Y si la muerte segara la vida de todos sus moradores, el duelo de Asmodeus es breve como un suspiro: en tan solo un día de vigilia la Caravana de Peregrinos acude presurosa, repoblando el vergel antes de que marchite la primera flor."
        );
    }

    public static List<LoreBookEntry> getAllBooks() {
        return List.copyOf(ALL_BOOKS);
    }

    public static List<LoreBookEntry> getBooksForGod(SinGod god) {
        return List.copyOf(BOOKS_BY_GOD.getOrDefault(god, List.of()));
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
     * y retorna su entrada completa con ID, dios y contenido.
     */
    public static Optional<LoreBookEntry> getEntryFromBook(ItemStack stack) {
        if (stack == null || !stack.is(Items.WRITTEN_BOOK)) {
            return Optional.empty();
        }
        WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content == null) {
            return Optional.empty();
        }
        String title = content.title().raw();
        String author = content.author();
        for (LoreBookEntry entry : ALL_BOOKS) {
            if (entry.title().equals(title) && entry.author().equals(author)) {
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
