# Paso 9: Comandos de Depuración, Batería de Pruebas y Pulido Final

Este paso final proporciona las herramientas de administración y depuración (`/gods village ...`), los casos de prueba de estrés en multijugador y las optimizaciones de rendimiento para el lanzamiento de la característica.

---

## 🎯 Objetivos del Paso
1. Añadir el subcomando `/gods village` para inspeccionar aldeas, forzar asedios y consagrar o desolar poblados en tiempo de desarrollo.
2. Ejecutar la batería de pruebas de carga: 10 asedios simultáneos, descarga abrupta de chunks y reconexión de jugadores.
3. Verificar la sincronización en clientes multijugador (sonidos, partículas y barras de jefe de avatares).

---

## 📁 Archivos Involucrados

| Tipo | Ruta del Archivo | Descripción |
| :--- | :--- | :--- |
| **Comandos** | `src/main/java/com/volcov/sevendeadlygods/command/GodsCommands.java` | Subcomandos de administración de aldeas. |
| **Configuración** | `src/main/java/com/volcov/sevendeadlygods/config/ModConfig.java` | Parámetros configurables (probabilidad 20%, cooldowns). |

---

## 💻 Implementación Técnica

### 9.1 Subcomandos de Administración en `GodsCommands.java`
Se añaden los siguientes subcomandos para agilizar el testeo:

```java
// /gods village info -> Muestra datos de la aldea actual (dios, estado, radio, raid cooldown)
// /gods village consecrate <dios> -> Consagra manualmente la aldea actual a un dios
// /gods village raid [atacante] -> Fuerza el inicio inmediato de un asedio en la aldea
// /gods village desolate -> Fuerza el estado de desolación para probar la caravana de peregrinos
```

Fragmento de registro en Brigadier:

```java
builder.then(Commands.literal("village")
    .requires(source -> source.hasPermission(2))
    .then(Commands.literal("info")
        .executes(ctx -> executeVillageInfo(ctx.getSource())))
    .then(Commands.literal("raid")
        .then(Commands.argument("attacker", StringArgumentType.word())
            .executes(ctx -> executeForceRaid(ctx.getSource(), StringArgumentType.getString(ctx, "attacker")))))
    .then(Commands.literal("consecrate")
        .then(Commands.argument("god", StringArgumentType.word())
            .executes(ctx -> executeForceConsecrate(ctx.getSource(), StringArgumentType.getString(ctx, "god")))))
);
```

---

## 🧪 Batería de Pruebas de Integración

### Prueba A: Ciclo Completo de Asedio y Regla de Avatar Único
1. Iniciar una caza de ira con Khorvath sobre el Jugador 1 (`favor = -100`).
2. Mientras Khorvath persigue al Jugador 1, teletransportarse a una aldea consagrada a Mammon y ejecutar `/gods village raid wrath`.
3. Forzar el 5% de manifestación de Khorvath:
   * **Verificación**: El Avatar de Khorvath abandona al Jugador 1 con mensaje de aplazamiento y aparece en la aldea de Mammon.
4. Derrotar a los invasores de la aldea:
   * **Verificación**: El Avatar de Khorvath se teletransporta de vuelta ante el Jugador 1 y reanuda la cacería a muerte.

### Prueba B: Prueba de Estrés de Descarga de Chunks (*Chunk Unload*)
1. Iniciar un asedio masivo en una aldea consagrada.
2. Hacer que todos los jugadores se teletransporten a 5,000 bloques de distancia.
3. Verificar en la consola del servidor que no se generan excepciones `NullPointerException` ni fugas de memoria.
4. Regresar a la aldea: verificar que la raid se reanuda de forma limpia sin entidades duplicadas.

### Prueba C: Bucle de Desolación y Peregrinación
1. Ejecutar `/gods village desolate` en una aldea activa.
2. Confirmar que el fuego del altar se apaga y emite humo negro.
3. Ejecutar `/time add 48000` y comprobar la llegada de la caravana de 3 peregrinos y 1 centinela escolta.

---

## 🏁 Checklist de Finalización de la Característica
- [ ] Todos los archivos Java compilan limpiamente sin advertencias (`./gradlew build`).
- [ ] No existen dependencias circulares ni fugas de memoria en `GodVillageSavedData`.
- [ ] La experiencia de usuario y los mensajes de Gemini / chat se leen fluidos, atmosféricos y sin spam.
- [ ] Documentación sincronizada y lista para mantenimiento a largo plazo.
