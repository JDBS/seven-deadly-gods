# Paso 2: Persistencia y Gestión Territorial (`GodVillageSavedData`)

Este paso implementa el subsistema de persistencia centralizado a nivel de dimensión (`ServerLevel.getDataStorage()`), evitando la dependencia de chunks individuales y garantizando que el estado de cada aldea se conserve fielmente entre reinicios del servidor.

---

## 🎯 Objetivos del Paso
1. Crear `GodVillageSavedData` para registrar todas las aldeas consagradas del mundo.
2. Definir la máquina de estados de cada poblado: `MUNDANA`, `CONSAGRADA`, `EN_ASEDIO`, `DESOLADA` y `ARRASADA`.
3. Conectar el `BlockEntity` del Altar Mayor para que, al generarse o colocarse en el centro del poblado, registre la aldea automáticamente evitando duplicados.

---

## 📁 Archivos Involucrados

| Tipo | Ruta del Archivo | Descripción |
| :--- | :--- | :--- |
| **Código Java** | `src/main/java/com/volcov/sevendeadlygods/village/GodVillageState.java` | Enumeración con los estados de vida de una aldea. |
| **Código Java** | `src/main/java/com/volcov/sevendeadlygods/village/GodVillageSavedData.java` | Gestor `SavedData` serializable en NBT. |
| **Integración** | `src/main/java/com/volcov/sevendeadlygods/block/SinAltarBlockEntity.java` | Hook de auto-registro al inicializarse en el mundo. |

---

## 💻 Implementación Técnica

### 2.1 Enumeración de Estados (`GodVillageState.java`)
```java
package com.volcov.sevendeadlygods.village;

public enum GodVillageState {
    MUNDANA,      // Aldea normal civil sin culto
    CONSAGRADA,   // Aldea consagrada activa con templo y milicia
    EN_ASEDIO,    // Asedio o guerra santa activa
    DESOLADA,     // 0 aldeanos vivos; esperando peregrinos o restauración
    ARRASADA      // Ruinas quemadas de Leviatán
}
```

### 2.2 Gestor Persistente (`GodVillageSavedData.java`)
```java
package com.volcov.sevendeadlygods.village;

import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class GodVillageSavedData extends SavedData {
    private static final String DATA_NAME = "sevendeadlygods_villages";
    private final Map<UUID, GodVillageRecord> villages = new HashMap<>();

    public record GodVillageRecord(
            UUID id,
            BlockPos altarPos,
            SinGod patronGod,
            int radius,
            long lastRaidTick,
            GodVillageState state,
            long stateSinceTick
    ) {
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", id);
            tag.putLong("altar_pos", altarPos.asLong());
            tag.putString("god", patronGod.getId());
            tag.putInt("radius", radius);
            tag.putLong("last_raid", lastRaidTick);
            tag.putString("state", state.name());
            tag.putLong("state_since", stateSinceTick);
            return tag;
        }

        public static GodVillageRecord load(CompoundTag tag) {
            GodVillageState st;
            try {
                st = GodVillageState.valueOf(tag.getString("state"));
            } catch (Exception e) {
                st = GodVillageState.CONSAGRADA;
            }
            return new GodVillageRecord(
                    tag.getUUID("id"),
                    BlockPos.of(tag.getLong("altar_pos")),
                    SinGod.byId(tag.getString("god")).orElse(SinGod.WRATH),
                    tag.getInt("radius"),
                    tag.getLong("last_raid"),
                    st,
                    tag.getLong("state_since")
            );
        }
    }

    public static GodVillageSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(GodVillageSavedData::new, GodVillageSavedData::load),
                DATA_NAME
        );
    }

    public boolean registerVillageIfAbsent(BlockPos altarPos, SinGod god) {
        // Evita registrar tótems o altares duplicados en la misma aldea (radio 128 bloques)
        for (GodVillageRecord record : villages.values()) {
            if (record.altarPos().closerThan(altarPos, 128.0D)) {
                return false;
            }
        }
        UUID id = UUID.randomUUID();
        villages.put(id, new GodVillageRecord(id, altarPos, god, 48, 0L, GodVillageState.CONSAGRADA, 0L));
        setDirty();
        return true;
    }

    public Optional<GodVillageRecord> getVillageAt(BlockPos pos) {
        for (GodVillageRecord record : villages.values()) {
            if (record.altarPos().closerThan(pos, record.radius())) {
                return Optional.of(record);
            }
        }
        return Optional.empty();
    }

    public Collection<GodVillageRecord> getAllVillages() {
        return Collections.unmodifiableCollection(villages.values());
    }

    public void updateVillage(GodVillageRecord updated) {
        villages.put(updated.id(), updated);
        setDirty();
    }

    public static GodVillageSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        GodVillageSavedData data = new GodVillageSavedData();
        ListTag list = tag.getList("villages", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            GodVillageRecord record = GodVillageRecord.load(list.getCompound(i));
            data.villages.put(record.id(), record);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (GodVillageRecord record : villages.values()) {
            list.add(record.save());
        }
        tag.put("villages", list);
        return tag;
    }
}
```

---

## ✅ Criterios de Aceptación y Validación
1. [ ] Cargar un mundo con un templo generado y verificar que se crea la entrada en `data/sevendeadlygods_villages.dat`.
2. [ ] Cerrar el servidor y volver a iniciar; verificar que las coordenadas y el dios patrón se cargan sin desincronización.
3. [ ] Intentar colocar un segundo altar a 20 bloques del templo central y confirmar que el segundo altar no sobrescribe los datos de la aldea.
