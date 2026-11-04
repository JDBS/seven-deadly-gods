package com.volcov.sevendeadlygods.worldgen;

import com.mojang.datafixers.util.Either;
import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.config.ModConfig;
import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Elemento de piscina Jigsaw que garantiza:
 * 1. Una sola tirada de consagración del 80% por aldea (indexada por el inicio real de la estructura).
 * 2. Un único Dios del Pecado elegido por aldea consagrada.
 * 3. Exactamente un único santuario físico colocado en la aldea (reserva del slot solo tras aceptación).
 */
public class ConsecratedVillageShrineElement extends SinglePoolElement {
    private final SinGod god;

    private static final double MIN_VILLAGE_DISTANCE = 160.0D;
    private static final int MAX_CACHE_ENTRIES = 2048;

    public record VillageDecision(SinGod chosenGod, boolean consecrated, AtomicBoolean placed) {}

    private static final Map<Long, VillageDecision> VILLAGE_DECISIONS = Collections.synchronizedMap(
            new LinkedHashMap<>(128, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, VillageDecision> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            }
    );

    private static final ThreadLocal<VillageDecision> ACTIVE_VILLAGE = new ThreadLocal<>();

    public ConsecratedVillageShrineElement(
            SinGod god,
            Identifier templateId,
            Holder<StructureProcessorList> processors,
            StructureTemplatePool.Projection projection
    ) {
        super(Either.left(templateId), processors, projection, Optional.empty());
        this.god = god;
    }

    public SinGod getGod() {
        return this.god;
    }

    public static void clearCache() {
        VILLAGE_DECISIONS.clear();
        ACTIVE_VILLAGE.remove();
    }

    public static VillageDecision setActiveVillageStart(BlockPos startPos, RandomSource random) {
        VillageDecision decision = getOrCreateDecision(startPos, random);
        ACTIVE_VILLAGE.set(decision);
        return decision;
    }

    public static VillageDecision getOrCreateDecision(BlockPos pos, RandomSource random) {
        synchronized (VILLAGE_DECISIONS) {
            for (Map.Entry<Long, VillageDecision> entry : VILLAGE_DECISIONS.entrySet()) {
                BlockPos decisionPos = BlockPos.of(entry.getKey());
                if (decisionPos.closerThan(pos, MIN_VILLAGE_DISTANCE)) {
                    return entry.getValue();
                }
            }

            double chance = 0.80D;
            try {
                if (ModConfig.SPEC.isLoaded() && ModConfig.VILLAGE_CONSECRATION_CHANCE != null) {
                    chance = ModConfig.VILLAGE_CONSECRATION_CHANCE.get();
                }
            } catch (Exception ignored) {
            }

            // Tirada única del 80% por aldea indexada por el inicio real de la estructura (20% vanilla)
            boolean consecrated = random.nextDouble() < chance;
            SinGod chosenGod = SinGod.values()[random.nextInt(SinGod.values().length)];
            VillageDecision decision = new VillageDecision(chosenGod, consecrated, new AtomicBoolean(false));

            if (consecrated) {
                SevenDeadlyGodsMod.LOGGER.info("Aldea en [{}] consagrada a {} (Tirada exitosa: {:.0f}%). Buscando conector Jigsaw para santuario...",
                        pos.toShortString(), chosenGod.getDisplayName(), chance * 100.0);
            } else {
                SevenDeadlyGodsMod.LOGGER.debug("Aldea en [{}] permanecera vanilla ordinaria (Tirada de consagración no superada).",
                        pos.toShortString());
            }

            VILLAGE_DECISIONS.put(pos.asLong(), decision);
            return decision;
        }
    }

    @Override
    public int getGroundLevelDelta() {
        // En JigsawPlacement, getGroundLevelDelta() se invoca de forma exclusiva cuando la pieza
        // candidata ha superado las comprobaciones de colisión de Shapes.joinIsNotEmpty y es
        // formalmente aceptada dentro de la lista de piezas de la estructura (pieces.add).
        VillageDecision decision = ACTIVE_VILLAGE.get();
        if (decision != null && decision.placed().compareAndSet(false, true)) {
            SevenDeadlyGodsMod.LOGGER.info("Santuario de {} colocado exitosamente en la aldea (patrón: {}).",
                    this.god.getDisplayName(), decision.chosenGod().getDisplayName());
        }
        return super.getGroundLevelDelta();
    }

    @Override
    public List<StructureTemplate.JigsawBlockInfo> getShuffledJigsawBlocks(
            StructureTemplateManager templateManager,
            BlockPos pos,
            Rotation rotation,
            RandomSource random
    ) {
        // Si no es ZERO, la pieza ya fue aceptada y se está procesando como origen en la cola de jigsaw
        if (!pos.equals(BlockPos.ZERO)) {
            VillageDecision active = ACTIVE_VILLAGE.get();
            if (active != null && active.placed().compareAndSet(false, true)) {
                SevenDeadlyGodsMod.LOGGER.info("Santuario de {} colocado exitosamente en la aldea en [{}] (patrón: {}).",
                        this.god.getDisplayName(), pos.toShortString(), active.chosenGod().getDisplayName());
            }
            return super.getShuffledJigsawBlocks(templateManager, pos, rotation, random);
        }

        // Obtener la decisión contextualizada por el inicio real de la aldea
        VillageDecision decision = ACTIVE_VILLAGE.get();
        if (decision == null) {
            // Fallback para contextos sin inicio de aldea registrado
            return Collections.emptyList();
        }

        // 1. Si la aldea no fue elegida para consagración (20% de las aldeas vanilla)
        if (!decision.consecrated()) {
            return Collections.emptyList();
        }

        // 2. Si la aldea fue consagrada para un dios distinto al de esta pieza
        if (decision.chosenGod() != this.god) {
            return Collections.emptyList();
        }

        // 3. Si ya se colocó un santuario en esta aldea (límite estricto de 1 santuario)
        if (decision.placed().get()) {
            return Collections.emptyList();
        }

        // Proporcionamos los conectores para que Jigsaw pruebe attachment y colisiones
        // sin bloquear preventivamente la aldea ante un posible fallo de colisión posterior.
        return super.getShuffledJigsawBlocks(templateManager, pos, rotation, random);
    }

    @Override
    public String toString() {
        return "ConsecratedVillageShrineElement[" + this.god.getId() + "]";
    }
}
