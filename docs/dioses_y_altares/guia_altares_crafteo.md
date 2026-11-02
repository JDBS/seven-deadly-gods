# 🔨 Guía Universal de Crafteo de Altares

Cada uno de los 7 Pecados Capitales se adora a través de su propio **Altar de Ritual**. A pesar de sus efectos asimétricos, todos los altares comparten una estructura arquitectónica común en la mesa de crafteo.

---

## 📐 Patrón Universal en Mesa de Crafteo

Todos los altares utilizan **4 bloques de Ladrillos de Piedra (`stone_bricks`)** organizados en forma de **T** (fila central e inferior). Lo único que cambia son los **3 componentes de la fila superior**, que representan la esencia del pecado:

```text
[ Ingrediente Izq ] [ Ingrediente Central ] [ Ingrediente Der ]
[     Vacío       ] [ Ladrillos de Piedra ] [     Vacío       ]
[ Ladrillos Piedra] [ Ladrillos de Piedra ] [ Ladrillos Piedra]
```

---

## 📜 Recetas de la Fila Superior para los 7 Dioses

| Pecado | Dios | Fila Superior del Crafteo | Bloque Resultante |
| :--- | :--- | :--- | :--- |
| **Avaricia** | **Mammon** | `Lingote de Oro` + `Esmeralda` + `Lingote de Oro` | `Altar de Mammon` (`greed_altar`) |
| **Ira** | **Khorvath** | `Redstone` + `Espada de Hierro` + `Redstone` | `Altar de Khorvath` (`wrath_altar`) |
| **Gula** | **Beelze** | `Manzana` + `Cuenco (Bowl)` + `Manzana` | `Altar de Beelze` (`gluttony_altar`) |
| **Soberbia** | **Lucian** | `Lingote de Oro` + `Cristal (Glass)` + `Lingote de Oro` | `Altar de Lucian` (`pride_altar`) |
| **Envidia** | **Leviatán** | `Lapislázuli` + `Cristal (Glass)` + `Lapislázuli` | `Altar de Leviatán` (`envy_altar`) |
| **Pereza** | **Belphegor** | `Lana Blanca` + `Cama Blanca` + `Lana Blanca` | `Altar de Belphegor` (`sloth_altar`) |
| **Lujuria** | **Asmodeus** | `Amapola (Poppy)` + `Fragmento de Amatista` + `Amapola (Poppy)` | `Altar de Asmodeus` (`lust_altar`) |

---

## 🏛️ Altares Mayores de Aldea vs. Altares del Jugador

* **Bono de Esfuerzo Devocional (+20%)**: Los altares ubicados en el centro de las **Aldeas Consagradas** son reconocidos como *Santuarios Mayores Canónicos*. Todos los rituales realizados en ellos otorgan un **20% más de esfuerzo de favor** que los altares construidos por el jugador en su base.
* **Exención de Altitud para Soberbia**: Mientras que el altar de Soberbia construido por un jugador exige estar por encima de `Y > 120` y a cielo abierto, las catedrales de aldea consagradas a Lucian están automáticamente bendecidas sin importar la altura del terreno.
