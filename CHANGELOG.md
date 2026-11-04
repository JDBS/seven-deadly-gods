# Changelog

Todos los cambios notables en este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/),
y este proyecto se adhiere a [Semantic Versioning](https://semver.org/lang/es/).

## [Unreleased]

### Added
- **Gestor Demográfico y Consagración Orgánica de Centinelas (`VillageRecruitmentManager`)**:
  - Implementación de `VillageRecruitmentManager` para evaluar la demografía de aldeas consagradas y convertir aldeanos civiles devotos en acólitos centinelas permanentes (`GodAcolyteEntity`), vinculados a su Altar Mayor.
  - Regla de prudencia demográfica con suelo mínimo civil (`minCivilianFloor = 3`) para evitar extinciones poblacionales y garantizar la continuidad del ciclo de natalidad vanilla permitiendo el reclutamiento en aldeas estándar de 4 habitantes.
  - Jerarquía de selección y scoring: prioridad a Nitwits (100 pts) y desempleados (50 pts); blindaje e inmunidad total para aldeanos con nombre personalizado (`hasCustomName()`), aldeanos en vehículos/transporte (`isPassenger()`), en comercio activo o con oficio avanzado (nivel >= 2).
  - Mecánica de «Sustento Onírico» (*Dream Manna*) en el culto de Belphegor: abastecimiento periódico sincronizado de pan (hasta 6) a Nitwits dormidos durante la noche para superar la parálisis de recolección y habilitar su apareamiento con partículas de reposo.
  - Inyección de la rutina de reclutamiento en el ciclo de 60s de `GodVillageRaidManager` y en el tick ambiental de `VillageCultureApplier`.
  - Comando administrativo `/gods village recruit` para evaluación y ordenación inmediata forzada, e integración de telemetría de guarnición y civiles en `/gods village info`.
- **Persistencia y Configuración de Reclutamiento Demográfico de Centinelas**:
  - Incorporación del campo `lastRecruitmentTick` en `GodVillageSavedData.GodVillageRecord` con soporte de deserialización retrocompatible mediante `Codec.optionalFieldOf("last_recruitment", 0L)`.
  - Propagación de estado temporal de consagración en todos los constructores y métodos mutadores `with...` de `GodVillageRecord`.
  - Nuevos parámetros en `ModConfig` (`sentinel_recruitment`): activación global, cooldown en ticks, suelo mínimo civil y cupos máximos por deidad.
- **Objetivo Táctico de Avatares Rivales y Respuesta Fraternal en Acólitos (`GodAcolyteEntity`)**:
  - Incorporación de `NearestAttackableTargetGoal` en [`GodAcolyteEntity`](src/main/java/com/volcov/sevendeadlygods/entity/GodAcolyteEntity.java) para que los acólitos detecten y ataquen proactivamente a los Avatares de deidades rivales (`GodAvatarEntity`), resolviendo la asimetría táctica donde el Avatar enemigo cazaba acólitos pero los centinelas e invasores ignoraban al dios invasor.
  - Jerarquía táctica escalonada de selección de objetivos: Prioridad 1 (Respuesta defensiva en enjambre), Prioridad 2 (Infantería rival y jugadores con `favor <= -40`), Prioridad 3 (Avatares y Titanes de cultos rivales) y Prioridad 4 (Monstruos comunes para centinelas y escoltas).
  - Alerta fraternal coordinada (`setAlertOthers` con `alertOther` selectivo): al recibir daño de un enemigo, los acólitos movilizan a sus hermanos de fe cercanos sin alertar a miembros de otras deidades.
  - Blindaje contra fuego amigo (`canAttack`): implementación en `GodAcolyteEntity` y `GodAvatarEntity` para evitar que siervos o deidades del mismo pecado se seleccionen o ataquen entre sí durante combates caóticos o ataques de área.
- **Mecánica de Perforación Divina en Duelos de Avatares (God vs God)**:
  - Implementación de daño penetrante en enfrentamientos directos entre Avatares Divinos (`GodAvatarEntity`), superando las capas extremas de armadura y protección vainilla para garantizar un combate dinámico, equilibrado (~1.5 a 2 minutos) y con peligro real de deicidio.
  - Efectos audiovisuales de Choque Divino (`playDivineClashEffects`): partículas temáticas del pecado del dios atacante (destellos áureos de Mammon, vórtice cósmico de Belphegor, etc.) y resonancia acústica pesada de impacto al conectar golpes penetrantes.
- **Comportamiento Hostil y Despojo de Acólitos de la Envidia (`GodAcolyteEntity`, `PlayerSinBehaviorEvents`)**:
  - Incorporación de `NearestAttackableTargetGoal` en [`GodAcolyteEntity`](src/main/java/com/volcov/sevendeadlygods/entity/GodAcolyteEntity.java) para que los acólitos de Leviatán (`SinGod.ENVY`) persigan y ejecuten a los aldeanos civiles (`Villager.class`) conforme a su lore de saqueo y despojo.
  - Objetivo proactivo de ataque contra gólems de hierro defensores (`IronGolem.class`) para huestes asaltantes e invasoras de Envidia.
  - Efectos visuales de impacto con partículas abisales (`SPLASH` y `ASH`) al empalar aldeanos con el tridente.
  - Atribución de bajas de aldeanos infligidas por acólitos aliados al jugador comandante, otorgando el favor correspondiente con Leviatán (`+0.6`).

### Changed
- **Ajuste de Frecuencia y Configuración de Aldeas Consagradas (`ConsecratedVillageShrineElement`, `ModConfig`)**:
  - Incremento de la tasa de consagración de aldeas naturales al **80%** (frente al 20% anterior), dejando exactamente el **20%** restante como aldeas mundanas/vanilla tradicionales.
  - Nuevo parámetro de configuración `villageConsecrationChance` en la categoría `[world]` de `ModConfig` (por defecto `0.80`, rango `0.0` a `1.0`) para permitir calibración total en servidores y partidas locales.
- **Indestructibilidad del Equipamiento Sagrado de Avatares**:
  - Asignación del componente `DataComponents.UNBREAKABLE` a todas las armas y piezas de armadura de los Avatares en `DivineEquipmentHelper`, evitando que el desgaste por durabilidad o el efecto reflejo de *Espinas V* destruya las piezas en combate prolongado.
- **Estandarización de Defensa Temática (Oro Divino vs Netherite)**:
  - Calibración de atributos defensivos en `GodAvatarEntity.initializeAvatar` para igualar la dureza (+24) y resistencia al empuje (1.0) entre deidades de temática áurea (Mammon, Lucian, Asmodeus) y deidades con armadura de netherite (Belphegor, Khorvath, etc.), asegurando paridad defensiva matemática sin alterar la salud máxima (400 HP).

### Fixed
- **Eliminación de Rutas Absolutas de Máquina Local (`c/Users/VolcoV`)**:
  - Limpieza del fallback local de JVM en el script de arranque `gradlew`, garantizando la ejecución estándar en cualquier entorno y sistema operativo.
  - Portabilidad de las configuraciones de lanzamiento en `.vscode/launch.json` mediante la variable `${workspaceFolder}`.
  - Conversión de enlaces en la documentación interna (`docs/dev/`) a rutas relativas dentro del repositorio.
- **Consistencia de Letargo Cósmico y Seguridad contra Nulos (`DivineChatRouter`, `SinFavorManager`, `GeminiAiService`)**:
  - Corrección de inconsistencia donde un dios abatido en su encarnación física continuaba conversando en el chat, otorgando favor y lanzando maldiciones/bendiciones pese a que su Altar Mayor yacía inerte.
  - Emisión de mensaje de silencio cósmico (`chat.sevendeadlygods.god_in_slumber`) y efecto de sonido `SOUL_ESCAPE` al llamar directamente a deidades dormidas.
  - Bloqueo de reacciones físicas divinas (rayos, maldiciones, bendiciones) y de re-invocación presencial benévola (`tryTriggerBenevolentAvatar`) durante el letargo.
  - Congelación de fluctuaciones de favor activas por chat y pasivas por comportamiento durante el letargo cósmico.
  - Notificación de deidades en letargo al modelo de lenguaje Gemini y silenciamiento automático en fallbacks locales.
  - Resolución de advertencia de análisis estático en `DivineChatRouter` garantizando no-nulidad de `PlayerSinData` en la invocación de Avatares.
- **Actualización de Metadatos NeoForge (`neoforge.mods.toml`)**:
  - Sustitución de la clave obsoleta `logoFile` por `iconFile` para la imagen cuadrada del mod ([`logo.png`](src/main/resources/logo.png)), eliminando la advertencia de deprecación en la consola del cliente.

## [1.2.0] - 2026-11-02

### Added
- **Identidad Visual y Branding Oficial del Mod**:
  - Incorporación de logo oficial en alta resolución ([`logo.png`](src/main/resources/logo.png)) y vinculación directa en metadatos de NeoForge ([`neoforge.mods.toml`](src/main/templates/META-INF/neoforge.mods.toml) con clave `logoFile`).
  - Carátula oficial de presentación para CurseForge ([`curseforge_cover.png`](docs/curseforge_cover.png)).
- **Automatización CI/CD y Flujo Operacional de Ramas**:
  - Configuración de workflow de GitHub Actions ([`auto-pr-to-experimental.yml`](.github/workflows/auto-pr-to-experimental.yml)) para creación automática e idempotente de Pull Requests hacia la rama base `experimental` tras cada `git push` de ramas operacionales (`feat/*`, `fix/*`, `bugfix/*`, `refactor/*`, `perf/*`, `chore/*`, `breaking/*`).
  - Desbloqueo y seguimiento del directorio de workflows `.github/` en [`.gitignore`](.gitignore).
- **Localización Integral y Adaptación Bilingüe Dinámica (Inglés y Español)**:
  - Soporte de cliente nativo mediante `Component.translatable` en la totalidad de sistemas interactivos del mod: 49 Tomos de Lore Sagrado, nombres, títulos e identidades de las 7 Deidades y sus siervos (`GodAcolyteEntity`, `GodAvatarEntity`, `GodEnvoySkeleton`), diálogos y pistas místicas de ritual en altares (`SinAltarBlockEntity`), transmisiones globales de Guerras Santas y asedios territoriales (`GodVillageRaidManager`), reliquias bendecidas y trofeos de deicidio conmemorativos (`DivineEquipmentHelper`), catálogo íntegro de comercio sagrado de 23 artefactos (`DivineTradingRegistry`), exigencias culturales de aldea (`VillageCultureApplier`), notificaciones de traición y salvación divina (`PlayerSinBehaviorEvents`) y uso táctico del cuerno de guerra (`DivineWarHornItem`).
  - Adaptación lingüística contextual en el servicio de IA Gemini (`GeminiAiService`): captura del idioma del cliente (`player.clientInformation().language()`) inyectada en el prompt del sistema para que las deidades respondan orgánicamente en el idioma utilizado por el devoto (inglés o español), incorporando además vocabulario bilingüe de invocación directa y un motor de evaluación local de respaldo con respuestas completas en ambos idiomas.
  - Paridad estricta del 100% con 476 claves entre los archivos de lenguaje `es_es.json` y `en_us.json`.
- **Recompensa de Experiencia y Sabiduría Arcana en Libros Sagrados**:
  - Al asimilar o leer por primera vez cualquiera de las 49 escrituras sagradas (`PlayerSinBehaviorEvents.handleLoreBookRead`), el jugador recibe `25` puntos de experiencia (configurable mediante `loreBookFirstReadXp` en `ModConfig`), acompañado de partículas de encantamiento (`ParticleTypes.ENCHANT`), efectos acústicos sagrados (`SoundEvents.EXPERIENCE_ORB_PICKUP`) y mensaje temático en el chat.
  - Validación de mano principal (`InteractionHand.MAIN_HAND`) al consultar atriles (`LecternBlockEntity`) para una interacción más limpia y consistente.
- **Huevos Generadores de Acólitos y Centinelas por Deidad (`GodAcolyteSpawnEggItem`, `ModItems`)**:
  - Implementación de 7 Huevos Generadores dedicados para cada Dios del Pecado (`greed_acolyte_spawn_egg`, `wrath_acolyte_spawn_egg`, `gluttony_acolyte_spawn_egg`, `pride_acolyte_spawn_egg`, `envy_acolyte_spawn_egg`, `sloth_acolyte_spawn_egg`, `lust_acolyte_spawn_egg`) con paletas de color y manchas temáticas auténticas de Minecraft (16×16).
  - Dualidad de invocación: clic derecho convencional genera un Acólito armado con sus atributos y armas sagradas; clic derecho con `Shift` (agachado) genera un Centinela Permanente pacífico para devotos con comercio divino habilitado.
  - Inclusión de los 7 huevos generadores en la pestaña creativa de Seven Deadly Gods y traducciones completas en `es_es.json` y `en_us.json`.
- **Comando de Ayuda Adaptativo (`/gods help`)**:
  - Subcomando público `/gods help` (y ejecución por defecto de `/gods` sin argumentos) que presenta un índice estructurado y estilizado de todos los comandos disponibles, adaptando dinámicamente la información según si el usuario es un jugador común o un operador (OP).

### Fixed
- **Avance de Deicidio en Minecraft 26.3 (`slay_god_avatar.json`)**: corrección de la definición del criterio de avance sustituyendo el disparador `minecraft:player_killed_entity` con predicado incompatible por `minecraft:impossible`, resolviendo el crash fatal de arranque por error de carga de registros (`loot_condition_type`) y alineándolo con la arquitectura del mod donde el logro es otorgado programáticamente por `GodAvatarEntity` vía `ModAdvancements.award(...)`.
- **Texturizado y Mapeo UV de Entidades (`GodAcolyteModel`, `GodAvatarRenderer`)**:
  - Reestructuración completa del mapa UV de [`GodAcolyteModel`](src/main/java/com/volcov/sevendeadlygods/client/model/GodAcolyteModel.java) a resolución 64×128, eliminando el solapamiento de coordenadas entre las alas y el torso, y reubicando la nariz para evitar colisiones con la cabeza.
  - Generación de texturas dedicadas con canal alfa y sombreado para los Acólitos y Centinelas en los 7 Pecados Capitales, aplicando apertura transparente en la capucha y túnica ceremonial para revelar el rostro y evitar cubos macizos inflados.
  - Creación de skins completas de formato estándar de jugador (64×64) para los Avatares Divinos de los 7 Pecados, con capas exteriores transparentes y alineación pixel-perfect con la capa emisiva de ojos brillantes ([`avatar_glowing_eyes.png`](src/main/resources/assets/sevendeadlygods/textures/entity/avatar/avatar_glowing_eyes.png)).

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

## [1.0.0] - 2026-11-02

### Added
- Creación de changelog inicial.
- Sistema base de los 7 Dioses del Pecado impulsados por IA (Google Gemini y motor temático local).
- Bloques de Altar de Ritual asimétricos para los 7 Pecados Capitales (`greed`, `wrath`, `gluttony`, `pride`, `envy`, `sloth`, `lust`).
- Sistema de Favor e Ira escalar (-100 a +100) con amortiguación progresiva y efectos de bendición mayor.
- Entidades `GodAcolyteEntity` (siervos alados) y `GodAvatarEntity` (avatares colosales con regla de Avatar Único).
- Biblioteca de 42 libros sagrados de lore (`SinLoreLibrary`) y sistema de logros.
