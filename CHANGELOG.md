# Changelog

Todos los cambios notables en este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/),
y este proyecto se adhiere a [Semantic Versioning](https://semver.org/lang/es/).

## [1.9.0] - 2026-11-02

### Added
- Subcomandos de administración y depuración en `GodsCommands`: `/gods village info`, `/gods village consecrate <dios>`, `/gods village raid [dios_atacante] [forzar_avatar]` y `/gods village desolate`.
- Parámetros configurables `villageRaidCooldownTicks` y `villageRaidAvatarChance` en `ModConfig`.
- Documentación completa de las Aldeas Consagradas, Comercio Divino, Guerras Santas, Cuerno de Guerra Divino, Desolación y nuevos comandos en `README.md`.

### Fixed
- Corrección de referencias de texturas de cuarzo en el modelo `pride_altar.json` para Minecraft 26.3 (`quartz_block_side` y `quartz_block_bottom`).

## [1.8.0] - 2026-11-02

### Added
- `VillageCultureApplier`: inicializa la guarnición de centinelas permanentes al cargar un santuario aldeano y aplica las 7 personalidades culturales y sociales de culto:
  - **Belphegor (Pereza)**: convierte a todos los aldeanos en Holgazanes (`VillagerProfession.NITWIT`), despliega un Gólem de Hierro trabajador y otorga aura de Regeneración a los devotos en reposo.
  - **Mammon (Avaricia)**: exige mostrar oro en mano para abrir puertas y cobra peaje de 1 Pepita de Oro al abrir cofres/barriles en sus aldeas (salvo Discípulos con `favor >= +60`), con predilección por aldeanos Armeros.
  - **Lucian (Soberbia)**: protocolo imperial que exige arrodillarse (`Shift`) o tener `favor >= +60` para hablar con los aldeanos.
  - **Beelze (Gula)**: predilección por aldeanos Carniceros, humo de festín y saciedad pasiva a los devotos.
  - **Asmodeus (Lujuria)**: partículas musicales/corazones y feromonas que inducen la reproducción pasiva de animales.
  - **Khorvath (Ira)**: guarnición reforzada de 6 Acólitos y llamas de alma.
  - **Leviatán (Envidia)**: asentamientos en estado `ARRASADA` con ceniza y goteo abisal sobre las ruinas.

## [1.7.0] - 2026-11-02

### Added
- Ciclo de **Desolación y Caravana de Peregrinos** en `GodVillageRaidManager`: detecta aldeas consagradas con `0` aldeanos vivos, transiciona su estado a `DESOLADA` (emitiendo humo negro de vigilia e invocando 2 Gólems de Hierro guardianes en aldeas de Belphegor) y envía automáticamente una Caravana de Peregrinos (3 aldeanos + 1 Acólito centinela; 4 aldeanos en la mitad de tiempo para Asmodeus) tras 2 días de Minecraft (`48,000` ticks).
- **Milagro de Restauración** en `SinAltarBlockEntity`: permite a jugadores con `favor >= +30` ofrecer un tributo sagrado en el Altar Mayor de una aldea `DESOLADA` para invocar inmediatamente la Caravana de Peregrinos y revitalizar el templo.

## [1.6.0] - 2026-11-02

### Added
- `DivineWarHornItem` (`divine_war_horn` — *Cuerno de Guerra Divino: Heraldo del Pecado*): permite a jugadores con `favor >= +40` liderar un asedio contra aldeas rivales consagradas e invocar un escuadrón de 6 Acólitos de asalto leales.
- Detección de **Alta Traición** en `PlayerSinBehaviorEvents`: penaliza inmediatamente con `-60` puntos de favor directos (`SinFavorManager.addDirectFavor`) cuando un jugador con `favor >= 0` ataca a un `GodAcolyteEntity` de ese dios, hiere a un `Villager` dentro de su aldea consagrada o profana su Altar Mayor.

## [1.5.0] - 2026-11-02

### Added
- Tirada del 5% de Manifestación Divina de `GodAvatarEntity` durante los asedios territoriales (incluyendo un 15% de probabilidad de *Choque de Dioses* con descenso simultáneo de ambos Avatares rivales).
- Prioridad absoluta del asedio territorial sobre las Cazas de Ira individuales (`POSTPONED_WRATH_HUNTS`): respeta estrictamente la Regla de Avatar Único, pausa la caza activa, teletransporta al Avatar al santuario preservando su salud y reanuda la caza al concluir la batalla si el Avatar sobrevive.

## [1.4.0] - 2026-11-02

### Added
- `GodVillageRaidManager` para gestionar los Asaltos Autónomos y Guerras Santas entre facciones divinas (evaluación cada minuto con enfriamiento de 3 días de Minecraft, anuncios globales en el chat, despliegue de 8 acólitos invasores, pausa automática ante descarga de chunks y resolución de victoria defensiva o conquista/arrasamiento del templo).

## [1.3.0] - 2026-11-02

### Added
- `DivineTradingRegistry` con el catálogo completo de Comercio Divino de 5 niveles de devoción para cada uno de los 7 Dioses del Pecado (reliquias, artefactos con lore y libros sobre-encantados como *Fortuna IV* y *Filo VI*).
- Implementación de `Merchant` en `GodAcolyteEntity`, modo Centinela Permanente de Aldea (`villageSentinel`), IA de hostilidad territorial (`favor <= -40`), rechazo de comercio (`-39 <= favor < 0` con sonido *Villager No*) y cierre automático de inventario si el favor cae por debajo de `0`.

## [1.2.0] - 2026-11-02

### Added
- `GodVillageState` con la máquina de estados territoriales (`MUNDANA`, `CONSAGRADA`, `EN_ASEDIO`, `DESOLADA`, `ARRASADA`).
- `GodVillageSavedData` (`SavedData` y `SavedDataType` con `Codec`) para persistir los datos de aldeas consagradas por dimensión y evitar altares duplicados en un radio de 128 bloques.
- Auto-registro de santuarios aldeanos en `SinAltarBlockEntity`, bonificación de +20% al esfuerzo devocional en altares mayores de aldea y exención de altitud para catedrales aldeanas de Soberbia.

## [1.1.0] - 2026-11-02

### Added
- Plantillas de estructura Jigsaw `.nbt` de templos aldeanos consagrados para los 7 Dioses del Pecado (`greed_shrine`, `wrath_shrine`, `gluttony_shrine`, `pride_shrine`, `envy_shrine`, `sloth_shrine`, `lust_shrine`, `god_shrine` y `ruined_envy_shrine`).
- `VillageStructureInjector` para inyectar dinámicamente los santuarios divinos en los `StructureTemplatePool` de aldeas vanilla (`plains`, `desert`, `savanna`, `taiga`, `snowy`) con una probabilidad aproximada del 20% por aldea.

## [1.0.0] - 2026-11-02

### Added
- Creación de changelog inicial.
- Sistema base de los 7 Dioses del Pecado impulsados por IA (Google Gemini y motor temático local).
- Bloques de Altar de Ritual asimétricos para los 7 Pecados Capitales (`greed`, `wrath`, `gluttony`, `pride`, `envy`, `sloth`, `lust`).
- Sistema de Favor e Ira escalar (-100 a +100) con amortiguación progresiva y efectos de bendición mayor.
- Entidades `GodAcolyteEntity` (siervos alados) y `GodAvatarEntity` (avatares colosales con regla de Avatar Único).
- Biblioteca de 42 libros sagrados de lore (`SinLoreLibrary`) y sistema de logros.
