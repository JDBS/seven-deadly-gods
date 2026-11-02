package com.volcov.sevendeadlygods.worldgen;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

/**
 * Envoltorio para los elementos de inicio de aldea (town_centers).
 * Intercepta la coordenada de inicio real de la aldea en el mundo cuando Jigsaw
 * procesa el centro de la aldea, permitiendo contextualizar todas las decisiones
 * de consagración y templo único de dicha aldea.
 */
public class TownCenterTrackerElement extends SinglePoolElement {
    private static Field templateField;
    private static Field processorsField;
    private static Field overrideLiquidSettingsField;

    static {
        try {
            templateField = SinglePoolElement.class.getDeclaredField("template");
            templateField.setAccessible(true);
            processorsField = SinglePoolElement.class.getDeclaredField("processors");
            processorsField.setAccessible(true);
            overrideLiquidSettingsField = SinglePoolElement.class.getDeclaredField("overrideLiquidSettings");
            overrideLiquidSettingsField.setAccessible(true);
        } catch (NoSuchFieldException ignored) {
        }
    }

    public TownCenterTrackerElement(SinglePoolElement original) {
        super(
                extractTemplate(original),
                extractProcessors(original),
                original.getProjection(),
                extractOverrideLiquidSettings(original)
        );
    }

    @SuppressWarnings("unchecked")
    private static Either<Identifier, StructureTemplate> extractTemplate(SinglePoolElement original) {
        if (templateField != null) {
            try {
                return (Either<Identifier, StructureTemplate>) templateField.get(original);
            } catch (IllegalAccessException ignored) {
            }
        }
        return Either.left(Identifier.withDefaultNamespace("empty"));
    }

    @SuppressWarnings("unchecked")
    private static Holder<StructureProcessorList> extractProcessors(SinglePoolElement original) {
        if (processorsField != null) {
            try {
                return (Holder<StructureProcessorList>) processorsField.get(original);
            } catch (IllegalAccessException ignored) {
            }
        }
        throw new IllegalStateException("No se pudo obtener processors de SinglePoolElement");
    }

    @SuppressWarnings("unchecked")
    private static Optional<LiquidSettings> extractOverrideLiquidSettings(SinglePoolElement original) {
        if (overrideLiquidSettingsField != null) {
            try {
                return (Optional<LiquidSettings>) overrideLiquidSettingsField.get(original);
            } catch (IllegalAccessException ignored) {
            }
        }
        return Optional.empty();
    }

    @Override
    public List<StructureTemplate.JigsawBlockInfo> getShuffledJigsawBlocks(
            StructureTemplateManager templateManager,
            BlockPos pos,
            Rotation rotation,
            RandomSource random
    ) {
        if (!pos.equals(BlockPos.ZERO)) {
            ConsecratedVillageShrineElement.setActiveVillageStart(pos, random);
        }
        return super.getShuffledJigsawBlocks(templateManager, pos, rotation, random);
    }

    @Override
    public String toString() {
        return "TownCenterTrackerElement[" + this.template + "]";
    }
}
