# Paso 7: Desolación, Caravana de Peregrinos y Repoblación

Este paso implementa el ciclo de vida ante la pérdida total de aldeanos (0 civiles), evitando la muerte definitiva de poblados mediante la **Caravana de Peregrinos** (tras 2 días de Minecraft de luto) y el **Milagro de Restauración** acelerado por ofrenda del jugador.

---

## 🎯 Objetivos del Paso
1. Detectar si una aldea asediada se queda sin aldeanos civiles vivos y transicionar su estado a `DESOLADA`.
2. Apagar el fuego sagrado del Altar Mayor y emitir partículas de humo negro y chispas moribundas.
3. Evaluar el paso de 48,000 ticks (2 días de Minecraft) para spawnear la **Caravana de Peregrinos** (3 aldeanos devotos + 1 acólito centinela) que reencienden el templo.
4. Permitir que un jugador devoto (`Favor >= +30`) revitalice el templo de inmediato depositando una ofrenda de restauración.

---

## 📁 Archivos Involucrados

| Tipo | Ruta del Archivo | Descripción |
| :--- | :--- | :--- |
| **Persistencia** | `src/main/java/com/volcov/sevendeadlygods/village/GodVillageSavedData.java` | Actualización de flag `DESOLADA` y tick de inicio. |
| **Gestor** | `src/main/java/com/volcov/sevendeadlygods/village/GodVillageRaidManager.java` | Bucle de chequeo y spawneo de la caravana. |
| **Bloque** | `src/main/java/com/volcov/sevendeadlygods/block/SinAltarBlockEntity.java` | Lógica de ofrenda de reanimación inmediata. |

---

## 💻 Implementación Técnica

### 7.1 Chequeo de Repoblación en `GodVillageRaidManager.java`
```java
public static void checkRepopulation(MinecraftServer server, ServerLevel level, GodVillageSavedData data, GodVillageSavedData.GodVillageRecord village) {
    if (village.state() != GodVillageState.DESOLADA) {
        return;
    }

    // Verificar si pasaron 48,000 ticks (2 días de Minecraft) desde la desolación
    long elapsedTicks = level.getGameTime() - village.stateSinceTick();
    if (elapsedTicks >= 48000L) {
        // 1. Spawneo de Caravana de Peregrinos
        spawnPilgrimCaravan(level, village.altarPos(), village.patronGod(), 3);

        // 2. Restaurar estado a CONSAGRADA
        data.updateVillage(new GodVillageSavedData.GodVillageRecord(
                village.id(),
                village.altarPos(),
                village.patronGod(),
                village.radius(),
                village.lastRaidTick(),
                GodVillageState.CONSAGRADA,
                level.getGameTime()
        ));

        // 3. Anuncio solemne en el chat
        Component msg = Component.literal("✨ ¡EL LLAMADO DE LOS FIELES! ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("Nuevos devotos llegan en peregrinación para reavivar el Templo de ").withStyle(ChatFormatting.WHITE))
                .append(village.patronGod().getDisplayName().copy().withStyle(village.patronGod().getColor(), ChatFormatting.BOLD))
                .append(Component.literal("!").withStyle(ChatFormatting.WHITE));

        server.getPlayerList().broadcastSystemMessage(msg, false);
    }
}

private static void spawnPilgrimCaravan(ServerLevel level, BlockPos altarPos, SinGod god, int count) {
    for (int i = 0; i < count; i++) {
        Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.EVENT);
        if (villager != null) {
            villager.snapTo(altarPos.getX() + (i * 2) - 2, altarPos.getY(), altarPos.getZ() + (i * 2) - 2, 0, 0);
            level.addFreshEntity(villager);
        }
    }
    // 1 Acólito centinela de escolta
    GodAcolyteEntity guardian = ModEntities.GOD_ACOLYTE.get().create(level, EntitySpawnReason.EVENT);
    if (guardian != null) {
        guardian.snapTo(altarPos.getX() + 0.5D, altarPos.getY(), altarPos.getZ() + 0.5D, 0, 0);
        guardian.configureAcolyte(level, god, null, false);
        level.addFreshEntity(guardian);
    }
}
```

### 7.2 Ofrenda de Restauración en `SinAltarBlockEntity.java`
Cuando el altar está en estado durmiente y un jugador fiel interactúa con una ofrenda:
```java
public boolean tryRevitalizeDesolatedVillage(ServerPlayer player, ItemStack offering) {
    if (!(level instanceof ServerLevel serverLevel)) return false;

    GodVillageSavedData data = GodVillageSavedData.get(serverLevel);
    Optional<GodVillageSavedData.GodVillageRecord> opt = data.getVillageAt(this.worldPosition);
    if (opt.isEmpty() || opt.get().state() != GodVillageState.DESOLADA) return false;

    SinGod god = opt.get().patronGod();
    int favor = SinFavorManager.getFavor(player, god);

    if (favor >= 30 && isValidRevitalizationOffering(god, offering)) {
        offering.shrink(1);
        GodVillageRaidManager.spawnPilgrimCaravan(serverLevel, this.worldPosition, god, 3);
        
        data.updateVillage(new GodVillageSavedData.GodVillageRecord(
                opt.get().id(), opt.get().altarPos(), god, opt.get().radius(),
                opt.get().lastRaidTick(), GodVillageState.CONSAGRADA, serverLevel.getGameTime()
        ));

        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, 40, 0.5, 0.5, 0.5, 0.1);
        serverLevel.playSound(null, worldPosition, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }
    return false;
}
```

---

## ✅ Criterios de Aceptación y Validación
1. [ ] Eliminar a todos los aldeanos de una aldea consagrada; confirmar que el estado pasa a `DESOLADA` y el altar emite partículas de humo negro.
2. [ ] Avanzar el tiempo 48,000 ticks (`/time add 48000`); verificar que llega la Caravana de Peregrinos y la aldea vuelve a estar activa.
3. [ ] Probar la restauración manual con un jugador devoto portando una ofrenda sagrada y verificar la reactivación instantánea.
