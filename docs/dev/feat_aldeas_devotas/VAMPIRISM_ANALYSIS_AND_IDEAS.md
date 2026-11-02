# 🧛 Análisis Técnico de Vampirism (TeamLapen) y Lecciones de Diseño

Este documento analiza en profundidad el funcionamiento del mod **[Vampirism (TeamLapen)](https://github.com/TeamLapen/Vampirism)** en lo relativo a sus aldeas de facción, tótems de control, generación de estructuras y asedios, extrayendo las mejores prácticas, errores conocidos de la comunidad y propuestas de adaptación para el mod **Seven Deadly Gods** en **Minecraft 26.3 / NeoForge**.

---

## 1. Deconstrucción del Sistema de Aldeas de Vampirism

En el mod *Vampirism*, las aldeas de Minecraft dejan de ser simples asentamientos pasivos y se convierten en enclaves estratégicos disputados entre facciones: **Vampiros (`vampires`)**, **Cazadores (`hunters`)** y **Aldeanos Neutros (`neutral`)**.

### 1.1 El Tótem de la Aldea (`TotemBlock` y `TotemBlockEntity`)
El núcleo funcional de cada aldea en Vampirism es el bloque **Village Totem**:
* **Estructura multibloque visual**: Se compone típicamente de una base (`totem_base`) y una cúspide (`totem_top`). El bloque superior funciona además como bloque de trabajo (*Job Site Block / POI*) para profesiones exclusivas de la facción gobernante.
* **Estado interno persistente**: El `BlockEntity` del tótem almacena:
  1. **Facción dominante**: Identificador de la facción dueña (`Faction`).
  2. **Nivel/Fuerza de la aldea (`Village Level`)**: Determina el número y poder de los guardias que patrullan, así como la calidad de los intercambios comerciales.
  3. **Radio de influencia territorial**: Bounding box esférico o cúbico (típicamente 48 a 80 bloques) en el cual los aldeanos y guardias reconocen a aliados y enemigos.
  4. **Estado de Asedio/Incursión (`Raid State`)**: Bandera booleana y temporizadores de oleadas activas.
* **Propagación estética y ambiental**: La facción del tótem cambia la iluminación (niebla/luces oscuras en aldeas vampíricas), los estandartes colgados en las casas y la apariencia visual de los aldeanos.

### 1.2 Generación en el Mundo (Jigsaw y Template Pools)
* Vampirism aprovecha el sistema **Jigsaw** de Minecraft (`StructureTemplatePool`) para inyectar su estructura de tótem en las aldeas naturales de los biomas vanilla (`plains`, `desert`, `taiga`, `savanna`, `snowy`).
* Cada aldea tiene una probabilidad calculada al generarse de albergar un tótem bajo el control de una facción específica o permanecer neutral.

### 1.3 Dinámica de Población y Comportamiento (Guardias vs Aldeanos)
* **Reemplazo / Conversión de Aldeanos**: Los aldeanos de una aldea capturada pueden ser convertidos en siervos o reemplazados por aldeanos de facción (`Faction Villagers`) que visten las túnicas correspondientes.
* **Guardias de Facción**: Se generan entidades agresivas contra enemigos jurados (Guardias Cazadores fuertemente armados con ballestas o Vampiros guerreros con colmillos y armadura oscura).
* **Filtro de Interacción / Reputación**:
  * Si el jugador pertenece a una facción enemiga o tiene reputación negativa, los guardias lo atacan inmediatamente al cruzar el perímetro.
  * Si el jugador es aliado o neutral, los aldeanos ofrecen intercambios comerciales especializados (armas anti-vampiros, sangre embotellada, mejoras de facción).

### 1.4 Incursiones y Captura de Aldeas (`Village Capture Raids`)
* **Disparador**: Puede iniciarse porque un jugador de facción rival hace clic derecho en el tótem de la aldea para desafiar el control, o por eventos automáticos (como entrar con el efecto *Bad Omen* o patrullas hostiles).
* **Fases del Asedio**:
  1. *Fase de Convocatoria (Gathering)*: Los defensores del tótem se reúnen y las tropas atacantes se agrupan en las afueras de la aldea. Se emite una alerta y una barra de jefe (BossBar) aparece en la pantalla de los jugadores en la zona.
  2. *Fase de Combate (Combat Wave)*: Los atacantes avanzan hacia el centro de la aldea intentando neutralizar a los defensores y profanar el tótem.
  3. *Resolución*:
     * Si los atacantes ganan: El tótem es reclamado por la nueva facción, los aldeanos se convierten y la aldea cambia de bando.
     * Si los defensores ganan: El asedio fracasa, se refuerzan las defensas y los defensores obtienen botines y reputación.

---

## 2. Errores Críticos Conocidos en Vampirism (Y Cómo Evitarlos)

Al revisar los problemas reportados en el repositorio de TeamLapen (`TeamLapen/Vampirism`), destacan varios fallos recurrentes relacionados con las aldeas que **debemos prever y solucionar** en nuestra arquitectura:

| Problema en Vampirism | Causa Raíz Identificada | Solución para Seven Deadly Gods (NeoForge 26.3) |
| :--- | :--- | :--- |
| **Generación de múltiples tótems duplicados** (Issues #1572, #1612 con *Lithostitched* / *CTOV*) | Los tótems se inyectaron en pools de casas repetitivas en vez de en el pool central (`town_centers`), provocando que cada casa generase un tótem adicional. | **Inyectar únicamente en `town_centers` con peso controlado (`weight = 1`)** o verificar en el `BlockEntity` del Altar si ya existe otro altar registrado en la misma aldea mediante `GodVillageSavedData`. |
| **Cuelgues y tirones de servidor (Tick Lag)** | El `TotemBlockEntity` realizaba búsquedas lineales de entidades en radios enormes cada tick (`level.getEntitiesOfClass`). | **Optimizar el tick del Altar**: verificar estado cada 40-100 ticks (2-5 segundos) y delegar el targeting a los `TargetGoal` nativos de los `GodAcolyteEntity` (usando el sistema de sensores de IA). |
| **Pérdida de datos de la aldea al descargar chunks** | La lógica de asedio residía únicamente en memoria dentro del `BlockEntity` del chunk; al descargarse el chunk central, el asedio se rompía. | **Desacoplar la lógica de asedio del bloque**: Toda la gestión de asedios y aldeas se almacena en un gestor global persistente a nivel de dimensión (`GodVillageSavedData` / `ServerLevel.getDataStorage()`). |
| **Aldeanos atrapados en loops de pánico** | Incompatibilidad entre el `Brain` (comportamiento vanilla 1.14+) y las tareas de huida de facción añadidas por inyección de goals. | **Utilizar acólitos independientes (`GodAcolyteEntity`) para la defensa y combate**, y mantener a los aldeanos vanilla con profesiones y trades de devoción o usando el sistema de eventos de reputación nativo. |

---

## 3. Matriz Comparativa: Vampirism vs Seven Deadly Gods

```
+-----------------------------------+------------------------------------+---------------------------------------+
| Característica                    | Vampirism (TeamLapen)              | Seven Deadly Gods (NeoForge 26.3)     |
+-----------------------------------+------------------------------------+---------------------------------------+
| Licencia                          | Multilicencia: LGPLv3 / VNCL /     | MIT License (Permisiva)               |
|                                   | Vampirism Asset License            | (100% Clean-Room Implementation)      |
+-----------------------------------+------------------------------------+---------------------------------------+
| Facciones                         | 2 principales (Vampires / Hunters) | 7 Dioses (Pecados Capitales)          |
|                                   | + Neutros                          | + Aldeas Vanilla Normales             |
+-----------------------------------+------------------------------------+---------------------------------------+
| Objeto/Bloque Sagrado Central     | Village Totem (Madera/Piedra)      | Altar del Pecado (Templo Sagrado)     |
+-----------------------------------+------------------------------------+---------------------------------------+
| Probabilidad de Aldea de Culto    | Configurable (~30-50%)             | Exactamente 20% (1 de cada 5 aldeas)  |
+-----------------------------------+------------------------------------+---------------------------------------+
| Umbrales de Relación con Jugador  | Rango de facción (Niveles 1-14)    | Favor Escalar:                        |
|                                   | Aliado / Hostil binario            | <= -40: Ataque inmediato              |
|                                   |                                    | >= 0: Comercio Divino habilitado      |
+-----------------------------------+------------------------------------+---------------------------------------+
| Eventos de Asedio                 | Faction Raids (Oleadas humanas)    | Guerras Santas entre Cultos Rivales   |
+-----------------------------------+------------------------------------+---------------------------------------+
| Clímax de Invasión                | Captura del tótem por guardias     | 5% Probabilidad de Manifestación      |
|                                   |                                    | del Avatar Divino colosal (Boss)      |
+-----------------------------------+------------------------------------+---------------------------------------+
| Notificación de Eventos           | Chat local y barra de jefe         | Transmisión global de chat con color  |
|                                   |                                    | y estilo único de cada Dios           |
+-----------------------------------+------------------------------------+---------------------------------------+
| Inteligencia & Diálogos           | Mensajes estáticos de texto        | Diálogos adaptativos con IA (Gemini)  |
|                                   |                                    | + Motor de respaldo temático local    |
+-----------------------------------+------------------------------------+---------------------------------------+
```

---

## 5. Marco Legal y Licencias

Para una revisión detallada sobre qué elementos de Vampirism se pueden o no utilizar, incompatibilidades con la licencia **MIT** de este mod y metodología *Clean-Room*, consulta el documento dedicado:
* 📄 **[LEGAL_AND_LICENSING_ANALYSIS.md](file:///mnt/c/Users/VolcoV/Desktop/backup/Mods%20de%20Minecraft/Minecraft%20Gods/docs/feat_aldeas_devotas/LEGAL_AND_LICENSING_ANALYSIS.md)**: Desglose de LGPLv3, VNCL, Vampirism Asset License y condiciones de uso.


Revisando las APIs de NeoForge 26.3 y la arquitectura de aldeas de Minecraft (POIs, Brain System, Jigsaw y Data Attachments), surgen estas mecánicas de alto valor:

### 4.1 La Campana Consagrada (`Bell of Consecration`)
En vanilla, tocar la campana de una aldea (`minecraft:bell`) hace que los aldeanos corran a sus casas y aplica el efecto `Glowing` a los invasores de raids.
* **Mecánica de Dioses**: Al tocar la campana de un templo consagrado:
  * Emite un acorde sagrado con instrumentos acordes al dios (gong de oro para Mammon, campana fúnebre para Khorvath, trompeta triunfal para Lucian).
  * Los jugadores con `Favor >= 0` reciben bendiciones temporales (Fuerza, Haste, Saturación según el dios).
  * Los jugadores con `Favor <= -40` reciben debuffs inmediatos (`Glowing`, `Slowness`, o rayos visuales de advertencia).

### 4.2 Cofre de Diezmos y Prosperidad Comunal
* En el interior del templo consagrado, junto al Altar, se genera un **Cofre de Diezmos**.
* Los aldeanos del pueblo depositan periódicamente pequeñas ofrendas (lingotes de oro para Mammon, comida para Beelze, flores/esmeraldas para Asmodeus).
* Un jugador fiel (`Favor >= +50`) puede rezar en el altar para recibir una fracción de estos tributos en forma de reliquias divinas o bendiciones.
* Si un jugador hereje saquea el cofre, el dios se enfurece inmediatamente perdiendo `-15` de favor por acto de profanación.

### 4.3 Profanación y Reconversión del Templo (Guerra de Creencias)
* Al igual que en Vampirism se puede reclamar el tótem:
  * Si un jugador con alto favor con otro dios (ej: Khorvath) derrota a todos los acólitos guardianes de un templo de Mammon, puede realizar un **Ritual de Profanación**:
    1. Romper o purificar el altar enemigo.
    2. Colocar el altar de su propio dios y realizar una ofrenda de sangre o riquezas.
    3. ¡La aldea se convierte formalmente al nuevo dios! Los aldeanos cambian sus túnicas y el chat anuncia la caída del culto anterior.

### 4.4 Integración con Gemini AI en los Asedios
* Si el 5% de probabilidad se activa y un **Avatar** se manifiesta durante la invasión:
  * El servicio de Gemini genera dinámicamente un grito de guerra contextualizado:
    * *Ejemplo*: Mammon defiende la aldea: *"¡Ratas sarnosas de Khorvath! ¿Creéis que podéis saquear mis arcas sin pagar con vuestras vidas?"*
    * *Ejemplo*: Khorvath asalta la aldea: *"¡Que vuestros templos ardan y vuestros falsos dioses lloren lágrimas de sangre!"*
  * Esto crea momentos cinematográficos inigualables que ningún otro mod de Minecraft posee.
