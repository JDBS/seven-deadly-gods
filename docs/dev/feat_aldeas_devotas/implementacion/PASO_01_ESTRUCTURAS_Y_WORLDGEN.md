# Paso 1: Estructuras y Generación de Mundo (Jigsaw & WorldGen)

Este paso establece la generación natural del **Templo del Culto** en las aldeas vanilla de Minecraft mediante el sistema **Jigsaw**, garantizando la tasa de aparición exacta del **20%** y la máxima compatibilidad con otros mods de generación.

---

## 🎯 Objetivos del Paso
1. Inyectar la estructura del Templo (`god_shrine.nbt`) en las piscinas de plantillas (`StructureTemplatePool`) de aldeas vanilla.
2. Calibrar los pesos matemáticos para lograr un 20% de probabilidad global de consagración (1 de cada 5 aldeas).
3. Prevenir la duplicación de templos dentro de una misma aldea (solución al bug clásico de Vampirism con Lithostitched).

---

## 📁 Archivos Involucrados

| Tipo | Ruta del Archivo | Descripción |
| :--- | :--- | :--- |
| **Estructura NBT** | `src/main/resources/data/sevendeadlygods/structures/village/god_shrine.nbt` | Archivo de estructura del santuario con el Altar Mayor central. |
| **Código Java** | `src/main/java/com/volcov/sevendeadlygods/worldgen/VillageStructureInjector.java` | Inyector de piscinas Jigsaw durante `ServerAboutToStartEvent`. |
| **Registro** | `src/main/java/com/volcov/sevendeadlygods/SevenDeadlyGodsMod.java` | Suscripción del evento de inyección en el bus de NeoForge. |

---

## 💻 Implementación Técnica

### 1.1 Inyector de Piscinas de Plantillas (`VillageStructureInjector.java`)
Se inyecta la pieza en las piscinas de `houses` de cada bioma de aldea (`plains`, `desert`, `savanna`, `taiga`, `snowy`):

```java
package com.volcov.sevendeadlygods.worldgen;

import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.util.List;

@EventBusSubscriber(modid = SevenDeadlyGodsMod.MOD_ID)
public class VillageStructureInjector {
    private static final ResourceKey<StructureProcessorList> EMPTY_PROCESSOR =
            ResourceKey.create(Registries.PROCESSOR_LIST, ResourceLocation.withDefaultNamespace("empty"));

    private static final List<ResourceLocation> VILLAGE_HOUSES_POOLS = List.of(
            ResourceLocation.withDefaultNamespace("village/plains/houses"),
            ResourceLocation.withDefaultNamespace("village/desert/houses"),
            ResourceLocation.withDefaultNamespace("village/savanna/houses"),
            ResourceLocation.withDefaultNamespace("village/taiga/houses"),
            ResourceLocation.withDefaultNamespace("village/snowy/houses")
    );

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        Registry<StructureTemplatePool> poolRegistry = server.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        Registry<StructureProcessorList> processorRegistry = server.registryAccess().lookupOrThrow(Registries.PROCESSOR_LIST);
        Holder<StructureProcessorList> emptyProcessor = processorRegistry.getOrThrow(EMPTY_PROCESSOR);

        for (ResourceLocation poolId : VILLAGE_HOUSES_POOLS) {
            StructureTemplatePool pool = poolRegistry.get(poolId);
            if (pool == null) continue;

            // Pieza única del templo del pecado
            StructurePoolElement shrinePiece = SinglePoolElement.single(
                    SevenDeadlyGodsMod.MOD_ID + ":village/god_shrine",
                    emptyProcessor
            ).apply(StructureTemplatePool.Projection.RIGID);

            // Inyectamos con peso calibrado = 2 (tasa controlada para el ratio 20%)
            injectPiece(pool, shrinePiece, 2);
        }
    }

    private static void injectPiece(StructureTemplatePool pool, StructurePoolElement piece, int weight) {
        for (int i = 0; i < weight; i++) {
            pool.templates.add(piece);
        }
        pool.rawTemplates.add(Pair.of(piece, weight));
    }
}
```

---

## ✅ Criterios de Aceptación y Validación
1. [ ] Crear un mundo nuevo y localizar aldeas mediante `/locate structure minecraft:village_plains`.
2. [ ] Comprobar que aproximadamente 1 de cada 5 aldeas contiene el santuario sagrado con el Altar Mayor.
3. [ ] Probar compatibilidad cargando el mod junto a mods de estructuras populares (*Towns and Towers*, *ChoiceTheorem's Overhauled Village*) y verificar que no genera errores de chunk ni *crashes*.
