# Paso 8: Personalidades Únicas de Culto y Aldeas Arrasadas de Leviatán

Este paso implementa la diferenciación visual, temática y social de cada una de las 7 deidades del pecado, con especial énfasis en el caso de **Leviatán (Envidia)**, cuyas aldeas se generan como **Asentamientos Arrasados**, y **Belphegor (Pereza)**, poblada íntegramente por aldeanos holgazanes (*Nitwits*).

---

## 🎯 Objetivos del Paso
1. Crear la variante de estructura de **Aldea en Ruinas de Leviatán** con casas destruidas y el Altar Mayor oculto en catacumbas inundadas.
2. Inyectar la profesión `VillagerProfession.NITWIT` en todos los aldeanos que spawneen en aldeas de Belphegor (Pereza).
3. Decorar los templos y plazas con elementos temáticos (fosos de arena para Khorvath, bóveda de oro para Mammon, mesas de festín para Beelze, etc.).

---

## 📁 Archivos Involucrados

| Tipo | Ruta del Archivo | Descripción |
| :--- | :--- | :--- |
| **Estructura** | `src/main/resources/data/sevendeadlygods/structures/village/ruined_envy_shrine.nbt` | Plantilla NBT de cripta subterránea para Leviatán. |
| **Código Java** | `src/main/java/com/volcov/sevendeadlygods/village/VillageCultureApplier.java` | Modificador de profesiones y decoración según el Dios patrón. |

---

## 💻 Implementación Técnica

### 8.1 Aplicador de Cultura de Aldea (`VillageCultureApplier.java`)
```java
package com.volcov.sevendeadlygods.village;

import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.Optional;

@EventBusSubscriber(modid = SevenDeadlyGodsMod.MOD_ID)
public class VillageCultureApplier {

    @SubscribeEvent
    public static void onVillagerSpawn(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Villager villager && event.getLevel() instanceof ServerLevel level) {
            GodVillageSavedData data = GodVillageSavedData.get(level);
            Optional<GodVillageSavedData.GodVillageRecord> villageOpt = data.getVillageAt(villager.blockPosition());

            if (villageOpt.isEmpty()) return;

            SinGod god = villageOpt.get().patronGod();

            switch (god) {
                case SLOTH -> {
                    // ¡En las aldeas de la Pereza, todos los aldeanos son Holgazanes (Nitwits)!
                    VillagerData currentData = villager.getVillagerData();
                    if (currentData.profession() != VillagerProfession.NITWIT) {
                        villager.setVillagerData(currentData.withProfession(VillagerProfession.NITWIT));
                    }
                }
                case GLUTTONY -> {
                    // Predilección por carniceros y granjeros
                    VillagerData currentData = villager.getVillagerData();
                    if (currentData.profession() == VillagerProfession.NONE && level.random.nextBoolean()) {
                        villager.setVillagerData(currentData.withProfession(VillagerProfession.BUTCHER));
                    }
                }
                case GREED -> {
                    // Predilección por armeros y herreros de oro
                    VillagerData currentData = villager.getVillagerData();
                    if (currentData.profession() == VillagerProfession.NONE && level.random.nextBoolean()) {
                        villager.setVillagerData(currentData.withProfession(VillagerProfession.ARMORER));
                    }
                }
                default -> {}
            }
        }
    }
}
```

### 8.2 Rasgos Exclusivos de las 7 Deidades
1. **Mammon**: Bóveda de oro blindada en el centro; los aldeanos no abren puertas si no llevas al menos 1 pepita de oro en mano.
2. **Khorvath**: Fosos de arena de gladiadores; acólitos afilando hachas; antorchas de fuego de alma en cada esquina.
3. **Beelze**: Mesas con comida gratis; hornos y ahumadores de carne siempre encendidos emitiendo humo constante.
4. **Lucian**: Catedral de mármol y cuarzo blanco; estandartes dorados de 8 bloques de altura; los aldeanos exigen arrodillarse (`Shift`) para hablar.
5. **Leviatán**: Casas destruidas cubiertas de telarañas; el altar se ubica bajo tierra en una cripta de prismarina con agua estancada.
6. **Belphegor**: Hamacas y camas en plena calle; huertos sin cosechar con 1 gólem lento haciendo el trabajo.
7. **Asmodeus**: Jardines con rosas rojas y cerezos; música constante; animales procreando continuamente por las feromonas del ambiente.

---

## ✅ Criterios de Aceptación y Validación
1. [ ] Generar una aldea consagrada a Belphegor y verificar que sus aldeanos visten túnica verde (*Nitwits*) y duermen hasta tarde.
2. [ ] Localizar una aldea de Leviatán y verificar que se genera en estado de ruinas con la cripta subterránea.
3. [ ] Comprobar que en aldeas de Beelze las mesas de banquete contienen alimentos y en las de Asmodeus los animales se reproducen pasivamente.
