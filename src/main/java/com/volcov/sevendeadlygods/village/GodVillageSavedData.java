package com.volcov.sevendeadlygods.village;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia global a nivel de dimensión (ServerLevel.getDataStorage()) para todas
 * las Aldeas Consagradas, en Asedio, Desoladas o Arrasadas.
 */
public class GodVillageSavedData extends SavedData {
    private static final int DEFAULT_VILLAGE_RADIUS = 48;
    private static final double MIN_ALTAR_SEPARATION = 128.0D;

    public record GodVillageRecord(
            UUID id,
            BlockPos altarPos,
            SinGod patronGod,
            int radius,
            long lastRaidTick,
            GodVillageState state,
            long stateSinceTick,
            boolean cultureInitialized
    ) {
        public static final Codec<GodVillageRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(GodVillageRecord::id),
                BlockPos.CODEC.fieldOf("altar_pos").forGetter(GodVillageRecord::altarPos),
                Codec.STRING.fieldOf("god").forGetter(r -> r.patronGod().getId()),
                Codec.INT.optionalFieldOf("radius", DEFAULT_VILLAGE_RADIUS).forGetter(GodVillageRecord::radius),
                Codec.LONG.optionalFieldOf("last_raid", 0L).forGetter(GodVillageRecord::lastRaidTick),
                Codec.STRING.optionalFieldOf("state", GodVillageState.CONSAGRADA.name()).forGetter(r -> r.state().name()),
                Codec.LONG.optionalFieldOf("state_since", 0L).forGetter(GodVillageRecord::stateSinceTick),
                Codec.BOOL.optionalFieldOf("culture_initialized", false).forGetter(GodVillageRecord::cultureInitialized)
        ).apply(instance, (id, altarPos, godId, radius, lastRaid, stateName, stateSince, cultureInit) -> {
            SinGod god = SinGod.byId(godId).orElse(SinGod.WRATH);
            GodVillageState parsedState;
            try {
                parsedState = GodVillageState.valueOf(stateName);
            } catch (IllegalArgumentException ex) {
                parsedState = god == SinGod.ENVY ? GodVillageState.ARRASADA : GodVillageState.CONSAGRADA;
            }
            return new GodVillageRecord(id, altarPos, god, radius, lastRaid, parsedState, stateSince, cultureInit);
        }));

        public GodVillageRecord(
                UUID id,
                BlockPos altarPos,
                SinGod patronGod,
                int radius,
                long lastRaidTick,
                GodVillageState state,
                long stateSinceTick
        ) {
            this(id, altarPos, patronGod, radius, lastRaidTick, state, stateSinceTick, false);
        }

        public GodVillageRecord withState(GodVillageState newState, long tick) {
            return new GodVillageRecord(id, altarPos, patronGod, radius, lastRaidTick, newState, tick, cultureInitialized);
        }

        public GodVillageRecord withLastRaidTick(long raidTick, GodVillageState newState, long stateTick) {
            return new GodVillageRecord(id, altarPos, patronGod, radius, raidTick, newState, stateTick, cultureInitialized);
        }

        public GodVillageRecord withPatronGod(SinGod newGod, GodVillageState newState, long tick) {
            return new GodVillageRecord(id, altarPos, newGod, radius, lastRaidTick, newState, tick, false);
        }

        public GodVillageRecord withCultureInitialized(boolean initialized) {
            return new GodVillageRecord(id, altarPos, patronGod, radius, lastRaidTick, state, stateSinceTick, initialized);
        }
    }

    public static final Codec<GodVillageSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GodVillageRecord.CODEC.listOf()
                    .optionalFieldOf("villages", List.of())
                    .forGetter(data -> new ArrayList<>(data.villages.values()))
    ).apply(instance, GodVillageSavedData::new));

    public static final SavedDataType<GodVillageSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SevenDeadlyGodsMod.MOD_ID, "villages"),
            GodVillageSavedData::new,
            CODEC
    );

    private final Map<UUID, GodVillageRecord> villages = new LinkedHashMap<>();

    public GodVillageSavedData() {
    }

    private GodVillageSavedData(List<GodVillageRecord> records) {
        for (GodVillageRecord record : records) {
            this.villages.put(record.id(), record);
        }
    }

    public static GodVillageSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Registra un santuario de aldea si no existe ya otra aldea consagrada en un radio de 128 bloques.
     * Evita que altares secundarios colocados por jugadores sobrescriban el centro o el dios patrón.
     */
    public boolean registerVillageIfAbsent(BlockPos altarPos, SinGod god) {
        return registerVillageIfAbsent(altarPos, god, 0L);
    }

    public boolean registerVillageIfAbsent(BlockPos altarPos, SinGod god, long currentTick) {
        for (GodVillageRecord record : villages.values()) {
            if (record.altarPos().closerThan(altarPos, MIN_ALTAR_SEPARATION)) {
                return false;
            }
        }
        UUID id = UUID.randomUUID();
        GodVillageState initialState = god == SinGod.ENVY ? GodVillageState.ARRASADA : GodVillageState.CONSAGRADA;
        villages.put(id, new GodVillageRecord(
                id,
                altarPos.immutable(),
                god,
                DEFAULT_VILLAGE_RADIUS,
                0L,
                initialState,
                currentTick,
                false
        ));
        setDirty();
        return true;
    }

    /**
     * Consagra o reconvierte forzosamente una aldea en la posición indicada (usado tras Guerras Santas o comandos).
     */
    public GodVillageRecord consecrateOrUpdateVillage(BlockPos altarPos, SinGod god, GodVillageState state, long currentTick) {
        for (GodVillageRecord existing : villages.values()) {
            if (existing.altarPos().closerThan(altarPos, MIN_ALTAR_SEPARATION)) {
                GodVillageRecord updated = new GodVillageRecord(
                        existing.id(),
                        altarPos.immutable(),
                        god,
                        existing.radius(),
                        existing.lastRaidTick(),
                        state,
                        currentTick,
                        false
                );
                villages.put(existing.id(), updated);
                setDirty();
                return updated;
            }
        }
        UUID id = UUID.randomUUID();
        GodVillageRecord created = new GodVillageRecord(
                id,
                altarPos.immutable(),
                god,
                DEFAULT_VILLAGE_RADIUS,
                0L,
                state,
                currentTick,
                false
        );
        villages.put(id, created);
        setDirty();
        return created;
    }

    public Optional<GodVillageRecord> getVillageAt(BlockPos pos) {
        GodVillageRecord closest = null;
        double bestDistSq = Double.MAX_VALUE;
        for (GodVillageRecord record : villages.values()) {
            if (record.altarPos().closerThan(pos, record.radius())) {
                double distSq = record.altarPos().distSqr(pos);
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    closest = record;
                }
            }
        }
        return Optional.ofNullable(closest);
    }

    public Optional<GodVillageRecord> findVillageAt(BlockPos pos) {
        return getVillageAt(pos);
    }

    public Optional<GodVillageRecord> getVillageById(UUID id) {
        return Optional.ofNullable(villages.get(id));
    }

    public boolean isCanonicalVillageAltar(BlockPos pos) {
        for (GodVillageRecord record : villages.values()) {
            if (record.altarPos().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    public Collection<GodVillageRecord> getAllVillages() {
        return Collections.unmodifiableCollection(villages.values());
    }

    public void updateVillage(GodVillageRecord updated) {
        villages.put(updated.id(), updated);
        setDirty();
    }

    public void removeVillage(UUID id) {
        if (villages.remove(id) != null) {
            setDirty();
        }
    }
}
