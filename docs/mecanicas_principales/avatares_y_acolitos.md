# 👑 Acólitos, Avatares y Deicidio

Los dioses se hacen presentes en el mundo físico de Minecraft mediante dos jerarquías de entidades: sus siervos alados (**Acólitos**) y sus encarnaciones físicas colosales (**Avatares**).

---

## 🪽 Acólitos del Pecado (`GodAcolyteEntity`)

Los Acólitos son humanoides alados encapuchados que visten las túnicas sagradas de su deidad y portan armaduras y armas acordes a su naturaleza.

### Modos de Comportamiento:
1. **Centinelas de Aldea (`villageSentinel`)**: Patrullan permanentemente el perímetro de los templos en aldeas consagradas (no despawnean jamás). Son independientes de cualquier comandante jugador previo. Actúan como **Mercaderes Sagrados** si tu `favor >= 0`, te rechazan si `-39 <= favor < 0` y te atacan a la vista si eres un hereje (`favor <= -40`). Solo los acólitos con este modo activo satisfacen el cupo de defensores del santuario.
2. **Escuadrones de Asalto (Guerras Santas)**: Desplegados en grupos de 8 durante asaltos autónomos o en grupos de 6 convocados por el jugador mediante el *Cuerno de Guerra Divino*. Si conquistan el santuario, se convierten en centinelas independientes del templo.
3. **Escoltas de Devoto (Favor >= +85)**: Cuando tu vida baja del 35% o entras en combate con siervos de otros dioses, tu dios invoca a 2 Acólitos leales que te siguen y defienden como ángeles custodios durante 6.000 ticks.
4. **Cazadores de Herejes (Favor <= -80)**: Descienden desde el cielo para rodearte y ejecutarte.

### Jerarquía Táctica de Objetivos en Combate:
* **Prioridad 1 (Respuesta de Enjambre Cohesionada)**: Al recibir daño, contraatacan inmediatamente y alertan a todos los acólitos cercanos pertenecientes a la misma fe (`alertOther`). Mediante el control de `canAttack`, están completamente blindados contra fuego amigo interno con su propia deidad o entre acólitos hermanos.
* **Prioridad 2 (Infantería vs Infantería y Herejes)**: Se enfrentan de forma prioritaria contra acólitos enemigos (`GodAcolyteEntity` con `other.getGod() != this.getGod()`) y castigan a jugadores con herejía territorial declarada (`favor <= -40`).
* **Prioridad 3 (Choque de Fe contra Titanes Divinos)**: Si no hay infantería rival presente o si defienden su templo, cargan coordinadamente contra los Avatares de dioses rivales (`GodAvatarEntity`), asediando al titán enemigo y desgastando su avance.
* **Prioridad 4 (Culto de la Envidia - Despojo y Aniquilación)**: Exclusivo de los acólitos de Leviatán (`SinGod.ENVY`): persiguen y atraviesan con sus tridentes a los aldeanos civiles (`Villager`), y en incursiones no aliadas destruyen proactivamente a los gólems defensores (`IronGolem`).
* **Prioridad 5 (Purga de Monstruos Comunes)**: Como centinelas permanentes o escoltas leales, defienden el perímetro sagrado eliminando monstruos hostiles vainilla (`Monster`).

---

## ⚡ Avatares del Pecado (`GodAvatarEntity`)

El Avatar es la manifestación física directa y colosal de la deidad:
* **Estadísticas de Jefe**: `400 HP`, escala visual colosal (`1.5x`), `50% de reducción pasiva de daño innata`, buffs de `Resistencia II` (40%) y `Regeneración I`, armadura con `Protección V`, ignífugos e inmunes a caída.
* **Barra de Jefe Dinámica**: Muestra el nombre sagrado del dios en su color temático y su título.
* **Equipamiento Sobre-encantado**: Portan armadura completa personalizada y armas legendarias con encantamientos de nivel superior (*Filo VIII*, *Frenesí de Furia*, etc.).
* **Persistencia y Scoping Territorial**: Su estado como defensor de asedio (`villageRaidAvatar`) y su altar de pertenencia (`raidAltarPos`) se guardan en el NBT de la entidad; no expiran a los 4.800 ticks de una manifestación ordinaria mientras la aldea siga en asedio, y suspenden su tarea de seguimiento a campeones (`StayNearChampionGoal`) para centrarse en el santuario.

---

## 🏛️ Regla Fundamental: El Avatar Único

En **Seven Deadly Gods** rige la regla estricta de **Avatar Único**:
> **Bajo ninguna circunstancia pueden existir dos Avatares de la misma deidad al mismo tiempo en el mundo.**

### Prioridad Territorial sobre Cazas de Ira:
Si un dios está persiguiendo a muerte a un jugador en una Caza de Ira individual (`favor <= -100`), y en ese momento su aldea consagrada es asediada en una Guerra Santa (o desciende en el 5% de tirada de asedio):
1. **La aldea tiene prioridad absoluta**: El dios envía un mensaje al jugador perseguido aplazando temporalmente su ejecución (*«Mortal, tu juicio queda aplazado: mi templo sagrado clama por mi presencia...»*).
2. El Avatar se teletransporta instantáneamente al santuario aldeano para liderar la defensa o conquista, **conservando su barra de vida intacta** y cancelando cualquier retorno a jugadores previos.
3. Al concluir la batalla en esa aldea concreta, si el Avatar ha sobrevivido, **se reanuda automáticamente la Caza de Ira** pospuesta contra el jugador sentenciado.

---

## ⚔️ Deicidio: Derrotar al Avatar de un Dios

Abatir al Avatar físico de un dios es una de las mayores proezas del mod:

1. **Recompensas Masivas de Favor**:
   * **Lucian (Soberbia)**: `+20.0` de esfuerzo devocional ("Gesta suprema de Deicidio").
   * **Khorvath (Ira)**: `+15.0` de esfuerzo devocional ("Derramamiento de sangre divina").
   * **Leviatán (Envidia)**: `+15.0` de esfuerzo devocional ("Humillación y despojo del Avatar rival").
   * *(El propio dios abatido no concede favor positivo).*
2. **Letargo del Altar Mayor y Silencio Cósmico**:
   * Tras la muerte del Avatar, la presencia del dios entra en un **profundo letargo cósmico** (3 a 7 días).
   * Su Altar Mayor queda frío e inerte con partículas de alma y humo negro: ninguna ofrenda ni ritual será escuchado.
   * La deidad no interviene ni responde al chat general; si se le invoca o nombra directamente, solo responderá un frío eco de silencio cósmico del vacío y se congelarán todas las fluctuaciones de favor.
