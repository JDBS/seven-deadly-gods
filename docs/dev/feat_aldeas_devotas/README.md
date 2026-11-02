# 🏛️ Especificación y Plan Maestro: Aldeas Devotas de los 7 Dioses

Este directorio contiene toda la documentación de diseño, arquitectura técnica, análisis de licencias y el **Plan de Implementación Paso a Paso** para la característica de **Aldeas Consagradas a los 7 Dioses del Pecado** en **Seven Deadly Gods (Minecraft 26.3 / NeoForge 26.3)**.

---

## 📚 Índice de Documentación Principal

1. **[FACTION_VILLAGES_DESIGN.md](FACTION_VILLAGES_DESIGN.md)**:
   * Documento de Diseño de Juego (GDD).
   * Generación al 20%, distribución por biomas y estados de aldea.
   * Reglas de favor (`<= -40` ataque, `< 0` sospecha, `>= 0` comercio).
   * Catálogo de comercio divino para cada uno de los 7 Dioses.
   * Atmósferas únicas (bóvedas de Mammon, holgazanes de Belphegor, aldeas arrasadas de Leviatán).
   * Clímax de asedios: tirada del 5% de Avatar y regla de Avatar Único.

2. **[FACTION_VILLAGES_TECHNICAL_ARCHITECTURE.md](FACTION_VILLAGES_TECHNICAL_ARCHITECTURE.md)**:
   * Especificación de ingeniería y arquitectura de software para NeoForge 26.3.
   * Inyección en piscinas Jigsaw (`StructureTemplatePool`).
   * Persistencia con `GodVillageSavedData` en `ServerLevel.getDataStorage()`.
   * IA y `Goals` en `GodAcolyteEntity` y comercio sin tocar el `Brain` de aldeanos vanilla.
   * Gestor de incursiones `GodVillageRaidManager` y resolución de 6 conflictos críticos.

3. **[VAMPIRISM_ANALYSIS_AND_IDEAS.md](VAMPIRISM_ANALYSIS_AND_IDEAS.md)**:
   * Deconstrucción conceptual del referente **[TeamLapen/Vampirism](https://github.com/TeamLapen/Vampirism)**.
   * Errores históricos prevenidos (duplicación con Lithostitched, tick lag, desincronización).
   * Ideas innovadoras adaptadas a la mitología de los 7 Pecados Capitales.

4. **[LEGAL_AND_LICENSING_ANALYSIS.md](LEGAL_AND_LICENSING_ANALYSIS.md)**:
   * Análisis legal y desglose multilicencia de Vampirism (LGPLv3, VNCL, Asset License).
   * Metodología de Sala Limpia (*Clean-Room Design*) para preservar la licencia **MIT** de Seven Deadly Gods.

---

## 🛠️ Plan de Implementación por Pasos (`implementacion/`)

Cada paso se encuentra desglosado en su propio documento técnico con código Java, rutas de archivos y validaciones de testing:

* 📄 **[PASO_01_ESTRUCTURAS_Y_WORLDGEN.md](implementacion/PASO_01_ESTRUCTURAS_Y_WORLDGEN.md)**: Inyección Jigsaw (20% prob.), templates NBT de templos y prevención de duplicados con otros mods.
* 📄 **[PASO_02_PERSISTENCIA_Y_DATOS_ALDEA.md](implementacion/PASO_02_PERSISTENCIA_Y_DATOS_ALDEA.md)**: `GodVillageSavedData`, anclaje de templos, máquina de estados y anclas POI permanentes.
* 📄 **[PASO_03_ENTIDADES_IA_Y_COMERCIO.md](implementacion/PASO_03_ENTIDADES_IA_Y_COMERCIO.md)**: Acólitos centinelas armados, targeting por favor (`<= -40`) y comercio divino nativo (`MerchantOffers`).
* 📄 **[PASO_04_ASALTOS_Y_GUERRAS_SANTAS.md](implementacion/PASO_04_ASALTOS_Y_GUERRAS_SANTAS.md)**: `GodVillageRaidManager`, avisos en chat, generación de invasores y asaltos a aldeas neutrales.
* 📄 **[PASO_05_MANIFESTACION_AVATAR_Y_PRIORIDADES.md](implementacion/PASO_05_MANIFESTACION_AVATAR_Y_PRIORIDADES.md)**: Tirada del 5%, regla de Avatar Único, aplazamiento de cazas de ira y reanudación posterior.
* 📄 **[PASO_06_ASEDIO_JUGADOR_Y_TRAICION.md](implementacion/PASO_06_ASEDIO_JUGADOR_Y_TRAICION.md)**: Ítem `DivineWarHornItem`, asedio en nombre del dios primario y detección de Alta Traición (-60 favor).
* 📄 **[PASO_07_DESOLACION_Y_REPOBLACION.md](implementacion/PASO_07_DESOLACION_Y_REPOBLACION.md)**: Estado desolado (0 aldeanos), Caravana de Peregrinos (2 días MC) y ofrenda de restauración.
* 📄 **[PASO_08_PERSONALIDADES_UNICAS_Y_LEVIATAN.md](implementacion/PASO_08_PERSONALIDADES_UNICAS_Y_LEVIATAN.md)**: Aldeas arrasadas de Leviatán, Nitwits de Belphegor, bóvedas de Mammon y atmósfera de los 7 cultos.
* 📄 **[PASO_09_TESTING_VERIFICACION_Y_POLISH.md](implementacion/PASO_09_TESTING_VERIFICACION_Y_POLISH.md)**: Comandos de depuración (`/gods village ...`), batería de pruebas, optimización de chunks y pulido final.
