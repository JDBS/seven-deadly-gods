# 💻 Referencia de Comandos (`/gods ...`)

El mod proporciona un árbol completo de comandos bajo el prefijo `/gods`, divididos entre consultas públicas para cualquier jugador y comandos de administración para operadores (Nivel de permiso 2).

---

## 👥 Comandos Públicos (Para Todos los Jugadores)

### `/gods favor`
Muestra en el chat un informe detallado con tu puntuación de favor actual (`-100` a `+100`) y tu título con cada uno de los 7 Pecados Capitales.

### `/gods next`
Muestra el temporizador de la próxima intervención divina sobre tu personaje, los minutos restantes, el intervalo asignado y la lista de dioses actualmente elegibles para actuar (`|favor| >= 30`).

### `/gods village info`
Muestra en el chat la información de la aldea consagrada donde te encuentras:
* Dios patrón y estado territorial (`CONSAGRADA`, `EN_ASEDIO`, `DESOLADA`, `ARRASADA`).
* Coordenadas exactas del Altar Mayor y radio de influencia.
* Tiempo restante de enfriamiento para el próximo asalto autónomo.

---

## 🛠️ Comandos de Administración (Operadores / Nivel 2)

### `/gods setfavor <dios> <valor>`
Ajusta directamente el favor de un jugador con un dios entre `-100` y `+100`.
* Acepta identificadores en inglés (`greed`, `wrath`, etc.), nombres de dioses (`mammon`, `khorvath`, etc.) o pecados en español (`avaricia`, `ira`, etc.).

### `/gods setinterval <min_ticks> <max_ticks>` o `/gods setinterval <jugador> <min_ticks> <max_ticks>`
Reprograma el intervalo mínimo y máximo (en ticks, 20 ticks = 1 segundo) entre intervenciones divinas para tu propio personaje o para un jugador específico en multijugador.

### `/gods manifest <dios>`
Fuerza la manifestación física inmediata del Avatar del dios especificado:
* Si tu favor es `≤ -100`, el Avatar aparece en **modo Caza** para matarte.
* En cualquier otro caso, aparece en **modo Benévolo** para conversar.

### `/gods lorebook <dios>`
Entrega en el inventario del jugador un libro sagrado de relato aleatorio de la deidad indicada.

### `/gods action <dios>`
Fuerza la ejecución inmediata de la acción divina periódica (lluvia de suministros o maldición) del dios indicado (requiere que el favor con ese dios sea `≥ 30` o `≤ -30`).

### `/gods trigger_periodic`
Fuerza la evaluación del ciclo periódico: selecciona un dios al azar entre los elegibles (`|favor| >= 30`) y desata su acción divina.

### `/gods village consecrate <dios>`
Consagra o reconvierte forzosamente la aldea actual al dios indicado, colocando su Altar Mayor e inicializando su guarnición de centinelas y cultura aldeana.

### `/gods village raid [dios_atacante] [forzar_avatar]`
Inicia inmediatamente una Guerra Santa contra la aldea consagrada actual:
* `dios_atacante` *(opcional)*: Especifica qué culto lidera la invasión (si se omite, se elige un rival al azar).
* `forzar_avatar` *(opcional, true/false)*: Si es `true`, fuerza el descenso inmediato del Avatar del dios atacante al campo de batalla.

### `/gods village desolate`
Pone inmediatamente la aldea consagrada actual en estado **`DESOLADA`**, apagando el altar y activando la vigilia de humo negro para probar la Caravana de Peregrinos o el Milagro de Restauración.
