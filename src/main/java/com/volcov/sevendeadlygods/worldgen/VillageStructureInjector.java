package com.volcov.sevendeadlygods.worldgen;

import com.mojang.datafixers.util.Pair;
import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.config.ModConfig;
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
 * Jigsaw de las aldeas vanilla (plains, desert, savanna, taiga, snowy) y registra el rastreador
 * de inicio de aldea (town_centers) para garantizar:
 * 1. Decisión de consagración del 80% indexada por la posición de inicio real de la aldea (20% vanilla).
 * 2. Un único Dios del Pecado elegido por aldea consagrada.
 * 3. Exactamente un único santuario físico colocado por aldea con rollback si un intento colisiona.
 */
public final class VillageStructureInjector {
    private static final List<Identifier> VILLAGE_TOWN_CENTERS_POOLS = List.of(
            Identifier.withDefaultNamespace("village/plains/town_centers"),
            Identifier.withDefaultNamespace("village/desert/town_centers"),
            Identifier.withDefaultNamespace("village/savanna/town_centers"),
            Identifier.withDefaultNamespace("village/taiga/town_centers"),
            Identifier.withDefaultNamespace("village/snowy/town_centers")
    );

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

        // 1. Envolver los centros de aldea para capturar la posición de inicio real de la aldea
        int trackedTownCenters = 0;
        for (Identifier centerPoolId : VILLAGE_TOWN_CENTERS_POOLS) {
            StructureTemplatePool pool = poolRegistry.getValue(centerPoolId);
            if (pool != null && wrapTownCentersPool(pool)) {
                trackedTownCenters++;
            }
        }

        // 2. Inyectar templos de los 7 pecados en las casas de aldea
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
                "Templos de los 7 Pecados Capitales inyectados en {} piscinas de casas y rastreados en {} centros de aldeas.",
                injectedPools,
                trackedTownCenters
        );
    }

    @SuppressWarnings("unchecked")
    private boolean wrapTownCentersPool(StructureTemplatePool pool) {
        try {
            List<Pair<StructurePoolElement, Integer>> currentRaw =
                    (List<Pair<StructurePoolElement, Integer>>) rawTemplatesField.get(pool);

            boolean modified = false;
            List<Pair<StructurePoolElement, Integer>> newRaw = new ArrayList<>();
            ObjectArrayList<StructurePoolElement> newTemplates = new ObjectArrayList<>();

            for (Pair<StructurePoolElement, Integer> pair : currentRaw) {
                StructurePoolElement el = pair.getFirst();
                StructurePoolElement wrapped = el;
                if (el instanceof SinglePoolElement single && !(el instanceof TownCenterTrackerElement)) {
                    wrapped = new TownCenterTrackerElement(single);
                    modified = true;
                }
                newRaw.add(Pair.of(wrapped, pair.getSecond()));
                for (int i = 0; i < pair.getSecond(); i++) {
                    newTemplates.add(wrapped);
                }
            }

            if (modified) {
                rawTemplatesField.set(pool, newRaw);
                templatesField.set(pool, newTemplates);
                return true;
            }
            return false;
        } catch (IllegalAccessException e) {
            SevenDeadlyGodsMod.LOGGER.error("Error al envolver centros de aldea para tracking de inicio Jigsaw", e);
            return false;
        }
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

            int weight = 20;
            try {
                if (ModConfig.SPEC.isLoaded() && ModConfig.VILLAGE_SHRINE_JIGSAW_WEIGHT != null) {
                    weight = ModConfig.VILLAGE_SHRINE_JIGSAW_WEIGHT.get();
                }
            } catch (Exception ignored) {
            }

            // Inyectar elementos con tirada única por aldea y límite de 1 santuario
            for (SinGod god : SinGod.values()) {
                Identifier templateId = Identifier.fromNamespaceAndPath(SevenDeadlyGodsMod.MOD_ID, "village/" + god.getId() + "_shrine");
                ConsecratedVillageShrineElement piece = new ConsecratedVillageShrineElement(
                        god,
                        templateId,
                        emptyProcessors,
                        StructureTemplatePool.Projection.TERRAIN_MATCHING
                );

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
