package com.volcov.sevendeadlygods.worldgen;

import com.mojang.datafixers.util.Pair;
import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.god.SinGod;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Inyecta los Templos Consagrados de los 7 Dioses del Pecado en las piscinas de plantillas
 * Jigsaw de las aldeas vanilla (plains, desert, savanna, taiga, snowy) manteniendo una
 * probabilidad aproximada del 20% por aldea generada.
 */
public final class VillageStructureInjector {
    private static final List<Identifier> VILLAGE_HOUSES_POOLS = List.of(
            Identifier.withDefaultNamespace("village/plains/houses"),
            Identifier.withDefaultNamespace("village/desert/houses"),
            Identifier.withDefaultNamespace("village/savanna/houses"),
            Identifier.withDefaultNamespace("village/taiga/houses"),
            Identifier.withDefaultNamespace("village/snowy/houses")
    );

    private static Field rawTemplatesField;
    private static Field templatesField;
    private static Field maxSizeField;

    static {
        try {
            rawTemplatesField = StructureTemplatePool.class.getDeclaredField("rawTemplates");
            rawTemplatesField.setAccessible(true);
            templatesField = StructureTemplatePool.class.getDeclaredField("templates");
            templatesField.setAccessible(true);
            maxSizeField = StructureTemplatePool.class.getDeclaredField("maxSize");
            maxSizeField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            SevenDeadlyGodsMod.LOGGER.error("No se pudieron localizar los campos de StructureTemplatePool para inyección Jigsaw", e);
        }
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (rawTemplatesField == null || templatesField == null) {
            return;
        }

        ConsecratedVillageShrineElement.clearCache();

        MinecraftServer server = event.getServer();
        Registry<StructureTemplatePool> poolRegistry = server.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        Registry<StructureProcessorList> processorRegistry = server.registryAccess().lookupOrThrow(Registries.PROCESSOR_LIST);
        ResourceKey<StructureProcessorList> emptyKey = ResourceKey.create(Registries.PROCESSOR_LIST, Identifier.withDefaultNamespace("empty"));
        Holder<StructureProcessorList> emptyProcessors = processorRegistry.getOrThrow(emptyKey);

        int injectedPools = 0;
        for (Identifier poolId : VILLAGE_HOUSES_POOLS) {
            StructureTemplatePool pool = poolRegistry.getValue(poolId);
            if (pool == null) {
                continue;
            }

            if (injectGodShrinesIntoPool(pool, emptyProcessors)) {
                injectedPools++;
            }
        }

        SevenDeadlyGodsMod.LOGGER.info(
                "Templos de los 7 Pecados Capitales inyectados en {} piscinas Jigsaw de aldeas (tirada única 20% por aldea).",
                injectedPools
        );
    }

    @SuppressWarnings("unchecked")
    private boolean injectGodShrinesIntoPool(
            StructureTemplatePool pool,
            Holder<StructureProcessorList> emptyProcessors
    ) {
        try {
            List<Pair<StructurePoolElement, Integer>> currentRaw =
                    (List<Pair<StructurePoolElement, Integer>>) rawTemplatesField.get(pool);
            ObjectArrayList<StructurePoolElement> currentTemplates =
                    (ObjectArrayList<StructurePoolElement>) templatesField.get(pool);

            // Evitar doble inyección si el pool ya contiene nuestros templos
            for (Pair<StructurePoolElement, Integer> pair : currentRaw) {
                if (pair.getFirst() instanceof ConsecratedVillageShrineElement
                        || (pair.getFirst() instanceof SinglePoolElement single
                        && single.toString().contains(SevenDeadlyGodsMod.MOD_ID + ":village/"))) {
                    return false;
                }
            }

            List<Pair<StructurePoolElement, Integer>> mutableRaw = new ArrayList<>(currentRaw);

            // Inyectar elementos con tirada única por aldea y límite de 1 santuario
            for (SinGod god : SinGod.values()) {
                Identifier templateId = Identifier.fromNamespaceAndPath(SevenDeadlyGodsMod.MOD_ID, "village/" + god.getId() + "_shrine");
                ConsecratedVillageShrineElement piece = new ConsecratedVillageShrineElement(
                        god,
                        templateId,
                        emptyProcessors,
                        StructureTemplatePool.Projection.RIGID
                );

                int weight = 2;
                mutableRaw.add(Pair.of(piece, weight));
                for (int i = 0; i < weight; i++) {
                    currentTemplates.add(piece);
                }
            }

            rawTemplatesField.set(pool, mutableRaw);
            if (maxSizeField != null) {
                maxSizeField.setInt(pool, Integer.MIN_VALUE);
            }
            return true;
        } catch (IllegalAccessException e) {
            SevenDeadlyGodsMod.LOGGER.error("Error al inyectar templos divinos en StructureTemplatePool", e);
            return false;
        }
    }
}
