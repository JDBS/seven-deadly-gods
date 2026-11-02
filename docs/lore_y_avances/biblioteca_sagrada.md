# 📜 Biblioteca Sagrada (Los 49 Libros de Lore)

A lo largo del mundo de Minecraft descansan **49 Escrituras Sagradas** distribuidas entre los 7 Dioses del Pecado (**7 libros arcanos por deidad**), que narran los orígenes, mandamientos, secretos, santuarios territoriales y guerras del panteón impío.

---

## 🏛️ Dónde encontrar los Libros Sagrados

* **Cofres de Estructuras**: Cualquier cofre generado en mazmorras, fortalezas, templos del desierto/jungla, bastiones, ciudades del End y aldeas tiene un **35% de probabilidad** (configurable mediante `loreBookChestChance` en `ModConfig`) de contener un libro escrito sagrado.
* **Creativo y Comandos**:
  * Todos los 49 libros están disponibles directamente en la pestaña creativa de **Seven Deadly Gods**.
  * Los operadores pueden invocar un libro sagrado específico con `/gods lorebook <dios>`.

---

## 📖 Desbloqueo de Cultos y Lectura

Al abrir y leer un libro sagrado por primera vez (ya sea teniéndolo en la mano o colocándolo sobre un **Atril / Lectern**):
1. **Desbloqueo de la Receta del Altar**: Se te concede automáticamente el avance sagrado del culto correspondiente (*«Culto a Mammon», «Culto a Khorvath», etc.*), grabando en tu alma la receta para construir su Altar de Ritual.
2. **Recompensa de Experiencia y Sabiduría**: Al asimilar cada escritura por primera vez, el jugador recibe **25 puntos de experiencia** (configurable mediante `loreBookFirstReadXp` en `ModConfig`), acompañado de partículas arcanas de encantamiento y el sonido místico de orbe recolectado.
3. **Registro Arcano**: El libro queda registrado en la memoria de tu personaje (`PlayerSinData.markLoreBookRead`).

---

## 📚 Catálogo de las 49 Escrituras por Deidad

Cada deidad cuenta con 6 parábolas y mandamientos individuales más un **7º tomo territorial** que revela las leyes de sus aldeas consagradas y santuarios:

| Dios | Tomos Individuales (1 al 6) | 7º Tomo Territorial (Santuarios y Leyes) |
| :--- | :--- | :--- |
| **Mammon** *(Avaricia)* | *Parábola del Herrero*, *El Peso del Oro Oculto*, *El Don de Midas*, *Cosecha de los Deudores*, *El Pacto del Ungido*, *Juicio del Usurero* | **El Diezmo de las Bóvedas** *(Explica el peaje en puertas y el cobro de pepitas exclusivamente tras abrir con éxito cofres y barriles)* |
| **Khorvath** *(Ira)* | *El Ara Sedienta*, *Anatema de los Cobardes*, *Elogio de la Agonía*, *El Bautismo de Sangre*, *El Perseguidor de Hueso*, *El Día de la Cólera* | **La Guardia del Templo Carmesí** *(Explica la guarnición de 6 centinelas permanentes del altar y la conversión independiente tras la victoria)* |
| **Beelze** *(Gula)* | *La Mesa de las Ofrendas*, *El Pecado de la Escasez*, *Salmo de los Manjares*, *El Vientre Bendito*, *Parábola del Asceta*, *El Convite del Avatar* | **El Banquete Perpetuo de la Villa** *(Explica los hornos perennes, los aldeanos carniceros, el aura de saciedad y la vigilia tras la desolación)* |
| **Lucian** *(Soberbia)* | *El Santuario de las Cumbres*, *Rito de la Reverencia*, *Crónica del Deshonor*, *La Corona del Sol*, *Castigo de la Lengua Altiva*, *El Decreto de Vida* | **El Protocolo de la Cerviz Inclinada** *(Explica la exención de altitud en catedrales y el arrodillarse con Shift para abrir tratados de comercio)* |
| **Leviatán** *(Envidia)* | *Las Aguas Celosas*, *Liturgia del Despojo*, *La Guerra de los Cielos*, *El Velo del Abismo*, *Arpones en la Bruma*, *La Reliquia de las Mareas* | **El Asolamiento de los Santuarios** *(Explica la generación como aldeas arrasadas y la reducción a ruinas tras una conquista de asedio)* |
| **Belphegor** *(Pereza)* | *Evangelio del Reposo*, *Contra la Vanidad de la Prisa*, *La Voz entre Bostezos*, *El Manto de la Quietud*, *El Castigo del Afanoso*, *Veladores del Sueño* | **La Vigilia del Gólem Durmiente** *(Explica los aldeanos Nitwits, los 2 gólems en vigilia desolada y la congelación del asedio al ausentarse)* |
| **Asmodeus** *(Lujuria)* | *Cantar del Jardín Florido*, *Las Vestiduras de Gala*, *Lamento del Prado Marchito*, *El Perfume de la Gracia*, *Las Espinas de la Rosa*, *Epifanía Carmesí* | **El Vergel del Renacimiento** *(Explica los jardines floridos, la cría animal pasiva y la Caravana de Peregrinos acelerada a solo 24.000 ticks)* |

---

## 🏆 Títulos de Coleccionista y Avances Arcanos

* **10 Libros Leídos**: Desbloquea el avance **«Buscador de Herejías»**.
* **25 Libros Leídos**: Desbloquea el avance **«Erudito del Abismo»** (más de la mitad del conocimiento arcano reunido).
* **40 Libros Leídos**: Desbloquea el avance supremo **«Archivero de los Siete Dioses»**.
