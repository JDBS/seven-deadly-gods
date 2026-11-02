# Paso 4: Gestor de Asaltos y Guerras Santas (`GodVillageRaidManager`)

Este paso implementa el motor de incursiones territoriales autónomas entre cultos rivales, coordinando la detección de proximidad de jugadores, cooldowns orgánicos de aldea, avisos formateados en el chat global y el despliegue de tropas invasoras.

---

## 🎯 Objetivos del Paso
1. Crear el bucle de evaluación periódica en `ServerTickEvent.Post` (cada 1 minuto / 1,200 ticks).
2. Asegurar que las invasiones solo se activen si hay un jugador a menos de 80 bloques y la aldea tiene más de 3 días de Minecraft de paz.
3. Emitir el anuncio dramático de guerra en el chat con los colores oficiales de ambos dioses.
4. Generar el escuadrón invasor de acólitos hostiles alrededor del perímetro del templo.

---

## 📁 Archivos Involucrados

| Tipo | Ruta del Archivo | Descripción |
| :--- | :--- | :--- |
| **Código Java** | `src/main/java/com/volcov/sevendeadlygods/village/GodVillageRaidManager.java` | Gestor global de ticks y asedios. |
| **Suscripción** | `src/main/java/com/volcov/sevendeadlygods/SevenDeadlyGodsMod.java` | Registro en el bus de eventos de NeoForge. |

---

## 💻 Implementación Técnica

### 4.1 Gestor de Asedios (`GodVillageRaidManager.java`)
```java
package com.volcov.sevendeadlygods.village;

import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.entity.GodAcolyteEntity;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

@EventBusSubscriber(modid = SevenDeadlyGodsMod.MOD_ID)
public class GodVillageRaidManager {
    private static final int CHECK_INTERVAL_TICKS = 1200; // 1 minuto
    private static final long RAID_COOLDOWN_TICKS = 72000L; // 3 días de Minecraft

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
                if (village.state() == GodVillageState.CONSAGRADA && (gameTime - village.lastRaidTick() > RAID_COOLDOWN_TICKS)) {
                    // Verificar proximidad de jugadores vivos
                    if (level.hasNearbyAlivePlayer(village.altarPos().getX(), village.altarPos().getY(), village.altarPos().getZ(), 80.0D)) {
                        // 15% de probabilidad por intervalo
                        if (level.random.nextFloat() < 0.15F) {
                            startAutonomousRaid(server, level, data, village);
                        }
                    }
                }
            }
        }
    }

    private static void startAutonomousRaid(
            MinecraftServer server,
            ServerLevel level,
            GodVillageSavedData data,
            GodVillageSavedData.GodVillageRecord village
    ) {
        SinGod defender = village.patronGod();
        SinGod attacker = pickRivalGod(defender, level.random);

        // 1. Notificación en el Chat Global
        Component msg = Component.literal("⚔ ¡GUERRA SANTA! ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("Acólitos de ").withStyle(ChatFormatting.GRAY))
                .append(attacker.getDisplayName().copy().withStyle(attacker.getColor(), ChatFormatting.BOLD))
                .append(Component.literal(" marchan para saquear el Templo de ").withStyle(ChatFormatting.GRAY))
                .append(defender.getDisplayName().copy().withStyle(defender.getColor(), ChatFormatting.BOLD))
                .append(Component.literal(String.format(" en [%d, %d]!", village.altarPos().getX(), village.altarPos().getZ()))
                        .withStyle(ChatFormatting.YELLOW));

        server.getPlayerList().broadcastSystemMessage(msg, false);

        // 2. Spawneo de Escuadrón Invasor (8 acólitos)
        spawnInvasionForces(level, village.altarPos(), attacker, 8);

        // 3. Actualizar estado en persistencia
        data.updateVillage(new GodVillageSavedData.GodVillageRecord(
                village.id(), village.altarPos(), village.patronGod(), village.radius(),
                level.getGameTime(), GodVillageState.EN_ASEDIO, level.getGameTime()
        ));
    }

    private static void spawnInvasionForces(ServerLevel level, BlockPos center, SinGod attackerGod, int count) {
        for (int i = 0; i < count; i++) {
            GodAcolyteEntity acolyte = ModEntities.GOD_ACOLYTE.get().create(level, EntitySpawnReason.EVENT);
            if (acolyte != null) {
                double angle = (2 * Math.PI / count) * i;
                int x = (int) (center.getX() + 32 * Math.cos(angle));
                int z = (int) (center.getZ() + 32 * Math.sin(angle));
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

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

## ✅ Criterios de Aceptación y Validación
1. [ ] Permanecer en una aldea consagrada y forzar el tiempo de juego (`/time add 75000`); verificar que la raid puede activarse.
2. [ ] Comprobar que todos los jugadores conectados reciben el mensaje formateado en el chat.
3. [ ] Confirmar que los 8 acólitos invasores spawnean a 32 bloques y atacan a los defensores de la aldea.
