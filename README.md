# ✦ Seven Deadly Gods (Minecraft 26.3 / NeoForge)

**Seven Deadly Gods** es un mod para **Minecraft 26.3 (NeoForge)** que encarna a los **7 Pecados Capitales** como Dioses conscientes impulsados por Inteligencia Artificial (**Google Gemini**, con motor temático local de respaldo). Los dioses observan tus actos en tiempo real, escuchan lo que dices en el chat, juzgan tus rituales, envían siervos y acólitos alados, y se manifiestan físicamente en el mundo como Avatares colosales.

---

## 📋 Requisitos Previos

* **Minecraft**: `26.3`
* **Mod Loader**: `NeoForge 26.3.0.99+`
* **Java**: `JDK 25`
* **API Key de Google Gemini** *(Opcional pero recomendada)*: Permite que los dioses respondan dinámicamente al contexto de tu partida y conversen contigo en tiempo real. Si no se configura o no hay conexión, el mod utiliza automáticamente su motor de respuestas temáticas integrado (`enableLocalFallback = true`).

---

## ⚙️ Instalación y Configuración de IA (Gemini)

### 1. Compilar o Ejecutar el Mod
Desde la raíz del proyecto puedes compilar el archivo `.jar` o lanzar un cliente de prueba:

```bash
# Compilar el mod (el .jar se generará en build/libs/)
./gradlew build

# Ejecutar el cliente de Minecraft directamente desde el entorno de desarrollo
./gradlew runClient
```

### 2. Configurar la Clave de API de Google Gemini
Tienes dos formas de proporcionar tu API Key de [Google AI Studio](https://aistudio.google.com/):

1. **Mediante variable de entorno** (en tu sistema operativo):
   ```bash
   export GEMINI_API_KEY="tu_clave_aqui"
   # (También soporta GOOGLE_API_KEY)
   ```
2. **Mediante el archivo de configuración del mod**:
   Tras iniciar el juego una vez, edita el archivo `config/sevendeadlygods-common.toml`:
   ```toml
   [gemini_ai]
       # Clave de API de Google Gemini (AI Studio).
       geminiApiKey = "TU_API_KEY_AQUI"
       # Modelo de Gemini a utilizar
       geminiModel = "gemini-3.8-flash"
       # Tiempo mínimo en segundos entre intervenciones espontáneas en el chat
       chatEvaluationCooldownSeconds = 8
       # Motor de respuestas temáticas local si no hay API Key o falla la red
       enableLocalFallback = true

   [world]
       # Probabilidad (0.0 a 1.0) de encontrar libros sagrados de lore en cofres de estructuras
       loreBookChestChance = 0.35
   ```

---

## 🏛️ Los 7 Dioses del Pecado: Comportamiento, Altares y Rituales

Cada dios observa acciones específicas de tu día a día y posee su propio **Altar de Ritual** con mecánicas asimétricas.

| Pecado | Dios | Qué le complace (+Favor) | Qué le enfurece (-Favor) |
| :--- | :--- | :--- | :--- |
| **Avaricia** (`greed`) | **Mammon** | Tributar minerales y joyas en su altar (en 5 niveles de valor hasta el Bloque de Netherite); hablar de riquezas y pactos. | Portar armaduras valiosas (oro, diamante, netherite) o acumular joyas/bloques valiosos en el inventario más de 1 día sin pagarle diezmo en su altar. |
| **Ira** (`wrath`) | **Khorvath** | Matar monstruos (especialmente con poca vida: ≤ 40% HP), sacrificios de sangre o **derrotar al Avatar de otro dios** (+15 base). | Usar escudos para bloquear ataques, dormir en camas para evitar la noche o pasar más de 1 día sin matar nada. |
| **Gula** (`gluttony`) | **Beelze** | Degustar buenos manjares (manzanas doradas, carnes asadas, estofados, zanahorias doradas). | Pasar hambre (barra de comida ≤ 6 muslos) o comer comida podrida/venenosa (carne podrida, ojo de araña, patata venenosa). |
| **Soberbia** (`pride`) | **Lucian** | Abatir enemigos poderosos (≥ 40 HP) en solitario, **derrotar al Avatar de otro dios** (+20 base), arrodillarse y alabarle en las cumbres. | Morir de formas humillantes (caídas, ahogamiento) o interactuar con su altar sin agacharse (`Shift`). |
| **Envidia** (`envy`) | **Leviatán** | Matar aldeanos, abatir siervos o **Avatares de otros dioses** (+15 base), sacrificar botín saqueado o caer en desgracia con otros dioses. | Comerciar pacíficamente con aldeanos o **superar +20 de favor con cualquier otro dios** (despierta sus celos). |
| **Pereza** (`sloth`) | **Belphegor** | Dormir en una cama (además reinicia tu fatiga laboral), caminar sin prisa o permanecer completamente inmóvil durante 30 segundos. | Esprintar continuamente o **trabajar en exceso** (cada 100 bloques rotos o colocados sin haber dormido tienes probabilidad creciente de perder favor). |
| **Lujuria** (`lust`) | **Asmodeus** | Reproducir animales, vestir armaduras decoradas con diseños (*Armor Trims*), rodearse de flores y música. | Romper flores, matar crías de animales o asesinar aldeanos. |

---

## 🔨 Crafteo de Altares y Cómo Realizar cada Ritual

Todos los altares comparten una base en forma de **T** hecha con 4 bloques de **Ladrillos de Piedra (`stone_bricks`)** en las filas central e inferior, cambiando únicamente la fila superior:

```text
[ Ingrediente Izq ] [ Ingrediente Central ] [ Ingrediente Der ]
[     Vacío       ] [ Ladrillos de Piedra ] [     Vacío       ]
[ Ladrillos Piedra] [ Ladrillos de Piedra ] [ Ladrillos Piedra]
```

### 1. 🪙 Altar de la Avaricia (Mammon)
* **Fila superior del crafteo**: `Lingote de Oro` + `Esmeralda` + `Lingote de Oro`
* **Cómo hacer el ritual**: Haz clic derecho sobre el altar con minerales o bloques preciosos refinados. Cada ofrenda está estrictamente balanceada para respetar las recetas de crafteo e incentivar juntar bloques compactados mediante un **bono del ~5%**:
  1. **Supremo (`26.0` esfuerzo)**: `Bloque de Netherite` (9 lingotes + ~5% bono de bloque).
  2. **Gemas y Metales Compactados**:
     * `Bloque de Diamante` (**`7.60`** esfuerzo, 9 diamantes + ~5% bono).
     * `Lingote de Netherite` (**`2.75`** esfuerzo, 4 fragmentos + 4 lingotes de oro + ~5% bono de forja).
     * `Bloque de Oro` (**`0.95`** esfuerzo, 9 lingotes de oro + ~5% bono).
     * `Bloque de Amatista` (**`0.85`** esfuerzo, 4 fragmentos + ~5% bono).
     * `Bloque de Esmeralda` (**`0.48`** esfuerzo, 9 esmeraldas + ~5% bono; tasado por debajo del bloque de oro).
  3. **Unidades Sueltas**:
     * `Diamante` (**`0.80`** esfuerzo, gema noble de alta rareza).
     * `Fragmento de Netherite` (**`0.55`** esfuerzo, residuo ancestral).
     * `Fragmento de Amatista` (**`0.20`** esfuerzo, gema de geoda tasada por encima del oro común).
     * `Lingote de Oro` (**`0.10`** esfuerzo, unidad áurea elemental refinada).
     * `Esmeralda` (**`0.05`** esfuerzo, tasada por debajo del oro debido a su abundancia y farmeo masivo con aldeanos).
  * ⚠️ **Rechazo al Oro en Bruto**: Mammon considera una ofensa el mineral sucio sin fundir (`Oro en Bruto` o `Bloque de Oro en Bruto`) y lo rechazará exigiéndote que lo fundas primero.
  * Además, cualquier ofrenda válida reinicia el contador de diezmo para que Mammon no te castigue por vestir armaduras caras o portar riquezas.

### 2. 🩸 Altar de la Ira (Khorvath)
* **Fila superior del crafteo**: `Redstone` + `Espada de Hierro` + `Redstone`
* **Cómo hacer el ritual** *(2 vías)*:
  1. **Pacto de Sangre**: Haz clic derecho sobre el altar teniendo más de 4.5 corazones (`> 8.5 HP`). El altar consumirá `8 HP` (4 corazones) de tu propia sangre.
  2. **Ritual de Carnicería**: Mata a **3 criaturas** a menos de 7 bloques del altar.

### 3. 🍖 Altar de la Gula (Beelze)
* **Fila superior del crafteo**: `Manzana` + `Cuenco (Bowl)` + `Manzana`
* **Cómo hacer el ritual** *(2 vías)*:
  1. Haz clic derecho sobre el altar entregando **3 platos selectos** (`Manzana dorada`, `Pastel`, `Tarta de calabaza`, `Filete cocinado`, `Chuleta cocinada`, `Zanahoria dorada`, `Botella de miel` o `Pan`).
  2. O bien **devora 3 manjares** tú mismo a menos de 6 bloques del altar.

### 4. ☀️ Altar de la Soberbia (Lucian)
* **Fila superior del crafteo**: `Lingote de Oro` + `Cristal (Glass)` + `Lingote de Oro`
* **Cómo hacer el ritual**:
  1. Coloca el altar en las alturas (**altura `Y > 120`**) y **bajo el cielo abierto** (sin bloques encima).
  2. **Agáchate (`Shift`)** frente al altar y haz clic derecho entregando un trofeo u ofrenda digna (`Lingote de oro`, `Diamante`, `Manzana dorada`, `Estrella del Nether`, `Aliento de dragón` o `Fragmento de amatista`).
  3. **Sin dejar de agacharte**, escribe en el chat una alabanza dedicada a Lucian a menos de 6 bloques del altar.

### 5. 🌊 Altar de la Envidia (Leviatán)
* **Fila superior del crafteo**: `Lapislázuli` + `Cristal (Glass)` + `Lapislázuli`
* **Cómo hacer el ritual**: Haz clic derecho sobre el altar para sacrificar y destruir objetos de valor saqueados de aldeas o estructuras (`Esmeralda`, `Libro encantado`, `Montura`, `Etiqueta de nombre`, `Manzana dorada`, `Tótem de la inmortalidad`, `Campana`, `Frasco con experiencia` o `Perla de Ender`).

### 6. 💤 Altar de la Pereza (Belphegor)
* **Fila superior del crafteo**: `Lana Blanca` + `Cama Blanca` + `Lana Blanca`
* **Cómo hacer el ritual**:
  1. Haz clic derecho sobre el altar entregando `Reloj`, `Lana blanca`, `Perla de Ender`, `Bacalao crudo`, `Salmón crudo` o `Pluma`.
  2. Permanece **completamente inmóvil** (sin caminar, esprintar ni golpear) a menos de 5 bloques del altar durante **30 segundos**.

### 7. 💜 Altar de la Lujuria (Asmodeus)
* **Fila superior del crafteo**: `Amapola (Poppy)` + `Fragmento de Amatista` + `Amapola (Poppy)`
* **Cómo hacer el ritual**:
  1. Decora el entorno colocando al menos **3 bloques de belleza/música** en un radio de 4 bloques alrededor del altar (`Flores`, `Bloques de Amatista`, `Geodas de Amatista`, `Tocadiscos` o `Bloques Musicales`).
  2. Haz clic derecho entregando `Fragmento de amatista`, `Rosal`, `Flor de esporas`, `Manzana dorada`, `Panal` o `Corazón del mar`; **o bien reproduce animales** a menos de 7 bloques del altar florido.

---

## ⚖️ Escala de Favor Divino (-100 a +100 — Condiciones Acumulativas)

El favor con cada dios varía **gradualmente de 1 en 1** según tus actos, rituales y conversaciones, mostrándote notificaciones en la barra de acción. **Todos los umbrales son acumulativos**: al alcanzar un nivel superior (o más severo en negativo), conservas también todos los efectos y condiciones de los umbrales anteriores.

### ⏳ Intervenciones Periódicas de los Dioses (`favor >= 30` o `favor <= -30`)

Cada intervalo aleatorio de **entre medio día y 2 días de Minecraft** (12.000 a 48.000 ticks = 10 a 40 minutos reales), el juego reúne a todos los dioses con los que mantengas un **favor de 30 o más (`>= +30`)** o un **favor de -30 o menos (`<= -30`)**. De esa lista de dioses elegibles, **un dios es elegido al azar** para interactuar con el jugador:

* **Acciones Hostiles de Ira (`favor <= -30`)**:
  * **`-30 a -59` (Castigos Temáticos del Pecado)**:
    * **Mammon (Avaricia)**: Impone *Fatiga minera II* y *Hambre*, recordándote con frialdad que no has pagado tu diezmo.
    * **Khorvath (Ira)**: Fulmina con rayo celestial (o descarga de fuego y explosión bajo techo), *Debilidad II* y *Lentitud*.
    * **Beelze (Gula)**: Drena instantáneamente la comida y saturación a niveles de inanición, aplicando *Hambre III* y *Debilidad*.
    * **Lucian (Soberbia)**: Lanza al jugador por los aires con *Levitación II* para luego cegarlo (*Ceguera*), humillando su insolencia.
    * **Leviatán (Envidia)**: Oscurece el mundo con *Oscuridad*, *Ceguera* y *Veneno* de las profundidades abisales.
    * **Belphegor (Pereza)**: Inmoviliza al jugador con *Lentitud III* extrema y *Fatiga minera II*, forzándole un letargo eterno.
    * **Asmodeus (Lujuria)**: Castiga con *Oscuridad*, *Debilidad II* y *Wither*, consumiéndolo en la pasión marchita.
  * **`<= -60`**: Además de castigos temáticos, tiene probabilidad de enviar a un **Enviado del Dios** (esqueleto ignífugo de combate cuerpo a cuerpo).
  * **`<= -80`**: Probabilidad de enviar un escuadrón de **Acólitos alados hostiles** para ejecutarte.
  * **`-100`**: Descenso del **Avatar del Dios en modo Caza**.

* **Acciones Benévolas de Gracia (`favor >= 30`)**:
  * **`+30 a +59`**:
    * **Suministros Celestiales**: El dios hace llover recursos útiles a tus pies (oro/esmeraldas para Mammon, flechas y combate para Khorvath, alimentos exquisitos para Beelze, botellas de experiencia y cohetes para Lucian, perlas y prismarina para Leviatán, miel y bayas para Belphegor, flores y melón reluciente para Asmodeus).
    * **Rejuvenecimiento Divino**: Curación instantánea de salud, restauración completa de comida y efectos de *Regeneración* y *Absorción*.
  * **`+60 a +89`**: Otorga la **Bendición Mayor de Ritual** del dios durante **5 minutos** con *Regeneración II* o cargamentos mayores de gemas y reliquias.
  * **`+90 a +100`**: Desata la **Plenitud de la Bendición Mayor** durante **10 minutos** con curación total y saciedad, u ofrendas supremas (*Bloques de Diamante*, *Manzanas Doradas Encantadas*, *Fragmentos de Netherite*, *Totems de la Inmortalidad*, *Corazón del Mar*).

---

### 🔥 Escala Negativa Acumulativa (Ira Divina)
* **`-20 o menos` (`≤ -20`, Disgustado)**: Ocasionalmente el dios te lanza un rayo (si estás bajo el cielo) o te aplica una maldición temática (*Fatiga minera*, *Debilidad*, *Hambre*, *Levitación*, *Ceguera*, *Lentitud* o *Oscuridad*).
* **`-60 o menos` (`≤ -60`, Cazado por Enviados)**: *(Acumula `-20`)* Además de los rayos y maldiciones, cada 2 a 4 días de Minecraft aparece un **Enviado del Dios** (esqueleto veloz e ignífugo equipado con armadura encantada y arma cuerpo a cuerpo) para darte caza.
* **`-80 o menos` (`≤ -80`, Perseguido por Acólitos)**: *(Acumula `-20` y `-60`)* Además de lo anterior, cada 2 a 5 días desciende un escuadrón de **Acólitos alados** con capucha y armas encantadas para ejecutarte.
* **`-100` (Condenado a Muerte — Caza del Avatar)**: *(Acumula `-20`, `-60` y `-80`)* El **Avatar del Dios** (500 HP, escala 1.5x, 80% de reducción de daño y equipo sobre-encantado) desciende en persona con barra de jefe para matarte. Si logra ejecutarte, su ira se sacia parcialmente y tu favor vuelve a `-85`.

### ✨ Escala Positiva Acumulativa (Gracia Divina)
* **`20+` (`≥ +20`, Favorecido)**: Cuando tienes poca vida (`< 65%`) o hambre, el dios puede intervenir espontáneamente otorgándote *Regeneración II* o restaurando tu comida y *Saturación*.
* **`61+` (`≥ +61`, Discípulo de Ritual)**: *(Acumula `20+`)* Además de la asistencia espontánea, cada vez que completas un ritual en el altar de ese dios recibes su **Bendición Mayor de Ritual** durante **20 a 40 minutos**:
  * **Mammon (Avaricia)**: *Toque de Midas* — Prisa Minera II, Suerte III y 35% de probabilidad de obtener pepitas de oro extra al picar menas.
  * **Khorvath (Ira)**: *Frenesí de Khorvath* — Fuerza III, Velocidad II y robo de vida al golpear enemigos.
  * **Beelze (Gula)**: *Festín de Beelze* — Nutrición automática constante, 4 corazones de Absorción regenerativa e inmunidad a Hambre y Veneno.
  * **Lucian (Soberbia)**: *Majestad Imperial* — Resistencia II y aura que aplica Debilidad a todos los monstruos en un radio de 6 bloques.
  * **Leviatán (Envidia)**: *Mirada Abisal* — Visión Nocturna, Invisibilidad al agacharte (`Shift`) y daño mágico adicional contra objetivos con armadura o efectos activos.
  * **Belphegor (Pereza)**: *Aura de Letargo* — Regeneración constante mientras no esprintes y Lentitud III a los enemigos cercanos que te apunten.
  * **Asmodeus (Lujuria)**: *Encanto de Asmodeus* — Regeneración II y Héroe de la Aldea II constante.
* **`85+` (`≥ +85`, Elegido — Protegido por Acólitos)**: *(Acumula `20+` y `61+`)* Si tu salud baja del `35%` en combate o si te ataca el siervo/avatar de un dios rival (*Guerra de Dioses*), el dios envía un par de **Acólitos Devotos** alados para defenderte.
* **`91+` (`≥ +91`, Campeón Divino)**: *(Acumula `20+`, `61+` y `85+`)*
  * **Salvación Milagrosa de la Muerte**: Si recibes un golpe mortal, tu dios cancela tu muerte (efecto de Tótem), restaura el 60% de tu salud y te otorga Regeneración III, Absorción IV y Resistencia al Fuego (enfriamiento de 1 día de Minecraft).
* **`+100` (Cúspide de la Devoción — Entrega de Reliquia Divina)**: *(Acumula todas las anteriores)*
  * **Manifestación Benévola del Avatar**: Al alcanzar el favor máximo (+100) y realizar una ofrenda en su Altar (o al invocarlo por chat), el **Avatar del Dios** desciende pacíficamente durante 4 minutos para entregarte por primera y única vez su **Reliquia Divina sobre-encantada** (*Filo VIII, Botín VI, Aspecto Ígneo IV, Irrompibilidad X, Reparación*).
  * **Límite de 1 Reliquia por Jugador**: El jugador recibe la tag permanente `has_relic_<god>` (ej. `has_relic_greed`) y `has_god_relic`. No se le volverá a entregar otro artefacto duplicado.
  * **Ofrendas Posteriores en el Altar**: Tras haber recibido la reliquia, realizar nuevas ofrendas en el Altar **no** vuelve a invocar al dios; en su lugar, el altar renueva de forma limpia tu **Bendición Mayor de Ritual** (20 a 40 minutos) y el dios agradece tu lealtad en el chat.
  * **Invocación Verbal Directa por Chat (+100 Favor)**: Puedes llamar al Avatar del dios para conversar cara a cara diciendo en el chat (*"¡Mammon, ven a mí!", "¡Lucian, manifiéstate!", "¡Te invoco!"*) con un enfriamiento de 1 día de Minecraft (24.000 ticks = 20 minutos reales).

---

## 💬 Conversación Activa con los Dioses y Libros de Lore

* **Chat Inteligente con Reacciones Físicas Activas**: Escribe normalmente en el chat de Minecraft. Si estás cerca de un altar, frente a un Avatar manifestado o mencionas palabras clave del dominio de algún pecado (oro, sangre, comida, orgullo, robo, sueño, belleza, o el nombre del propio dios), el dios evaluará tus palabras, tu estado actual y te responderá en el chat.
  * 🎲 **Alteración Probabilística del Favor (Sentido y Progreso)**: Hablar en el chat no garantiza automáticamente sumar o restar favor en cada mensaje; se resuelve mediante una **tirada probabilística** que depende de:
    * **Progreso (|Favor| actual)**: Cuanto más cerca estés de los extremos (`-100` o `+100`), **más difícil es avanzar**. Cerca de 0 la probabilidad es alta (~85%), en tramos medios ronda el 50%-70%, y en los extremos superiores (`86 a 100`) cae drásticamente (al 5%-15%), impidiendo farmear favor máximo sólo spameando texto en el chat.
    * **Sentido del movimiento**: Escalar hacia los extremos es exigente, pero **caer de la cima por insultar al dios es sumamente probable (70%-80%)**, ya que los dioses son celosos y no toleran traiciones de sus devotos. Redimirse desde el fondo hacia 0 requiere persistencia (35%-65%).
    * **Intensidad del mensaje**: Plegarias fervientes o blasfemias explícitas tienen mayor impacto que frases ambiguas.
  * ⚡ **Castigo por Blasfemia o Insultos (`LIGHTNING`)**: Si insultas o desafías abiertamente a un dios, te fulminará de inmediato con un rayo divino (o descarga mágica si estás bajo techo) y te restará favor (-1.0).
  * 🔮 **Maldición Temática por Desprecio (`CURSE`)**: Si muestras falta de respeto o arrogancia hacia su pecado, te impondrá en el acto una aflicción temática (fatiga minera para Mammon, ignición y debilidad para Khorvath, hambre para Beelze, levitación humillante para Lucian, ceguera para Leviatán, lentitud para Belphegor, oscuridad para Asmodeus).
  * 💨 **Repulsión Sagrada frente a Altares/Avatares (`REPULSION`)**: Si eres insolente o blasfemas estando junto al Altar o Avatar de un dios, una onda expansiva te arrojará violentamente por los aires con daño de impacto.
  * ✨ **Bendición Instantánea por Súplica o Devoción (`BLESSING`)**: Si suplicas piedad con humildad, pides perdón o proclamas alabanzas sinceras, el dios te bendecirá de inmediato con curación, regeneración y comida.
  * 👑 **Invocación Verbal del Avatar en el Chat (`SUMMON_AVATAR`)**: Al tener **Favor +100** con un dios, llamarlo expresamente por su nombre en el chat (*"¡Ven a mí, Khorvath!", "¡Lucian, manifiéstate ante tu Campeón!", "¡Te invoco!"*) hace que su Avatar colosal descienda pacíficamente ante ti en el acto. Cuenta con un enfriamiento persistente de **1 día de Minecraft** (24,000 ticks = 20 minutos reales). Si intentas llamarlo antes de tiempo o sin tener +100, el dios te responderá indicándote los minutos restantes o exigiéndote mayor devoción.
* **Libros Sagrados de Lore**: Al explorar estructuras del mundo (mazmorras, templos, aldeas, bastiones, etc.), tienes un `35%` de probabilidad de encontrar **Libros Sagrados** en los cofres con relatos y pistas sobre cómo complacer a cada uno de los 7 dioses.

---

## 💻 Comandos del Mod

* `/gods favor` *(Disponible para todos los jugadores)*
  Muestra tu puntuación actual de favor (`-100` a `+100`) y tu rango con cada uno de los 7 Dioses.
* `/gods next` *(Disponible para todos los jugadores)*
  Muestra el temporizador de la próxima intervención divina en tu personaje, el rango de intervalo asignado y la lista de dioses elegibles para actuar.
* `/gods setinterval <min_ticks> <max_ticks>` o `/gods setinterval <jugador> <min_ticks> <max_ticks>` *(Requiere permisos de Operador / Nivel 2)*
  Reprograma los límites mínimo y máximo de tiempo (en ticks de Minecraft, 20 ticks = 1 segundo) entre intervenciones para tu propio personaje o para un jugador específico en multijugador, reprogramando inmediatamente su próxima tirada.
* `/gods setfavor <dios> <valor>` *(Requiere permisos de Operador / Nivel 2)*
  Ajusta directamente tu favor con un dios entre `-100` y `100`. Puedes usar su ID en inglés (`greed`, `wrath`, `gluttony`, `pride`, `envy`, `sloth`, `lust`), su nombre (`mammon`, `khorvath`, `beelze`, `lucian`, `leviatan`, `belphegor`, `asmodeus`) o el pecado en español.
* `/gods manifest <dios>` *(Requiere permisos de Operador / Nivel 2)*
  Fuerza la manifestación inmediata del Avatar del dios indicado junto a ti (en modo Caza si tu favor es `≤ -100`, o en modo Benévolo en caso contrario).
* `/gods lorebook <dios>` *(Requiere permisos de Operador / Nivel 2)*
  Te entrega un Libro Sagrado aleatorio del dios especificado.
* `/gods action <dios>` *(Requiere permisos de Operador / Nivel 2)*
  Fuerza la ejecución inmediata de la acción divina periódica del dios especificado sobre el jugador (requiere que el favor con ese dios sea `≥ 30` o `≤ -30`).
* `/gods trigger_periodic` *(Requiere permisos de Operador / Nivel 2)*
  Fuerza la evaluación del ciclo periódico: selecciona un dios al azar de la lista de dioses elegibles (`favor >= 30` o `favor <= -30`) y desata su acción.
