# ⚖️ Escala de Favor e Ira Divina (-100 a +100)

En **Seven Deadly Gods**, tu relación con cada uno de los 7 Pecados Capitales se mide en una escala que va desde **-100 (Condenado a Muerte)** hasta **+100 (Cúspide de la Devoción)**.

El marcador es individual para cada dios y se calcula de forma gradual (**sube o baja de 1 en 1 punto entero de favor** mediante esfuerzo amortiguado, salvo eventos de Alta Traición o decretos divinos).

---

## 📈 Amortiguación del Esfuerzo (*Damped Effort*)

Para evitar que el favor se dispare en minutos realizando acciones repetitivas, el mod implementa una **resistencia al progreso**:
* Cuanto más cerca te encuentres de los extremos (`-100` o `+100`), mayor esfuerzo de acciones o tributos se requiere para mover el marcador 1 punto.
* En rangos neutrales (`-20 a +20`), los dioses prestan atención rápidamente; en rangos avanzados (`+85 a +100`), solo ofrendas de enorme valor o hazañas de combate consiguen aumentar la devoción.

---

## ✨ Umbrales Positivos Acumulativos (Gracia y Devoción)

> [!IMPORTANT]
> **Todos los umbrales son acumulativos**: al alcanzar un rango superior, mantienes permanentemente todos los beneficios de los rangos anteriores.

| Rango de Favor | Título del Devoto | Beneficios y Efectos Acumulados |
| :---: | :---: | :--- |
| **`+20` a `+60`** | **Favorecido** | Los dioses te observan con simpatía. Cuando tu salud cae por debajo del 65% o sufres hambre extrema, el dios puede intervenir espontáneamente otorgándote *Regeneración II* o restaurando tu comida y saturación. |
| **`+61` a `+84`** | **Discípulo de Ritual** | *(Acumula `+20`)* Cada vez que completas un ritual en el Altar de ese dios, recibes su **Bendición Mayor de Ritual** durante **20 a 40 minutos** (efectos como *Toque de Midas*, *Frenesí de Khorvath*, etc.). |
| **`+85` a `+90`** | **Elegido** | *(Acumula `+20` y `+61`)* Si tu salud baja del 35% en combate o si te ataca el siervo de un dios rival (*Guerra de Dioses*), tu dios despacha **2 Acólitos Devotos alados** para luchar a tu lado. |
| **`+91` a `+99`** | **Campeón Divino** | *(Acumula `+20`, `+61` y `+85`)* **Salvación Milagrosa de la Muerte**: si recibes un golpe letal, tu dios cancela tu muerte (efecto Tótem), restaura el 60% de tu salud y te otorga *Regeneración III*, *Absorción IV* y *Resistencia al Fuego* (cooldown de 1 día MC). |
| **`+100`** | **Cúspide de la Devoción** | *(Acumula todos los anteriores)* Al realizar una ofrenda en su altar (o invocarlo en el chat), el **Avatar del Dios** desciende pacíficamente para entregarte por única vez su **Reliquia Sagrada sobre-encantada** (límite de 1 reliquia por jugador). En ofrendas posteriores, renueva tu Bendición Mayor. |

---

## 🔥 Umbrales Negativos Acumulativos (Ira y Herejía)

| Rango de Favor | Título del Infractor | Consecuencias y Castigos Acumulados |
| :---: | :---: | :--- |
| **`-20` a `-59`** | **Disgustado** | Ocasionalmente cae un rayo divino junto a ti o sufres maldiciones temáticas (*Fatiga minera*, *Debilidad*, *Lentitud*, *Ceguera*, *Hambre* o *Levitación*). |
| **`-60` a `-79`** | **Cazado por Enviados** | *(Acumula `-20`)* Cada 2 a 4 días de Minecraft, un **Enviado del Dios** (esqueleto ignífugo de combate con armadura completa encantada) es despachado para darte caza. |
| **`-80` a `-99`** | **Marca del Hereje** | *(Acumula `-20` y `-60`)* Cada 2 a 5 días desciende un escuadrón de **Acólitos alados hostiles** encapuchados para ejecutarte. Además, las aldeas consagradas a ese dios te atacarán a la vista (`favor <= -40`). |
| **`-100`** | **Condenado a Muerte** | *(Acumula todos los anteriores)* El **Avatar colosal del Dios** desciende en persona con barra de jefe para cazarte y destruirte. Si logra ejecutarte, su ira se sacia parcialmente y tu favor regresa a `-85`. |

---

## ⏳ Intervenciones Periódicas (`|Favor| >= 30`)

Cada **10 a 40 minutos reales** (12.000 a 48.000 ticks = medio día a 2 días de Minecraft), el juego evalúa si tienes dioses con `favor >= 30` o `favor <= -30`:
* Si hay varios candidatos, **un dios es elegido al azar** para manifestar su gracia o su cólera.
* **Acciones Benévolas (`favor >= 30`)**: lluvia de suministros temáticos a tus pies, curación instantánea, bendición mayor de 5 o 10 minutos, o entrega de gemas supremas.
* **Acciones Punitivas (`favor <= -30`)**: aflicciones directas al pecado (vaciar la barra de hambre para Gula, lanzar al aire para Soberbia, etc.) o emboscadas de siervos armados.
