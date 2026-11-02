# Changelog

Todos los cambios notables en este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/),
y este proyecto se adhiere a [Semantic Versioning](https://semver.org/lang/es/).

## [1.1.0] - 2026-11-02

### Added
- **Estructuras y WorldGen de Aldeas Consagradas**:
  - Plantillas de estructura Jigsaw `.nbt` (`DataVersion: 5023`) de templos aldeanos consagrados para los 7 Dioses del Pecado (`greed_shrine`, `wrath_shrine`, `gluttony_shrine`, `pride_shrine`, `envy_shrine`, `sloth_shrine`, `lust_shrine`, `god_shrine` y `ruined_envy_shrine`).
  - `VillageStructureInjector` para inyectar dinámicamente los santuarios divinos en los `StructureTemplatePool` de aldeas vanilla (`plains`, `desert`, `savanna`, `taiga`, `snowy`) con una probabilidad aproximada del 20% por aldea.
- **Persistencia Territorial y Hooks de Altar**:
  - `GodVillageState` con la máquina de estados territoriales (`MUNDANA`, `CONSAGRADA`, `EN_ASEDIO`, `DESOLADA`, `ARRASADA`).
  - `GodVillageSavedData` (`SavedData` y `SavedDataType` con `Codec`) para persistir los datos de aldeas consagradas por dimensión y evitar altares duplicados en un radio de 128 bloques.
  - Auto-registro de santuarios aldeanos en `SinAltarBlockEntity`, bonificación de `+20%` al esfuerzo devocional en altares mayores de aldea y exención de altitud para catedrales aldeanas de Soberbia.
- **IA Centinela y Comercio Divino**:
  - `DivineTradingRegistry` con el catálogo completo de Comercio Divino de 5 niveles de devoción para cada uno de los 7 Dioses del Pecado (reliquias, artefactos con lore y libros sobre-encantados como *Fortuna IV* y *Filo VI*).
  - Implementación de `Merchant` en `GodAcolyteEntity`, modo Centinela Permanente de Aldea (`villageSentinel`), IA de hostilidad territorial (`favor <= -40`), rechazo de comercio (`-39 <= favor < 0` con sonido *Villager No*) y cierre automático de inventario si el favor cae por debajo de `0`.
- **Asaltos Autónomos, Guerras Santas y Regla de Avatar Único**:
  - `GodVillageRaidManager` para gestionar los Asaltos Autónomos y Guerras Santas entre facciones divinas (evaluación cada minuto con enfriamiento configurable de 3 días de Minecraft, anuncios globales en el chat, despliegue de 8 acólitos invasores, pausa automática ante descarga de chunks y resolución de victoria defensiva o conquista/arrasamiento del templo).
  - Tirada del 5% de Manifestación Divina de `GodAvatarEntity` durante los asedios territoriales (incluyendo un 15% de probabilidad de *Choque de Dioses* con descenso simultáneo de ambos Avatares rivales).
  - Prioridad absoluta del asedio territorial sobre las Cazas de Ira individuales (`POSTPONED_WRATH_HUNTS`): respeta estrictamente la Regla de Avatar Único, pausa la caza activa, teletransporta al Avatar al santuario preservando su salud y reanuda la caza al concluir la batalla si el Avatar sobrevive.
- **Asedios Liderados por el Jugador y Alta Traición**:
  - `DivineWarHornItem` (`divine_war_horn` — *Cuerno de Guerra Divino: Heraldo del Pecado*): permite a jugadores con `favor >= +40` liderar un asedio contra aldeas rivales consagradas e invocar un escuadrón de 6 Acólitos de asalto leales.
  - Detección de **Alta Traición** en `PlayerSinBehaviorEvents`: penaliza inmediatamente con `-60` puntos de favor directos (`SinFavorManager.addDirectFavor`) cuando un jugador con `favor >= 0` ataca a un `GodAcolyteEntity` de ese dios, hiere a un `Villager` dentro de su aldea consagrada o profana su Altar Mayor.
- **Desolación, Caravana de Peregrinos y Milagro de Restauración**:
  - Detección de aldeas consagradas con `0` aldeanos vivos en `GodVillageRaidManager`, transicionando su estado a `DESOLADA` (emitiendo humo negro de vigilia e invocando 2 Gólems de Hierro guardianes en aldeas de Belphegor) y enviando automáticamente una Caravana de Peregrinos (3 aldeanos + 1 Acólito centinela; 4 aldeanos en la mitad de tiempo para Asmodeus) tras 2 días de Minecraft (`48,000` ticks).
  - **Milagro de Restauración** en `SinAltarBlockEntity`: permite a jugadores con `favor >= +30` ofrecer un tributo sagrado en el Altar Mayor de una aldea `DESOLADA` para invocar inmediatamente la Caravana de Peregrinos y revitalizar el templo.
- **Personalidades Culturales de los 7 Dioses (`VillageCultureApplier`)**:
  - **Belphegor (Pereza)**: convierte a todos los aldeanos en Holgazanes (`VillagerProfession.NITWIT`), despliega un Gólem de Hierro trabajador y otorga aura de Regeneración a los devotos en reposo.
  - **Mammon (Avaricia)**: exige mostrar oro en mano para abrir puertas y cobra peaje de 1 Pepita de Oro al abrir cofres/barriles en sus aldeas (salvo Discípulos con `favor >= +60`), con predilección por aldeanos Armeros.
  - **Lucian (Soberbia)**: protocolo imperial que exige arrodillarse (`Shift`) o tener `favor >= +60` para hablar con los aldeanos.
  - **Beelze (Gula)**: predilección por aldeanos Carniceros, humo de festín y saciedad pasiva a los devotos.
  - **Asmodeus (Lujuria)**: partículas musicales/corazones y feromonas que inducen la reproducción pasiva de animales.
  - **Khorvath (Ira)**: guarnición reforzada de 6 Acólitos y llamas de alma.
  - **Leviatán (Envidia)**: asentamientos en estado `ARRASADA` con ceniza y goteo abisal sobre las ruinas.
- **Comandos de Administración, Configuración y Documentación**:
  - Subcomandos `/gods village info`, `/gods village consecrate <dios>`, `/gods village raid [dios_atacante] [forzar_avatar]` y `/gods village desolate` en `GodsCommands`.
  - Parámetros configurables `villageRaidCooldownTicks` y `villageRaidAvatarChance` en `ModConfig`.
  - Documentación completa de las Aldeas Consagradas y sus mecánicas en `README.md`.
- **Reorganización Modular de la Documentación para GitHub**:
  - Desacoplamiento del `README.md` monolítico hacia una arquitectura modular en `docs/` con guías temáticas navegables (`guia_inicio/`, `mecanicas_principales/`, `dioses_y_altares/`, `aldeas_consagradas/`, `lore_y_avances/` y `administracion/`).
  - Rediseño del `README.md` principal como portal de bienvenida con badges, resumen visual y mapa interactivo hacia la Wiki.

- **Biblioteca Sagrada Ampliada (49 Tomos Arcanos)**:
  - Registro del 7º Libro Sagrado para cada uno de los 7 Dioses del Pecado (`greed_7`, `wrath_7`, `gluttony_7`, `pride_7`, `envy_7`, `sloth_7`, `lust_7`), narrando en prosa bíblica los orígenes de sus Santuarios Mayores, leyes territoriales, centinelas y guerras santas.
  - Actualización de los textos de avance de «Archivero de los Siete Dioses» para reflejar la colección de 49 tomos sagrados.
- **Licencia y Distribución Open Source**:
  - Incorporación formal del archivo de licencia [LICENSE](LICENSE) bajo términos MIT (Copyright (c) 2026 JDBS).
- **Sistema de Avances y Logros de Aldeas Consagradas (14 Nuevos Avances)**:
  - **7 Avances Normales**: «Santuario de los Fieles» (`village_discovery`), «Comercio Devoto» (`divine_trade_master`), «Heraldo de la Guerra Santa» (`divine_war_horn_use`), «Bastión de la Fe» (`defend_shrine_victory`), «Conquistador de Altares» (`temple_conquest`), «Aliento de los Dioses» (`village_miracle_restore`) y «Deicidio» (`slay_god_avatar`).
  - **7 Logros Ocultos y Secretos**: «Alta Traición» (`secret_high_treason`), «Choque de Inmortales» (`secret_clash_of_the_gods`), «Envidia Devoradora» (`secret_ruined_envy_village`), «La Siesta de los Fieles» (`secret_sloth_society`), «De Rodillas ante la Realeza» (`secret_pride_humiliation`), «Peaje de Oro» (`secret_greed_tax_paid`) y «Pasión Desenfrenada» (`secret_lust_wild_passion`).
  - Archivos JSON de avances en `data/sevendeadlygods/advancement/` con textos 100% localizados en inglés (`en_us.json`) y español (`es_es.json`).
  - Hooks de activación en `GodAvatarEntity`, `PlayerSinBehaviorEvents`, `DivineTradingRegistry`, `GodAcolyteEntity`, `DivineWarHornItem`, `GodVillageRaidManager`, `SinAltarBlockEntity` y `VillageCultureApplier`.

### Fixed
- **Identidad de Aldeas en Jigsaw (`TownCenterTrackerElement` / `ConsecratedVillageShrineElement` / `VillageStructureInjector`)**: corrección del sesgo global originado por `BlockPos.ZERO` en la enumeración de conectores candidatos; ahora se captura la coordenada de inicio real (`adjustedPosition`) en los centros de aldea (`town_centers`) y se aísla por hilo (`ThreadLocal`), garantizando que cada aldea tire su 20% de consagración y elija su deidad de forma totalmente independiente.
- **Reserva y Rollback de Santuarios ante Colisiones Jigsaw (`ConsecratedVillageShrineElement`)**: postergación de la reserva del cupo del santuario (`placed = true`) hasta que la pieza candidata es formalmente aceptada en la estructura (`getGroundLevelDelta` vía `Projection.TERRAIN_MATCHING` o procesamiento de pieza aceptada), evitando que candidatos descartados por colisión con casas vanilla bloqueen el templo de la aldea.
- **Resolución Dimensional del Altar en Cazas Pospuestas (`DivineInterventionScheduler`)**: resolución de la aldea del Avatar activo consultando el `ServerLevel` del Avatar en lugar de la dimensión del jugador perseguido en `triggerWrathAvatarHunt`, previniendo claves erróneas de asedio (`RaidGodKey`) cuando el jugador se encuentra en otra dimensión (ej. Nether o End).
- **Ajuste y Balance de Atributos del Avatar (`GodAvatarEntity` / `DivineEquipmentHelper`)**: salud calibrada a 400 HP, resistencia al daño innata al 50%, reducción moderada de regeneración continua y encantamiento de armadura ajustado a Protección V para combates más dinámicos.
- **Ciclo de Vida y Limpieza de Avatares en Choque de Dioses (`GodAvatarEntity` / `GodVillageRaidManager`)**: desacople de la posición canónica del altar de la aldea y del `raidVillageId` respecto al desplazamiento físico de aparición (`offset(±4, 0, 0)`), asegurando que ambos Avatares sean correctamente descartados al finalizar el asedio sin quedar bloqueados de por vida en el mundo ni impedir futuras manifestaciones divinas.
- **Sincronización de Enfriamiento de Asedios en `/gods village info` (`GodsCommands`)**: cálculo dinámico del tiempo restante consultando `ModConfig.VILLAGE_RAID_COOLDOWN_TICKS.get()` en lugar de la constante fija de 72,000 ticks, garantizando exactitud con la configuración personalizada del servidor.
- **Aislamiento y Reanudación de Cazas de Ira Pospuestas (`DivineInterventionScheduler` / `GodVillageRaidManager`)**: resolución del ID de aldea del avatar activo al posponer cacerías (`postponeWrathHunt`) y verificación estricta de propiedad del asedio antes de consumir cacerías globales en `onVillageRaidEnded`, evitando pérdidas colaterales de venganzas pendientes.
- **Balance de Tropas en Asedios del Cuerno de Guerra (`DivineWarHornItem` / `GodVillageRaidManager`)**: discriminación de incursiones lideradas por jugador frente a asaltos autónomos en `startRaidOnVillage`, eliminando el spawneo redundante de invasores y desplegando exactamente el escuadrón táctico de 6 Acólitos subordinados documentado.
- **Inyección Jigsaw Calibrada y Límite de Santuario Único (`VillageStructureInjector` / `ConsecratedVillageShrineElement`)**: implementación de `ConsecratedVillageShrineElement` para ejecutar la tirada de consagración del 20% una sola vez por aldea e imponer un límite estricto de exactamente 1 santuario por poblado, evitando templos múltiples o de deidades rivales coexistiendo en la misma aldea.
- **IA y Lealtad de Acólitos Centinelas (`GodAcolyteEntity`)**: desvinculación explícita del jugador comandante (`assignedPlayerUuid = null`) y reinicio de objetivo al convertirse en centinela permanente tras la conquista de una aldea.
- **Persistencia y Comportamiento de Avatares en Asedio (`GodAvatarEntity`)**: serialización completa de `villageRaidAvatar` y `raidAltarPos` en NBT para evitar expiración tras 4.800 ticks o recarga de chunks, y suspensión inmediata del goal `StayNearChampionGoal` al transferirse a un asedio.
- **Gestión de Asedios y Sincronización Territorial (`GodVillageRaidManager`)**:
  - Persistencia del dios atacante (`attackerGod`) y tiempo activo de asedio (`activeRaidTicks`) en `GodVillageSavedData`, impidiendo victorias defensivas erróneas por sorteo aleatorio tras reiniciar el servidor.
  - Pausa efectiva del timeout máximo (6.000 ticks) cuando no hay jugadores en 128 bloques, midiendo exclusivamente duración activa de combate.
  - Scoping estricto de Avatares y cazas de ira pospuestas por aldea específica (`RaidGodKey`), evitando descartar avatares que combatan en otras aldeas.
  - Recarga del registro aldeano tras `checkDesolationState()` para proteger el ciclo de duelo de aldeas desoladas contra sobrescrituras inmediatas de asedios.
  - Filtro estricto en `ensureMinimumDefenders()` contabilizando exclusivamente centinelas permanentes del altar para evitar que escoltas aliadas temporales agoten el cupo de la guarnición.
- **Interacción y Comercio en Aldeas de Soberbia (`VillageCultureApplier`)**: apertura explícita del menú de comercio del aldeano al arrodillarse (`Shift`) con favor inferior a +60, evitando que la interacción secundaria vanilla bloquee el intercambio.
- **Cobro Preciso del Peaje de Avaricia (`VillageCultureApplier`)**: migración del cobro de la pepita de oro al evento `PlayerContainerEvent.Open`, cobrando únicamente cuando el cofre o barril se abre exitosamente y previniendo pérdidas de oro ante cofres bloqueados o intentos fallidos.
- **Correcciones de Compilación y Tipado Estricto (Java / NeoForge 26.3)**:
  - `GodVillageRaidManager`: añadido el `import java.util.concurrent.ConcurrentHashMap` que impedía compilar `POSTPONED_WRATH_HUNTS` y `POSTPONED_GLOBAL_WRATH_HUNTS`.
  - `VillageCultureApplier`: llamada corregida al método de record `villager.getVillagerData().level()` en lugar de `getLevel()` en el protocolo de reverencia de Soberbia.
  - Resolución completa de advertencias del compilador de Eclipse / JDT LS sobre `Null type safety` (`@Nonnull LivingEntity` y funciones de `Codec`) mediante expresiones lambda explícitas en `GodVillageRaidManager`, `VillageCultureApplier`, `GodsCommands` y `GodVillageSavedData`.
- Corrección de referencias de texturas de cuarzo en el modelo `pride_altar.json` para Minecraft 26.3 (`quartz_block_side` y `quartz_block_bottom`).
- **Avance de Deicidio en Minecraft 26.3 (`slay_god_avatar.json`)**: corrección de la definición del criterio de avance sustituyendo el disparador `minecraft:player_killed_entity` con predicado incompatible por `minecraft:impossible`, resolviendo el crash fatal de arranque por error de carga de registros (`loot_condition_type`) y alineándolo con la arquitectura del mod donde el logro es otorgado programáticamente por `GodAvatarEntity` vía `ModAdvancements.award(...)`.

## [1.0.0] - 2026-11-02

### Added
- Creación de changelog inicial.
- Sistema base de los 7 Dioses del Pecado impulsados por IA (Google Gemini y motor temático local).
- Bloques de Altar de Ritual asimétricos para los 7 Pecados Capitales (`greed`, `wrath`, `gluttony`, `pride`, `envy`, `sloth`, `lust`).
- Sistema de Favor e Ira escalar (-100 a +100) con amortiguación progresiva y efectos de bendición mayor.
- Entidades `GodAcolyteEntity` (siervos alados) y `GodAvatarEntity` (avatares colosales con regla de Avatar Único).
- Biblioteca de 42 libros sagrados de lore (`SinLoreLibrary`) y sistema de logros.
