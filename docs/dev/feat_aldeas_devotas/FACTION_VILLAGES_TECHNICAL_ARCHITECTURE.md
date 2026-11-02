# 🛠️ Arquitectura Técnica y Guía de Implementación (NeoForge 26.3)

> [!NOTE]
> **Referencia e Inspiración de Arquitectura**: La ingeniería de control territorial, anclaje de templos y asedios toma como punto de referencia el análisis técnico del repositorio **[TeamLapen/Vampirism](https://github.com/TeamLapen/Vampirism)**. Toda la implementación en este documento se diseña como una solución *Clean-Room* completamente original optimizada para **NeoForge 26.3**, eliminando los problemas históricos de desincronización y *tick lag* del referente.

Este documento contiene la especificación de ingeniería para programar el sistema de **Aldeas Consagradas, Templos, Acólitos Mercaderes y Asedios** en **Minecraft 26.3 con NeoForge 26.3**.

---

## 1. Visión General de Componentes

La arquitectura se divide en 5 subsistemas desacoplados y eficientes:

```
+-----------------------------------------------------------------------------------+
|                              ARQUITECTURA DEL SISTEMA                              |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  [ 1. WorldGen & Jigsaw Injection ]                                               |
|        │                                                                          |
|        ├──> Inyección de Template Pools (20% probabilidad)                        |
|        └──> Estructura del Templo (NBT) con Altar Central                         |
|                                                                                   |
|  [ 2. Persistencia: GodVillageSavedData ]                                         |
|        │                                                                          |
|        ├──> Registra poblados en ServerLevel.getDataStorage()                     |
|        └──> Mantiene Posición, Radio, Dios Patrón y Estado de Asedio              |
|                                                                                   |
|  [ 3. Entidades & IA: GodAcolyteEntity ]                                          |
|        │                                                                          |
|        ├──> TargetGoal: Favor <= -40  ──> Ataque inmediato                       |
|        ├──> Interact:  -39 <= F < 0   ──> Rechazo verbal (Villager No)            |
|        └──> Interact:  Favor >= 0     ──> Abre Interfaz de Comercio Divino        |
|                                                                                   |
|  [ 4. Gestor de Incursiones: GodVillageRaidManager ]                              |
|        │                                                                          |
|        ├──> Temporizador de asalto de cultos rivales                              |
|        ├──> Notificación en el Chat Global del Servidor                           |
|        └──> Tirada del 5% ──> Spawnea GodAvatarEntity (Jefe)                     |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

---

## 2. Inyección de Estructuras en Aldeas (Jigsaw & Template Pools)

En Minecraft moderno y NeoForge, las aldeas se ensamblan proceduralmente a través del sistema **Jigsaw** usando piscinas de plantillas (`StructureTemplatePool`).

### 2.1 Estrategia de Probabilidad del 20%
Para que exactamente el 20% de las aldeas generen un templo de culto:
1. **Inyección en `town_centers`**:
   Las aldeas eligen su pieza inicial desde el pool de centros de aldea:
   * `minecraft:village/plains/town_centers`
   * `minecraft:village/desert/town_centers`
   * `minecraft:village/taiga/town_centers`
   * `minecraft:village/savanna/town_centers`
   * `minecraft:village/snowy/town_centers`
2. **Ponderación de pesos**:
   Si una piscina de centro tiene piezas vanilla con peso total de **40**, agregar nuestra pieza `sevendeadlygods:village/god_temple_center` con peso **10** (o 1 de cada 5) garantiza matemáticamente una tasa de aparición del **20%**.
3. **Alternativa Robusta (Inyección en `houses`)**:
   Si se inyecta como una casa o santuario en el pool `houses`, se utiliza un **Structure Processor** o la lógica del `BlockEntity` del altar para que solo el primer altar generado en el radio del poblado reclame la aldea.

### 2.2 Inyección Programática Segura en NeoForge
Para evitar modificar archivos JSON vanilla directamente (lo cual puede crear incompatibilidades con otros mods de aldeas), se utiliza el evento de carga de datos en el servidor:

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

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = SevenDeadlyGodsMod.MOD_ID)
public class VillageStructureInjector {
    private static final ResourceKey<StructureProcessorList> EMPTY_PROCESSOR =
            ResourceKey.create(Registries.PROCESSOR_LIST, ResourceLocation.withDefaultNamespace("empty"));

    private static final List<ResourceLocation> VILLAGE_POOLS = List.of(
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

        for (ResourceLocation poolId : VILLAGE_POOLS) {
            StructureTemplatePool pool = poolRegistry.get(poolId);
            if (pool == null) continue;

            // Inyectar el templo del culto con peso calibrado
            StructurePoolElement piece = SinglePoolElement.single(
                    SevenDeadlyGodsMod.MOD_ID + ":village/god_shrine",
                    emptyProcessor
            ).apply(StructureTemplatePool.Projection.RIGID);

            // Añadir a rawTemplates para que el generador Jigsaw lo incluya
            // Nota: Se calcula el peso para mantener el ratio del 20%
            injectPieceIntoPool(pool, piece, 3);
        }
    }

    private static void injectPieceIntoPool(StructureTemplatePool pool, StructurePoolElement piece, int weight) {
        // En NeoForge / MC moderno se accede a rawTemplates y template (vía AccessTransformer o Reflection seguro)
        for (int i = 0; i < weight; i++) {
            pool.templates.add(piece);
        }
        pool.rawTemplates.add(Pair.of(piece, weight));
    }
}
```

---

## 3. Persistencia de Aldeas Consagradas (`GodVillageSavedData`)

Para no depender de que los chunks estén siempre cargados y evitar los bugs de pérdida de datos que sufría Vampirism, centralizamos el estado de las aldeas en un `SavedData` en `ServerLevel`:

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
            boolean isUnderRaid,
            boolean isDesolated,
            long desolatedSinceTick
    ) {
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", id);
            tag.putLong("altar_pos", altarPos.asLong());
            tag.putString("god", patronGod.getId());
            tag.putInt("radius", radius);
            tag.putLong("last_raid", lastRaidTick);
            tag.putBoolean("under_raid", isUnderRaid);
            tag.putBoolean("desolated", isDesolated);
            tag.putLong("desolated_since", desolatedSinceTick);
            return tag;
        }

        public static GodVillageRecord load(CompoundTag tag) {
            return new GodVillageRecord(
                    tag.getUUID("id"),
                    BlockPos.of(tag.getLong("altar_pos")),
                    SinGod.byId(tag.getString("god")).orElse(SinGod.WRATH),
                    tag.getInt("radius"),
                    tag.getLong("last_raid"),
                    tag.getBoolean("under_raid"),
                    tag.getBooleanOr("desolated", false),
                    tag.getLongOr("desolated_since", 0L)
            );
        }
    }

    public static GodVillageSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        GodVillageSavedData::new,
                        GodVillageSavedData::load
                ),
                DATA_NAME
        );
    }

    public void registerVillage(BlockPos altarPos, SinGod god) {
        // Evitar duplicados si ya hay una aldea registrada en este radio
        for (GodVillageRecord existing : villages.values()) {
            if (existing.altarPos().closerThan(altarPos, 128.0D)) {
                return;
            }
        }
        UUID id = UUID.randomUUID();
        villages.put(id, new GodVillageRecord(id, altarPos, god, 48, 0L, false, false, 0L));
        setDirty();
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

## 4. IA de Acólitos y Comercio Divino

Modificamos y extendemos el comportamiento de `GodAcolyteEntity` para implementar las reglas de hostilidad (`favor <= -40`) y comercio (`favor >= 0`).

### 4.1 TargetGoal para Hostilidad Inmediata (`Favor <= -40`)
En `GodAcolyteEntity.java`, dentro de `registerGoals()`:

```java
// Objetivo de ataque para jugadores con favor <= -40 (Herejes)
this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
        this,
        Player.class,
        10,
        true,
        false,
        (living, serverLevel) -> {
            if (living instanceof ServerPlayer serverPlayer) {
                int favor = SinFavorManager.getFavor(serverPlayer, this.getGod());
                return favor <= -40; // Hostilidad total
            }
            return false;
        }
));
```

### 4.2 Interacción del Jugador: Negación vs Comercio Divino
Sobrescribir `mobInteract` en `GodAcolyteEntity`:

```java
@Override
public InteractionResult mobInteract(Player player, InteractionHand hand) {
    if (hand != InteractionHand.MAIN_HAND) {
        return InteractionResult.PASS;
    }

    if (!(player instanceof ServerPlayer serverPlayer)) {
        return InteractionResult.SUCCESS;
    }

    SinGod god = this.getGod();
    int favor = SinFavorManager.getFavor(serverPlayer, god);

    // Caso 1: Hereje extremo (<= -40) -> No hay interacción, ataca
    if (favor <= -40) {
        this.setTarget(serverPlayer);
        return InteractionResult.CONSUME;
    }

    // Caso 2: Sospecha / Neutral (-39 a -1) -> Rechazo
    if (favor < 0) {
        this.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
        serverPlayer.sendSystemMessage(
                god.getFormattedPrefix().append(
                        Component.literal("«No tratamos con infieles. Purifica tus pecados ante el altar antes de hablarme.»")
                                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC)
                )
        );
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    // Caso 3: Devoto / Aliado (Favor >= 0) -> Abre el Comercio Divino
    openDivineTradingMenu(serverPlayer);
    return InteractionResult.sidedSuccess(this.level().isClientSide());
}
```

### 4.3 Generación de Ofertas de Comercio Divino
Implementando el sistema nativo de `Merchant` de Minecraft:

```java
private void openDivineTradingMenu(ServerPlayer player) {
    MerchantOffers offers = new MerchantOffers();
    SinGod god = this.getGod();

    switch (god) {
        case GREED -> {
            offers.add(new MerchantOffer(new ItemCost(Items.GOLD_INGOT, 8), new ItemStack(Items.EMERALD, 5), 16, 5, 0.05F));
            offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 32), new ItemStack(Items.DIAMOND, 4), 8, 10, 0.05F));
        }
        case WRATH -> {
            offers.add(new MerchantOffer(new ItemCost(Items.SKELETON_SKULL, 1), new ItemStack(Items.IRON_SWORD, 1), 12, 5, 0.05F));
            offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 20), new ItemStack(Items.STRENGTH_POTION, 2), 6, 8, 0.05F));
        }
        case GLUTTONY -> {
            offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 4), new ItemStack(Items.GOLDEN_CARROT, 8), 16, 4, 0.05F));
            offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 12), new ItemStack(Items.GOLDEN_APPLE, 2), 8, 8, 0.05F));
        }
        // ... Configuración específica para los 7 Dioses
        default -> {}
    }

    // Abre el contenedor Merchant sin necesidad de inventario custom
    this.openTradingScreen(player, god.getDisplayName().copy().append(" - Mercader del Templo"), 1);
}
```

---

## 5. Gestor de Incursiones y Manifestación Divina (`GodVillageRaidManager`)

El asedio a las aldeas y la tirada del **5% para invocar al Avatar** se ejecutan en este gestor:

```java
package com.volcov.sevendeadlygods.village;

import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.entity.GodAcolyteEntity;
import com.volcov.sevendeadlygods.entity.GodAvatarEntity;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.intervention.DivineInterventionScheduler;
import com.volcov.sevendeadlygods.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

@EventBusSubscriber(modid = SevenDeadlyGodsMod.MOD_ID)
public class GodVillageRaidManager {
    // Evaluar cada 1 minuto (1200 ticks)
    private static final int CHECK_INTERVAL_TICKS = 1200;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            GodVillageSavedData data = GodVillageSavedData.get(level);
            long gameTime = level.getGameTime();

            for (GodVillageSavedData.GodVillageRecord village : data.getAllVillages()) {
                // Verificar si hay jugadores cerca del poblado y si pasaron al menos 3 días desde la última raid
                if (!village.isUnderRaid() && (gameTime - village.lastRaidTick() > 72000L)) {
                    if (level.hasNearbyAlivePlayer(village.altarPos().getX(), village.altarPos().getY(), village.altarPos().getZ(), 80.0D)) {
                        // 15% de probabilidad de detonar la invasión en esta ventana
                        if (level.random.nextFloat() < 0.15F) {
                            startVillageRaid(server, level, data, village);
                        }
                    }
                }
            }
        }
    }

    private static void startVillageRaid(
            MinecraftServer server,
            ServerLevel level,
            GodVillageSavedData data,
            GodVillageSavedData.GodVillageRecord village
    ) {
        SinGod defenderGod = village.patronGod();
        SinGod attackerGod = pickRivalGod(defenderGod, level.random);

        // 1. Notificación en el Chat Global del Servidor
        Component raidAnnouncement = Component.literal("⚔ ¡GUERRA SANTA! ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("Acólitos de ").withStyle(ChatFormatting.GRAY))
                .append(attackerGod.getDisplayName().copy().withStyle(attackerGod.getColor(), ChatFormatting.BOLD))
                .append(Component.literal(" marchan para asediar el Templo de ").withStyle(ChatFormatting.GRAY))
                .append(defenderGod.getDisplayName().copy().withStyle(defenderGod.getColor(), ChatFormatting.BOLD))
                .append(Component.literal(String.format(" en [%d, %d]!", village.altarPos().getX(), village.altarPos().getZ()))
                        .withStyle(ChatFormatting.YELLOW));

        server.getPlayerList().broadcastSystemMessage(raidAnnouncement, false);

        // 2. Spawneo de Acólitos Invasores
        spawnInvasionForces(level, village.altarPos(), attackerGod, 8);

        // 3. Tirada del 5% de Probabilidad: ¡Manifestación del Avatar!
        boolean manifestsAvatar = level.random.nextFloat() < 0.05F;
        if (manifestsAvatar) {
            triggerAvatarManifestation(server, level, village.altarPos(), defenderGod, attackerGod);
        }

        // Marcar estado de asedio
        data.updateVillage(new GodVillageSavedData.GodVillageRecord(
                village.id(),
                village.altarPos(),
                village.patronGod(),
                village.radius(),
                level.getGameTime(),
                true,
                village.isDesolated(),
                village.desolatedSinceTick()
        ));
    }

    /**
     * Reajuste de Población: Si la aldea quedó desolada (0 aldeanos) pero el Altar sigue en pie,
     * tras 48,000 ticks (2 días MC), el Dios envía una Caravana de Peregrinos para reanimar el templo.
     */
    public static void checkRepopulation(MinecraftServer server, ServerLevel level, GodVillageSavedData data, GodVillageSavedData.GodVillageRecord village) {
        if (!village.isDesolated()) return;

        long elapsedTicks = level.getGameTime() - village.desolatedSinceTick();
        if (elapsedTicks >= 48000L) { // 2 días de Minecraft
            // Spawneo de Caravana de Peregrinos
            spawnPilgrimCaravan(level, village.altarPos(), village.patronGod(), 3);

            // Reavivar aldea en la persistencia
            data.updateVillage(new GodVillageSavedData.GodVillageRecord(
                    village.id(),
                    village.altarPos(),
                    village.patronGod(),
                    village.radius(),
                    village.lastRaidTick(),
                    false,
                    false, // Ya no está desolada
                    0L
            ));

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
            net.minecraft.world.entity.npc.villager.Villager villager = net.minecraft.world.entity.EntityType.VILLAGER.create(level, EntitySpawnReason.EVENT);
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

    /**
     * Cruzadas contra Aldeas Normales Vanilla (Facción Mundana):
     * Un dios envía acólitos para someter a los Gólems de Hierro y forzar la conversión.
     */
    public static void startNeutralVillageCrusade(MinecraftServer server, ServerLevel level, BlockPos villageBellPos, SinGod attackingGod) {
        Component announcement = Component.literal("⚔ ¡CRUZADA DE CONVERSIÓN! ")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                .append(Component.literal("Una horda de ").withStyle(ChatFormatting.GRAY))
                .append(attackingGod.getDisplayName().copy().withStyle(attackingGod.getColor(), ChatFormatting.BOLD))
                .append(Component.literal(String.format(" asalta un poblado inocente en [%d, %d]!", villageBellPos.getX(), villageBellPos.getZ()))
                        .withStyle(ChatFormatting.YELLOW));

        server.getPlayerList().broadcastSystemMessage(announcement, false);
        spawnInvasionForces(level, villageBellPos, attackingGod, 6);
    }

    // Registro de cazas de ira pospuestas por prioridad de asedio a aldeas
    private static final Map<SinGod, UUID> POSTPONED_WRATH_HUNTS = new EnumMap<>(SinGod.class);

    private static void triggerAvatarManifestation(
            MinecraftServer server,
            ServerLevel level,
            BlockPos altarPos,
            SinGod defenderGod,
            SinGod attackerGod
    ) {
        // 50% Defensor, 50% Atacante
        SinGod appearingGod = level.random.nextBoolean() ? defenderGod : attackerGod;

        // --- REGLA DE AVATAR ÚNICO Y PRIORIDAD DE ASEDIO SOBRE CAZA ---
        GodAvatarEntity existingAvatar = DivineInterventionScheduler.getActiveAvatar(server, appearingGod);

        if (existingAvatar != null) {
            // El Avatar ya existe en el servidor. Si está en una Caza de Ira, la aldea es PRIORITARIA:
            if (existingAvatar.isWrathHunter() && existingAvatar.getTargetPlayerUuid() != null) {
                UUID huntedPlayerUuid = existingAvatar.getTargetPlayerUuid();
                POSTPONED_WRATH_HUNTS.put(appearingGod, huntedPlayerUuid);

                ServerPlayer huntedPlayer = server.getPlayerList().getPlayer(huntedPlayerUuid);
                if (huntedPlayer != null) {
                    huntedPlayer.sendSystemMessage(
                            appearingGod.getFormattedPrefix().append(
                                    Component.literal("«¡Mortal insolente! Tu juicio queda aplazado: mi templo sagrado clama por mi presencia divina. ¡Considera este día un préstamo de vida!»")
                                            .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC)
                            )
                    );
                }

                // Efectos de salida en la ubicación anterior y teletransporte al altar preservando vida
                ServerLevel oldLevel = (ServerLevel) existingAvatar.level();
                oldLevel.sendParticles(ParticleTypes.PORTAL, existingAvatar.getX(), existingAvatar.getY() + 1.0D, existingAvatar.getZ(), 50, 0.5D, 0.8D, 0.5D, 0.1D);
                float currentHealth = existingAvatar.getHealth();

                if (oldLevel == level) {
                    existingAvatar.snapTo(altarPos.getX() + 0.5D, altarPos.getY() + 1.0D, altarPos.getZ() + 0.5D, 0.0F, 0.0F);
                    existingAvatar.getNavigation().stop();
                    existingAvatar.initializeAvatar(level, appearingGod, null, false);
                    existingAvatar.setHealth(currentHealth);
                } else {
                    existingAvatar.discard();
                    GodAvatarEntity transferred = ModEntities.GOD_AVATAR.get().create(level, EntitySpawnReason.EVENT);
                    if (transferred != null) {
                        transferred.snapTo(altarPos.getX() + 0.5D, altarPos.getY() + 1.0D, altarPos.getZ() + 0.5D, 0.0F, 0.0F);
                        transferred.initializeAvatar(level, appearingGod, null, false);
                        transferred.setHealth(currentHealth);
                        level.addFreshEntity(transferred);
                    }
                }
            } else {
                // Si es un Avatar Campeón o ya está en la aldea, se reubica al frente de batalla
                existingAvatar.snapTo(altarPos.getX() + 0.5D, altarPos.getY() + 1.0D, altarPos.getZ() + 0.5D, 0.0F, 0.0F);
            }
        } else {
            // No existe avatar activo: spawnear uno nuevo
            GodAvatarEntity avatar = ModEntities.GOD_AVATAR.get().create(level, EntitySpawnReason.EVENT);
            if (avatar != null) {
                avatar.snapTo(altarPos.getX() + 0.5D, altarPos.getY() + 1.0D, altarPos.getZ() + 0.5D, 0.0F, 0.0F);
                avatar.initializeAvatar(level, appearingGod, null, false);
                level.addFreshEntity(avatar);
            }
        }

        // Anuncio trascendental en el chat global
        Component avatarAnnouncement = Component.literal("⚡ ¡MANIFESTACIÓN DIVINA! ")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal("El clamor de la sangre ha desgarrado el velo: ").withStyle(ChatFormatting.WHITE))
                .append(appearingGod.getDisplayName().copy().withStyle(appearingGod.getColor(), ChatFormatting.BOLD))
                .append(Component.literal(" ha descendido para juzgar la batalla!").withStyle(ChatFormatting.WHITE));

        server.getPlayerList().broadcastSystemMessage(avatarAnnouncement, false);
    }

    /**
     * Al concluir el asedio, si el Avatar sobrevivió y tenía una caza pospuesta, la retoma:
     */
    public static void onVillageRaidEnded(MinecraftServer server, ServerLevel level, SinGod god) {
        UUID postponedPlayerUuid = POSTPONED_WRATH_HUNTS.remove(god);
        if (postponedPlayerUuid == null) return;

        ServerPlayer target = server.getPlayerList().getPlayer(postponedPlayerUuid);
        GodAvatarEntity avatar = DivineInterventionScheduler.getActiveAvatar(server, god);

        if (target != null && target.isAlive() && avatar != null && avatar.isAlive()) {
            target.sendSystemMessage(
                    god.getFormattedPrefix().append(
                            Component.literal("«Mi templo sagrado está a salvo. Ahora... volvamos a nuestra deuda pendiente, mortal.»")
                                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                    )
            );
            DivineInterventionScheduler.triggerWrathAvatarHunt(target, god);
        }
    }

    private static void spawnInvasionForces(ServerLevel level, BlockPos center, SinGod attackerGod, int count) {
        for (int i = 0; i < count; i++) {
            GodAcolyteEntity acolyte = ModEntities.GOD_ACOLYTE.get().create(level, EntitySpawnReason.EVENT);
            if (acolyte != null) {
                // Spawnea a 32 bloques del altar
                double angle = (2 * Math.PI / count) * i;
                int x = (int) (center.getX() + 32 * Math.cos(angle));
                int z = (int) (center.getZ() + 32 * Math.sin(angle));
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

                acolyte.snapTo(x + 0.5D, y, z + 0.5D, 0.0F, 0.0F);
                acolyte.configureAcolyte(level, attackerGod, null, false);
                level.addFreshEntity(acolyte);
            }
        }
    }

    private static SinGod pickRivalGod(SinGod defender, RandomSource random) {
        List<SinGod> rivals = java.util.Arrays.stream(SinGod.values())
                .filter(g -> g != defender)
                .toList();
        return rivals.get(random.nextInt(rivals.size()));
    }
}
```

---

## 6. Asedios del Jugador, Detección de Traición y Población Única

### 6.1 El Cuerno de Asedio Divino (`DivineWarHornItem`)
Permite al jugador liderar asaltos contra aldeas consagradas en nombre de su dios primario (`favor >= +40`):

```java
package com.volcov.sevendeadlygods.item;

import com.volcov.sevendeadlygods.entity.GodAcolyteEntity;
import com.volcov.sevendeadlygods.favor.PlayerSinData;
import com.volcov.sevendeadlygods.favor.SinFavorManager;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.registry.ModEntities;
import com.volcov.sevendeadlygods.village.GodVillageSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class DivineWarHornItem extends Item {
    public DivineWarHornItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        GodVillageSavedData villageData = GodVillageSavedData.get(serverLevel);
        Optional<GodVillageSavedData.GodVillageRecord> villageOpt = villageData.getVillageAt(player.blockPosition());

        if (villageOpt.isEmpty()) {
            player.sendSystemMessage(Component.literal("Debes encontrarte en los dominios de una Aldea Consagrada para sonar el cuerno de asedio.")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        GodVillageSavedData.GodVillageRecord village = villageOpt.get();
        PlayerSinData sinData = SinFavorManager.get(serverPlayer);
        
        // Obtener el dios primario del jugador (el de mayor favor, mínimo +40)
        SinGod playerGod = sinData.getPrimaryPatronGod().orElse(null);
        if (playerGod == null || sinData.getFavor(playerGod) < 40) {
            player.sendSystemMessage(Component.literal("Ningún dios te reconoce como su adalid (se requiere Favor >= +40 para liderar un asedio sagrado).")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        if (playerGod == village.patronGod()) {
            player.sendSystemMessage(Component.literal("¡No puedes asediar un templo consagrado a tu propio Dios patrón!")
                    .withStyle(ChatFormatting.GOLD));
            return InteractionResult.FAIL;
        }

        // Tocar sonido solemne de cuerno
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 2.5F, 0.85F);
        player.getCooldowns().addCooldown(stack, 20 * 60); // 1 minuto de cooldown

        // Notificar en el chat del servidor
        serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("⚔ ¡ASEDIO JURADO! ")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                        .append(player.getName().copy().withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" lidera a los siervos de ").withStyle(ChatFormatting.GRAY))
                        .append(playerGod.getDisplayName().copy().withStyle(playerGod.getColor(), ChatFormatting.BOLD))
                        .append(Component.literal(" contra el templo de ").withStyle(ChatFormatting.GRAY))
                        .append(village.patronGod().getDisplayName().copy().withStyle(village.patronGod().getColor(), ChatFormatting.BOLD))
                        .append(Component.literal("!").withStyle(ChatFormatting.YELLOW)),
                false
        );

        // Invocar escuadrón de asalto leal al jugador
        spawnPlayerAssaultSquad(serverLevel, player.blockPosition(), playerGod, serverPlayer);
        return InteractionResult.SUCCESS;
    }

    private void spawnPlayerAssaultSquad(ServerLevel level, BlockPos origin, SinGod god, ServerPlayer leader) {
        for (int i = 0; i < 6; i++) {
            GodAcolyteEntity acolyte = ModEntities.GOD_ACOLYTE.get().create(level, EntitySpawnReason.EVENT);
            if (acolyte != null) {
                acolyte.snapTo(origin.getX() + (i * 2) - 5, origin.getY() + 1.0D, origin.getZ() + (i * 2) - 5, 0, 0);
                acolyte.configureAcolyte(level, god, leader.getUUID(), true);
                level.addFreshEntity(acolyte);
            }
        }
    }
}
```

---

### 6.2 Detección de Alta Traición en Aldeas
Si un jugador con `favor >= 0` ataca a los habitantes de una aldea consagrada, el servidor detecta el acto de traición:

```java
@SubscribeEvent
public static void onLivingHurt(LivingDamageEvent.Pre event) {
    if (event.getSource().getEntity() instanceof ServerPlayer player) {
        if (event.getEntity() instanceof GodAcolyteEntity acolyte && acolyte.level() instanceof ServerLevel level) {
            SinGod god = acolyte.getGod();
            int currentFavor = SinFavorManager.getFavor(player, god);

            if (currentFavor >= 0) {
                // ¡ALTA TRAICIÓN DETECTADA!
                // Drenaje masivo de -60 de favor de un solo golpe
                SinFavorManager.addEffort(player, god, -60.0F, "Alta Traición: Has atacado a tus propios hermanos de fe");

                player.sendSystemMessage(
                        god.getFormattedPrefix().append(
                                Component.literal("«¡Traidor miserable! Con sangre pagas mis favores. ¡Que el cielo descargue su ira sobre ti!»")
                                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                        )
                );
                level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.7F);
            }
        }
    }
}
```

---

### 6.3 Configuración de Poblaciones Únicas (Holgazanes y Aldeas en Ruinas)
1. **Belphegor (Pereza - Holgazanes / Nitwits)**:
   * Al generarse los aldeanos en una aldea consagrada a Belphegor, un `EntityJoinLevelEvent` o procesador de estructura reemplaza su profesión por `VillagerProfession.NITWIT` (el aldeano de túnica verde que no trabaja y duerme hasta tarde).
2. **Leviatán (Envidia - Aldea Arrasada)**:
   * En `VillageStructureInjector`, la piscina de plantillas de Leviatán inyecta piezas de casas destruidas (`village/ruined_house_1`, `ruined_house_2`) y un Altar Mayor oculto en una cripta de piedra con prismarina.

---

## 7. Análisis de Conflictos Potenciales y Soluciones Arquitectónicas

Para garantizar máxima estabilidad, compatibilidad con otros mods y coherencia con el motor de **Minecraft 26.3 / NeoForge 26.3**, se han analizado y resuelto los siguientes 6 conflictos críticos:

```
+---------------------------------------------------------------------------------------------------------------+
|                            MATRIZ DE RESOLUCIÓN DE CONFLICTOS ARQUITECTÓNICOS                                 |
+------------------------------------+------------------------------------+-------------------------------------+
| Escenario de Conflicto Potencial   | Causa Raíz / Riesgo Técnico        | Solución de Ingeniería Adoptada     |
+------------------------------------+------------------------------------+-------------------------------------+
| 1. Descarga de Chunks durante      | Los jugadores se marchan mientras  | Máquina de estados en SavedData:    |
|    un Asedio (Chunk Unloading)     | una raid ocurre; entidades se      | La raid entra en PAUSED_RAID si no  |
|                                    | congelan o desincronizan.          | hay jugadores en 80m. Se reanuda    |
|                                    |                                    | limpia al volver. Cancelación tras  |
|                                    |                                    | 5 días MC sin jugadores.            |
+------------------------------------+------------------------------------+-------------------------------------+
| 2. Conflicto con la IA Vanilla     | Mojang usa Brain<Villager> en 1.14+| Desacoplamiento total: Los aldeanos|
|    de Aldeanos (Brain vs Goals)    | Inyectar MeleeAttackGoal en        | conservan su Brain civil puro.      |
|                                    | Villager corrompe sus tareas.      | Todo el combate y patrulla recae en |
|                                    |                                    | GodAcolyteEntity (PathfinderMob).   |
+------------------------------------+------------------------------------+-------------------------------------+
| 3. Incompatibilidad con Mods de    | Mods como CTOV o Towns & Towers    | Auto-registro de Altar Único: El BE |
|    Aldeas (CTOV, Lithostitched)    | alteran o duplican pools Jigsaw;   | verifica StructureStart. Si ya hay  |
|                                    | riesgo de generar 20 templos.      | un altar en 128m, los demás no se   |
|                                    |                                    | registran como corazón de aldea.    |
+------------------------------------+------------------------------------+-------------------------------------+
| 4. Conflicto de Invocación de      | Un jugador realiza un ritual para  | Validación con Scheduler: El ritual |
|    Avatar durante un Asedio        | invocar al Avatar mientras este ya | detecta al Avatar activo en aldea,  |
|    (Regla de Avatar Único)         | pelea en un asedio territorial.    | no duplica cuerpo físico y otorga   |
|                                    |                                    | Bendición Divina V alternativa.     |
+------------------------------------+------------------------------------+-------------------------------------+
| 5. Desolación y POIs de Aldea      | Si mueren todos los aldeanos, el   | El Altar Mayor se registra como un  |
|    (PoiManager de Minecraft)       | PoiManager puede disolver el pueblo| POI sagrado permanente en la zona,  |
|                                    | impidiendo su repoblación.         | anclando la aldea para los nuevos   |
|                                    |                                    | peregrinos.                         |
+------------------------------------+------------------------------------+-------------------------------------+
| 6. Fluctuación de Favor durante    | El jugador realiza acciones que    | Validación reactiva en MerchantMenu:|
|    la Sesión de Comercio           | bajan el favor a negativo mientras | Si el favor cae a < 0 con la GUI    |
|                                    | tiene el menú de trade abierto.    | abierta, se cierra inmediatamente   |
|                                    |                                    | y el acólito cancela el trato.      |
+------------------------------------+------------------------------------+-------------------------------------+
```

### 7.1 Detalle Técnico: Manejo de Pausa de Asedios por Descarga de Chunks
```java
public static void tickActiveRaids(ServerLevel level, GodVillageSavedData data) {
    long currentTime = level.getGameTime();

    for (GodVillageSavedData.GodVillageRecord village : data.getAllVillages()) {
        if (!village.isUnderRaid()) continue;

        // Comprobar si el chunk del Altar Mayor está cargado
        boolean isChunkLoaded = level.isLoaded(village.altarPos());
        boolean hasPlayersNear = level.hasNearbyAlivePlayer(
                village.altarPos().getX(), village.altarPos().getY(), village.altarPos().getZ(), 80.0D
        );

        if (!isChunkLoaded || !hasPlayersNear) {
            // Si la aldea pasa más de 5 días de Minecraft descargada durante una raid, cancelarla limpiamente
            if (currentTime - village.lastRaidTick() > 120000L) {
                data.updateVillage(new GodVillageSavedData.GodVillageRecord(
                        village.id(), village.altarPos(), village.patronGod(), village.radius(),
                        currentTime, false, village.isDesolated(), village.desolatedSinceTick()
                ));
            }
            continue; // Pausar ticks de combate mientras esté descargada
        }

        // Si está cargada y con jugadores, proceder con el combate normal
    }
}
```

---

## 8. Checklist de Implementación Paso a Paso

1. [ ] **Creación del Template NBT**:
   * Diseñar en un mundo de desarrollo una pequeña estructura de Templo/Santuario (`god_shrine.nbt`) que contenga el bloque `sevendeadlygods:greed_altar` (o un bloque genérico que el procesador sustituya por el dios correspondiente).
   * Colocarlo en `src/main/resources/data/sevendeadlygods/structures/village/god_shrine.nbt`.
2. [ ] **Inyector de Jigsaw**:
   * Implementar `VillageStructureInjector` registrado en `ServerAboutToStartEvent` para agregar el santuario a las piscinas de casas de los biomas de aldeas.
3. [ ] **SavedData de Aldeas**:
   * Añadir `GodVillageSavedData.java` al paquete `village` para registrar automáticamente los templos y coordinar la presencia territorial.
4. [ ] **Actualización de `GodAcolyteEntity`**:
   * Integrar la meta de ataque para `favor <= -40` y el handler de `mobInteract` con rechazo en `< 0` y `openTradingScreen` en `>= 0`.
5. [ ] **Event Manager de Incursiones (`GodVillageRaidManager`)**:
   * Suscribir al tick de servidor, evaluar proximidad de jugadores, disparar alertas con `broadcastSystemMessage`, spawnear atacantes e invocar al Avatar con el **5% de probabilidad**, respetando la regla de Avatar Único y posposición de cazas.
6. [ ] **Ítem de Asedio y Detección de Traición**:
   * Registrar `DivineWarHornItem` y suscribir el evento de ataque para penalizar la alta traición con `-60 de favor`.
7. [ ] **Reajuste de Población (Peregrinos)**:
   * Implementar `checkRepopulation` para reanimar aldeas desoladas tras 48,000 ticks si el altar mayor sigue intacto.

