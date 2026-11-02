# 💬 Chat Inteligente con Reacciones Activas

Los 7 Pecados Capitales escuchan activamente todo lo que dices en el chat de Minecraft. Dependiendo del contexto de tu personaje, tu proximidad a un Altar o Avatar, y el contenido de tu mensaje, los dioses pueden intervenir, responderte con voz propia y desatar reacciones físicas en el mundo.

---

## 🎯 ¿Cuándo escucha un Dios?

Un dios evaluará tu mensaje si se cumple cualquiera de estas condiciones:
1. **Mención explícita**: Escribes el nombre de la deidad (*Mammon, Khorvath, Beelze, Lucian, Leviatán, Belphegor, Asmodeus*) o de su pecado.
2. **Proximidad a un Altar**: Estás a menos de 8 bloques del Altar consagrado a ese dios.
3. **Proximidad a un Avatar**: Estás a menos de 24 bloques de su Avatar físico.
4. **Palabras temáticas de su dominio**: Mencionas conceptos fuertemente ligados a su pecado (oro, tesoro, sangre, matar, banquete, comer, orgullo, cumbre, robar, descanso, cama, flor, deseo).

---

## 🎲 Probabilidad de Alterar el Favor por Chat

Hablar en el chat **no garantiza sumar o restar favor** automáticamente. El sistema utiliza una fórmula probabilística que evalúa dos factores fundamentales:

### 1. El Progreso (|Favor| Actual)
* **Tramo Inicial (0 a 20 de favor)**: Probabilidad muy alta (~85% a 70%). El dios siente curiosidad por tus primeras palabras.
* **Tramo Intermedio (21 a 60 de favor)**: Probabilidad moderada (~70% a 40%). Requiere constancia en tus plegarias.
* **Tramo Avanzado (61 a 85 de favor)**: Probabilidad baja (~40% a 15%). El dios se vuelve exigente.
* **Tramo Extremo (86 a 100 de favor)**: Probabilidad mínima (15% a 5%). En la cima de la devoción, las meras palabras rara vez impresionan; se requieren sacrificios tangibles en su altar.

### 2. Sentido del Movimiento (Avanzar vs. Caer)
* **Caer de la gracia por insolencia (`favor > 0` con insulto)**: Probabilidad muy alta (**70% a 80%**). Los dioses son celosos y soberbios; insultar al dios con el que tienes alta devoción te hará perder favor con extrema facilidad.
* **Redención desde el abismo (`favor < 0` pidiendo perdón)**: Probabilidad reducida (35% a 50%). Un hereje condenado debe suplicar con fervor reiterado para que un dios ofendido vuelva a prestarle atención.

---

## ⚡ Reacciones Físicas Automáticas en el Chat

Además de la respuesta de texto generada por Gemini o el motor local, el sistema puede desencadenar efectos en el mundo:

| Tipo de Reacción | Condición | Efecto Físico en el Mundo |
| :--- | :--- | :--- |
| **`LIGHTNING` (Rayo Divino)** | Blasfemia, insulto grave o desafío directo. | Cae un rayo celestial sobre el jugador (o descarga mágica de 4 HP si está bajo techo) y se penaliza el favor (-1.0). |
| **`CURSE` (Maldición Temática)** | Desprecio o arrogancia hacia su pecado. | Aplica de inmediato la aflicción temática del dios (fatiga minera, hambre, ceguera, levitación, etc.). |
| **`REPULSION` (Onda Expansiva)** | Blasfemar estando pegado al Altar o Avatar. | Una onda de choque lanza al jugador varios bloques por el aire con daño de impacto. |
| **`BLESSING` (Bendición)** | Plegarias sinceras, súplicas de piedad o alabanzas. | Restaura salud, saciedad y otorga partículas de encantamiento y regeneración. |
| **`SUMMON_AVATAR`** | Llamada directa teniendo **+100 de Favor**. | Invoca pacíficamente al Avatar del dios ante ti (cooldown de 1 día de Minecraft = 20 minutos). |
