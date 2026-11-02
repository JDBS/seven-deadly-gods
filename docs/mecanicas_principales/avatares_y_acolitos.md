# 👑 Acólitos, Avatares y Deicidio

Los dioses se hacen presentes en el mundo físico de Minecraft mediante dos jerarquías de entidades: sus siervos alados (**Acólitos**) y sus encarnaciones físicas colosales (**Avatares**).

---

## 🪽 Acólitos del Pecado (`GodAcolyteEntity`)

Los Acólitos son humanoides alados encapuchados que visten las túnicas sagradas de su deidad y portan armaduras y armas acordes a su naturaleza.

### Modos de Comportamiento:
1. **Centinelas de Aldea (`villageSentinel`)**: Patrullan permanentemente el perímetro de los templos en aldeas consagradas (no despawnean jamás). Actúan como **Mercaderes Sagrados** si tu `favor >= 0`, te rechazan si `-39 <= favor < 0` y te atacan a la vista si eres un hereje (`favor <= -40`).
2. **Escuadrones de Asalto (Guerras Santas)**: Desplegados en grupos de 8 durante asaltos autónomos o en grupos de 6 convocados por el jugador mediante el *Cuerno de Guerra Divino*.
3. **Escoltas de Devoto (Favor >= +85)**: Cuando tu vida baja del 35% o entras en combate con siervos de otros dioses, tu dios invoca a 2 Acólitos leales que te siguen y defienden como ángeles custodios.
4. **Cazadores de Herejes (Favor <= -80)**: Descienden desde el cielo para rodearte y ejecutarte.

---

## ⚡ Avatares del Pecado (`GodAvatarEntity`)

El Avatar es la manifestación física directa y colosal de la deidad:
* **Estadísticas de Jefe**: `500 HP`, escala visual colosal (`1.5x`), `80% de reducción pasiva de daño`, inmunidad a proyectiles débiles, ignífugos e inmunes a caída.
* **Barra de Jefe Dinámica**: Muestra el nombre sagrado del dios en su color temático y su título.
* **Equipamiento Sobre-encantado**: Portan armadura completa personalizada y armas legendarias con encantamientos de nivel superior (*Filo VIII*, *Frenesí de Furia*, etc.).

---

## 🏛️ Regla Fundamental: El Avatar Único

En **Seven Deadly Gods** rige la regla estricta de **Avatar Único**:
> **Bajo ninguna circunstancia pueden existir dos Avatares de la misma deidad al mismo tiempo en el mundo.**

### Prioridad Territorial sobre Cazas de Ira:
Si un dios está persiguiendo a muerte a un jugador en una Caza de Ira individual (`favor <= -100`), y en ese momento su aldea consagrada es asediada en una Guerra Santa (o desciende en el 5% de tirada de asedio):
1. **La aldea tiene prioridad absoluta**: El dios envía un mensaje al jugador perseguido aplazando temporalmente su ejecución (*«Mortal, tu juicio queda aplazado: mi templo sagrado clama por mi presencia...»*).
2. El Avatar se teletransporta instantáneamente al santuario aldeano para liderar la defensa o conquista, **conservando su barra de vida intacta**.
3. Al concluir la batalla en la aldea, si el Avatar ha sobrevivido, **se reanuda automáticamente la Caza de Ira** contra el jugador sentenciado.

---

## ⚔️ Deicidio: Derrotar al Avatar de un Dios

Abatir al Avatar físico de un dios es una de las mayores proezas del mod:

1. **Recompensas Masivas de Favor**:
   * **Lucian (Soberbia)**: `+20.0` de esfuerzo devocional ("Gesta suprema de Deicidio").
   * **Khorvath (Ira)**: `+15.0` de esfuerzo devocional ("Derramamiento de sangre divina").
   * **Leviatán (Envidia)**: `+15.0` de esfuerzo devocional ("Humillación y despojo del Avatar rival").
   * *(El propio dios abatido no concede favor positivo).*
2. **Letargo del Altar Mayor**:
   * Tras la muerte del Avatar, la presencia del dios entra en un **profundo letargo cósmico**.
   * Su Altar Mayor queda frío e inerte con partículas de alma y humo negro: ninguna ofrenda será escuchada hasta que su espíritu despierte del vacío.
