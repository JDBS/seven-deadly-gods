# 🏰 Facciones Aldeanas y Culturas Únicas

Aproximadamente el **80% de las aldeas generadas en el mundo** pertenecen a una de las facciones consagradas a los **7 Pecados Capitales** (dejando sólo el 20% como aldeas vanilla ordinarias).

---

## 🏗️ Generación de Estructuras Jigsaw

Mediante `VillageStructureInjector`, el mod inyecta dinámicamente los Santuarios Mayores en los `StructureTemplatePool` de los 5 biomas de aldeas vanilla (`plains`, `desert`, `savanna`, `taiga`, `snowy`):
* Se generan como piezas centrales de aldea con una probabilidad de consagración del ~80% (configurable mediante `villageConsecrationChance` en la sección `[world]`).
* Cada santuario incorpora el **Altar de Ritual** del dios correspondiente, estandartes heráldicos, iluminación temática y una guarnición de **Acólitos Centinelas permanentes**.
* El Altar Mayor es registrado en el sistema dimensional de persistencia (`GodVillageSavedData`) con un radio territorial de 48 bloques y protección contra altares duplicados en 128 bloques.
* **Bono de Devoción**: Cualquier ritual completado en el Altar Mayor de una aldea otorga un **+20% de esfuerzo devocional**.

---

## 🔄 Ciclo de Vida y Estados Territoriales

Cada aldea consagrada opera bajo una máquina de estados finitos (`GodVillageState`):

```text
       [Generación en el Mundo]
                  │
        ┌─────────┴─────────┐
        ▼                   ▼
   CONSAGRADA            ARRASADA (Leviatán)
   (Aldea Activa)        (Ruinas abisales)
        │
        ├── Enfriamiento de 3 días MC superado + Jugador cerca (80b)
        ▼
    EN_ASEDIO (Guerra Santa activa)
        │
        ├─ Victoria Defensora ──► Vuelve a CONSAGRADA
        └─ Victoria Atacante  ──► Altar reconvertido al nuevo dios
        │
        ▼ (Si mueren todos los aldeanos civiles)
    DESOLADA (Altar humea en vigilia)
        │
        ├─ Espera de 48.000 ticks (24.000 Lujuria)
        └─ O Milagro de Restauración con ofrenda
        ▼
   CONSAGRADA (Llega la Caravana de Peregrinos)
```

---

## 🎭 Personalidades Culturales y Sociales de los 7 Cultos

Cada culto transforma la vida cotidiana de los aldeanos y la aldea:

### 1. 🪙 Culto de Mammon (Avaricia)
* **Predilección Laboral**: Los aldeanos sin empleo se convierten prioritariamente en **Armeros (`ARMORER`)** y herreros.
* **Peaje de Puertas**: Las puertas de las casas no se abrirán ante ti a menos que sostengas al menos 1 Pepita, Lingote o Bloque de Oro en la mano (salvo Discípulos con `favor >= +60`).
* **Peaje de Bóvedas**: Abrir cualquier cofre o barril dentro de la aldea te cobrará automáticamente **1 Pepita de Oro** de tu inventario **únicamente tras una apertura exitosa del contenedor**. Si un cofre está obstruido o intentas colocar un bloque agachado, no se cobrará peaje; si no portas al menos una pepita al momento de abrir, el acceso será denegado y los aldeanos te rechazarán.
* **Ambiente**: Destellos áureos continuos sobre la plaza.

### 2. 🩸 Culto de Khorvath (Ira)
* **Guarnición Pesada**: Custodiada por una milicia reforzada de **6 Acólitos Centinelas Permanentes** fuertemente armados con espadas y hachas. Solo los centinelas asignados a este altar cuentan para la guarnición (los acólitos de escolta temporales no satisfacen este cupo).
* **Fosos de Arena**: La plaza incorpora fosos de combate con hogueras y antorchas de **fuego de alma**.

### 3. 🍖 Culto de Beelze (Gula)
* **Predilección Laboral**: Los aldeanos sin empleo se convierten en **Carniceros (`BUTCHER`)**.
* **Hornos Eternos**: Ahumadores y hornos siempre humeantes despidiendo humo constante de asados.
* **Aura de Festín**: Cualquier devoto de Beelze (`favor >= 0`) que permanezca en la aldea y tenga hambre verá su comida y saturación restauradas automáticamente sin consumir alimentos.

### 4. ☀️ Culto de Lucian (Soberbia)
* **Catedral de Mármol y Cuarzo**: Exenta por decreto divino del requisito de altitud (`Y > 120`).
* **Protocolo de Reverencia Imperial**: Los aldeanos se niegan a hablar contigo si no te **arrodillas (`Shift`)** ante ellos, salvo que seas un Discípulo consagrado (`favor >= +60`). **Al arrodillarte (`Shift`) ante un ciudadano elegible, este acepta tu reverencia y abre de inmediato su catálogo de comercio**, superando la omisión vanilla habitual durante el agachamiento.
* **Ambiente**: Columnas de luz y destellos solares sobre la nave central.

### 5. 🌊 Culto de Leviatán (Envidia)
* **Asentamiento Arrasado**: Las aldeas de Leviatán se generan directamente en estado **`ARRASADA`**: casas destruidas, telarañas, agua estancada y niebla de ceniza abisal.
* **Cripta Sumergida**: El Altar Mayor se ubica en una catacumba de prismarina bajo tierra.

### 6. 💤 Culto de Belphegor (Pereza)
* **Aldeanos 100% Holgazanes**: **Todos los aldeanos son convertidos en *Nitwits* (túnica verde)**. Duermen hasta tarde, deambulan sin rumbo y se niegan a trabajar.
* **Gólem Obrero**: La aldea cuenta con un Gólem de Hierro permanente que hace el trabajo pesado por ellos.
* **Aura de Descanso**: Los devotos de la Pereza (`favor >= 0`) reciben el efecto de **Regeneración constante** mientras no esprinten dentro de la aldea.

### 7. 💜 Culto de Asmodeus (Lujuria)
* **Jardines Floridos y Musicales**: La aldea está repleta de rosales, flores de cerezo y partículas de notas musicales y corazones.
* **Feromonas Animales**: El ambiente cargado de feromonas hace que los animales de granja en la aldea **se reproduzcan pasivamente de forma automática** sin necesidad de darles trigo o zanahorias.
* **Repoblación Acelerada**: Si la aldea queda desolada, la Caravana de Peregrinos tarda solo **1 día MC (24.000 ticks)** en lugar de 2 días.
