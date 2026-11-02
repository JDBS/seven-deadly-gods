package com.volcov.sevendeadlygods.worldgen;

import com.mojang.datafixers.util.Either;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Elemento de piscina Jigsaw que garantiza:
 * 1. Una sola tirada de consagración del 20% por aldea (independiente de los pesos vanilla de bioma).
 * 2. Un único Dios del Pecado elegido por aldea consagrada.
 * 3. Exactamente un único santuario físico colocado en la aldea (sin duplicados).
 */
public class ConsecratedVillageShrineElement extends SinglePoolElement {
    private final SinGod god;

    private static final double MIN_VILLAGE_DISTANCE = 160.0D;
    private static final int MAX_CACHE_ENTRIES = 2048;

    private record VillageDecision(SinGod chosenGod, boolean consecrated, AtomicBoolean placed) {}

    private static final Map<Long, VillageDecision> VILLAGE_DECISIONS = Collections.synchronizedMap(
            new LinkedHashMap<>(128, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, VillageDecision> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            }
    );

    public ConsecratedVillageShrineElement(
            SinGod god,
            Identifier templateId,
            Holder<StructureProcessorList> processors,
            StructureTemplatePool.Projection projection
    ) {
        super(Either.left(templateId), processors, projection);
        this.god = god;
    }

    public SinGod getGod() {
        return this.god;
    }

    public static void clearCache() {
        VILLAGE_DECISIONS.clear();
    }

    private static VillageDecision getOrCreateDecision(BlockPos pos, RandomSource random) {
        synchronized (VILLAGE_DECISIONS) {
            for (Map.Entry<Long, VillageDecision> entry : VILLAGE_DECISIONS.entrySet()) {
                BlockPos decisionPos = BlockPos.of(entry.getKey());
                if (decisionPos.closerThan(pos, MIN_VILLAGE_DISTANCE)) {
                    return entry.getValue();
                }
            }

            // Tirada única del 20% por aldea (independiente del número de casas o bioma)
            boolean consecrated = random.nextFloat() < 0.20F;
            SinGod chosenGod = SinGod.values()[random.nextInt(SinGod.values().length)];
            VillageDecision decision = new VillageDecision(chosenGod, consecrated, new AtomicBoolean(false));

            VILLAGE_DECISIONS.put(pos.asLong(), decision);
            return decision;
        }
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(
            StructureTemplateManager templateManager,
            BlockPos pos,
            Rotation rotation,
            RandomSource random
    ) {
        VillageDecision decision = getOrCreateDecision(pos, random);

        // 1. Si la aldea no fue elegida para consagración (80% de las aldeas vanilla)
        if (!decision.consecrated()) {
            return Collections.emptyList();
        }

        // 2. Si la aldea fue consagrada para un dios distinto al de esta pieza
        if (decision.chosenGod() != this.god) {
            return Collections.emptyList();
        }

        // 3. Si ya se colocó un santuario en esta aldea (límite estricto de 1 santuario por aldea)
        if (decision.placed().get()) {
            return Collections.emptyList();
        }

        // 4. Intentar reservar la colocación atómicamente:
        if (!decision.placed().compareAndSet(false, true)) {
            return Collections.emptyList();
        }

        return super.getShuffledJigsawBlocks(templateManager, pos, rotation, random);
    }

    @Override
    public String toString() {
        return "ConsecratedVillageShrineElement[" + this.god.getId() + "]";
    }
}
