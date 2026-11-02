# ⚔️ Guerras Santas, Cuerno de Guerra y Choque de Dioses

Las facciones divinas compiten constantemente por expandir su culto y saquear los templos ajenos mediante **Guerras Santas y Asaltos Territoriales**.

---

## 🛡️ Asaltos Autónomos entre Facciones

Cada **3 días de Minecraft** (`72.000` ticks, configurable en `ModConfig`):
1. **Condición de Disparo**: Si una aldea consagrada (`CONSAGRADA`) tiene a un jugador vivo en un radio de **80 bloques**, existe una probabilidad del **15% por minuto** de que un dios rival ordene un asedio.
2. **Selección del Rival**: Se selecciona al azar uno de los otros 6 dioses del pecado (con **doble probabilidad para Leviatán / Envidia**, cuyo mayor deseo es saquear templos prósperos).
3. **Anuncio Global**: Suena el cuerno de asalto (`RAID_HORN`) y todo el servidor recibe la advertencia en el chat:
   * *«⚔ ¡GUERRA SANTA! Acólitos de [Atacante] marchan para saquear el Templo de [Defensor] en [X, Z]!»*
4. **Despliegue del Escuadrón**: Spawnea un contingente invasor de **8 Acólitos hostiles** en el perímetro de la aldea avanzando directamente hacia el Altar Mayor.

---

## 📯 Asedios Liderados por el Jugador: El Cuerno de Guerra Divino

El **Cuerno de Guerra Divino (`divine_war_horn`)** permite a los mortales convocar la furia de su deidad para liderar una conquista en persona:

* **Cómo obtenerlo**: Se adquiere en el Nivel 4 de Comercio Divino con cualquier Acólito (`Favor >= +35`).
* **Requisitos de uso**:
  * Debes tener al menos **`Favor >= +40`** con al menos un dios (tu dios con mayor devoción liderará el asedio).
  * Debes encontrarte dentro de una **aldea consagrada rival** (`village.patronGod != playerGod`) que no esté bajo asedio en ese momento.
* **Efecto al sonar el cuerno**:
  * Hace sonar el cuerno de asedio con estruendo global.
  * Inicia de inmediato la Guerra Santa contra el templo enemigo.
  * **Spawnea un escuadrón aliado de 6 Acólitos de Asalto** que obedecen al jugador y marchan a derribar a los centinelas rivales.
  * Aplica un enfriamiento de **60 segundos** en el ítem.

---

## ⚡ Tirada del 5% de Manifestación del Avatar y el Choque de Dioses

Durante cualquier asedio (autónomo o liderado por el jugador), la sangre derramada puede romper el velo celestial:

1. **Tirada del 5%**: Existe un **5% de probabilidad estricta** (configurable en `ModConfig`) de que el Avatar de una de las dos deidades involucradas descienda físicamente en mitad de la batalla para impartir justicia.
2. **El Gran Choque de Dioses (15% del 5%)**:
   * Dentro de ese 5%, hay un **15% de probabilidad** de que los cielos se desgarren y **desciendan simultáneamente los Avatares de AMBOS dioses rivales** para batirse en un duelo colosal cuerpo a cuerpo en mitad de la aldea (*«⚡ ¡CHOQUE DE DIOSES! Los Avatares de [...] y [...] han descendido en persona para batirse en duelo!»*).

---

## 🏛️ Prioridad Territorial y Regla de Avatar Único

Si el Avatar elegido para manifestarse en la aldea estaba en ese momento persiguiendo a muerte a un jugador en una Caza de Ira individual (`favor <= -100`):
* **El asedio territorial tiene prioridad absoluta**: Se pausa la cacería individual y se le notifica al jugador condenado que su juicio queda aplazado, vinculando la suspensión exclusivamente a esa aldea.
* El Avatar se teletransporta instantáneamente al santuario aldeano **conservando su barra de salud actual**, desactiva cualquier tarea de escolta de jugador previo (`StayNearChampionGoal`) y adopta asignación territorial fija (`raidAltarPos`).
* Su estado de asedio queda **persistido en disco**, impidiendo que expire tras 4.800 ticks o tras una recarga de chunks.
* Al finalizar el asedio en esa aldea concreta (por victoria o conquista), si el Avatar sobrevive, **se reanuda automáticamente la cacería de ira** pospuesta contra el jugador sentenciado; los asedios en otras aldeas no son afectados por esta resolución.

---

## ⏸️ Persistencia, Pausa y Duración Activa

* **Persistencia Integral**: El estado de asedio (`EN_ASEDIO`), el dios atacante original (`attackerGod`) y el tiempo activo transcurrido (`activeRaidTicks`) se guardan en el archivo de datos de la dimensión (`GodVillageSavedData`). Tras un reinicio del servidor, el combate se reanuda con el mismo rival sin sortear deidades erróneas.
* **Pausa por Descarga de Chunks**: Si ningún jugador permanece a menos de 128 bloques del altar, la evaluación del asalto se congela. El límite máximo de 5 minutos (**6.000 ticks**) mide **exclusivamente tiempo activo en combate**; ausentarse de la aldea no consumirá el tiempo ni concederá victorias por timeout anticipadas.

---

## 🏆 Resolución del Asedio: Victoria o Conquista

* **Victoria de los Defensores**:
  * Si todos los invasores son repelidos (o se agotan los 5 minutos máximos de combate activo), el templo prevalece.
  * Suena fanfarria victoriosa (`TOTEM_OF_UNDYING`), la aldea vuelve a estado `CONSAGRADA` y **todos los jugadores fieles cercanos reciben +3.0 de esfuerzo devocional** por defender el templo.
* **Conquista del Templo (Victoria Invasora)**:
  * Si la guarnición defensora cae por completo, el Altar Mayor es profanado y **reconvertido automáticamente al bloque de altar del dios conquistador**.
  * Los acólitos invasores supervivientes se transforman en **Centinelas Permanentes** del nuevo culto, desligándose de cualquier comandante jugador previo y fijando su patrulla sobre el nuevo altar.
  * La aldea pasa a ser consagrada al nuevo dios (**o pasa a estado `ARRASADA` si el conquistador fue Leviatán**).
