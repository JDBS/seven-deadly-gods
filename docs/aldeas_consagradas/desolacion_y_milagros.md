# 🕯️ Desolación, Caravana de Peregrinos y Milagros de Restauración

En **Seven Deadly Gods**, las aldeas consagradas poseen mecanismos de auto-preservación para evitar que queden permanentemente vacías ante masacres o asedios sangrientos.

---

## 💀 Transición al Estado `DESOLADA`

Si durante una incursión, asedio o incursión de monstruos todos los **aldeanos civiles** de una aldea consagrada pierden la vida (`0 aldeanos vivos` en su radio territorial):

1. **Vigilia del Altar**: La aldea pasa al estado **`DESOLADA`**, quedando protegida contra cualquier asedio rival en esa misma iteración para garantizar el ciclo sagrado de duelo.
2. **Efectos Audiovisuales**:
   * Suena la desactivación solemne del faro (`BEACON_DEACTIVATE`).
   * El fuego sagrado del Altar Mayor se apaga y comienza a emitir continuamente **humo negro de luto** y chispas moribundas.
3. **Anuncio en el Servidor**:
   * *«🕯 ¡SANTUARIO DESOLADO! La aldea consagrada a [Dios] en [X, Z] ha perdido a todos sus habitantes. Su altar entra en vigilia...»*
4. **Los Guardianes de Belphegor (Pereza)**:
   * Si la aldea desolada pertenece a Belphegor, el dios invoca de inmediato a **2 Gólems de Hierro durmientes** en el templo para custodiar las ruinas hasta que los fieles regresen.

---

## 🚶‍♂️ Llegada Automática de la Caravana de Peregrinos

Los templos no permanecen muertos para siempre:

* Tras cumplirse **2 días de Minecraft de luto (`48.000` ticks)**:
  * *(O solo **1 día MC / 24.000 ticks** en aldeas de Asmodeus, gracias a la fertilidad del culto).*
* Una **Caravana de Peregrinos** llega desde las afueras de la aldea avanzando hacia el templo:
  * Spawnean **3 aldeanos civiles nuevos** (4 para Asmodeus) que caminan hacia la plaza para repoblar las casas.
  * Spawnea **1 Acólito Centinela Permanente** como escolta armada para custodiar el Altar Mayor.
* El estado territorial se restablece a **`CONSAGRADA`**, reencendiendo el templo con un anuncio alegre en el chat:
  * *«✨ ¡EL LLAMADO DE LOS FIELES! Nuevos devotos llegan en peregrinación para reavivar el Templo de [Dios]!»*

---

## ✨ El Milagro de Restauración (Intervención del Jugador)

Un jugador devoto no tiene que esperar los 2 días de Minecraft para ver su templo revitalizado:

### Requisitos:
1. Mantener un **`Favor >= +30`** con el dios patrón de la aldea desolada.
2. Acudir al Altar Mayor desolado sosteniendo una **ofrenda de restauración sagrada**:
   * **Mammon**: Cualquier mineral noble refinado (oro, diamante, netherite).
   * **Khorvath**: Polvo de blaze, fragmento de netherite, diamante o hierro.
   * **Beelze**: Manzana dorada, pastel, zanahoria dorada o filete.
   * **Lucian**: Lingote de oro, diamante, manzana dorada o amatista.
   * **Leviatán**: Esmeralda, perla de ender, tótem o manzana dorada.
   * **Belphegor**: Reloj, lana, pluma o perla de ender.
   * **Asmodeus**: Fragmento de amatista, rosal, manzana dorada o panal.

### Efectos del Milagro:
* Se consume 1 unidad de la ofrenda.
* Se desata una explosión de partículas de Tótem (`TOTEM_OF_UNDYING`).
* **La Caravana de Peregrinos spawnea instantáneamente** en el perímetro de la aldea.
* La aldea vuelve a estar activa (`CONSAGRADA`) en el acto.
* **El jugador recibe +4.0 de esfuerzo devocional** por el mérito de haber resucitado el santuario de su dios.
