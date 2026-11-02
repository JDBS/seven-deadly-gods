# Paso 6: Asedios Liderados por el Jugador y Detección de Alta Traición

Este paso permite al jugador tomar la iniciativa militar mediante el **Cuerno de Asedio Divino (`DivineWarHornItem`)** para invadir aldeas enemigas en nombre de su Dios Primario (`favor >= +40`), e implementa las severas consecuencias por **Alta Traición** si ataca a sus propios hermanos de fe (`-60 de favor`).

---

## 🎯 Objetivos del Paso
1. Registrar el ítem `DivineWarHornItem` con sonido ceremonial de asedio y cooldown de 1 minuto.
2. Identificar al Dios Patrón Primario del jugador y generar un escuadrón de 6 acólitos invasores leales que obedezcan su liderazgo.
3. Detectar si el jugador agrede a habitantes de una aldea donde tenía `favor >= 0`, aplicando la penalización instantánea de `-60 de favor` y la respuesta de repudio cósmico.

---

## 📁 Archivos Involucrados

| Tipo | Ruta del Archivo | Descripción |
| :--- | :--- | :--- |
| **Ítem** | `src/main/java/com/volcov/sevendeadlygods/item/DivineWarHornItem.java` | Lógica del cuerno ritual de asedio. |
| **Registro** | `src/main/java/com/volcov/sevendeadlygods/registry/ModItems.java` | Registro del nuevo ítem en NeoForge. |
| **Eventos** | `src/main/java/com/volcov/sevendeadlygods/favor/PlayerSinBehaviorEvents.java` | Detección de daño a aliados y penalización de traición. |

---

## 💻 Implementación Técnica

### 6.1 El Cuerno de Asedio Divino (`DivineWarHornItem.java`)
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
        
        // Determinar el dios con mayor favor positivo (mínimo +40)
        SinGod playerGod = null;
        int highestFavor = 39;
        for (SinGod god : SinGod.values()) {
            int fav = sinData.getFavor(god);
            if (fav > highestFavor) {
                highestFavor = fav;
                playerGod = god;
            }
        }

        if (playerGod == null) {
            player.sendSystemMessage(Component.literal("Ningún dios te reconoce como su adalid (se requiere Favor >= +40 para liderar un asedio sagrado).")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        if (playerGod == village.patronGod()) {
            player.sendSystemMessage(Component.literal("¡No puedes asediar un templo consagrado a tu propio Dios patrón!")
                    .withStyle(ChatFormatting.GOLD));
            return InteractionResult.FAIL;
        }

        serverLevel.playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 2.5F, 0.85F);
        player.getCooldowns().addCooldown(stack, 20 * 60);

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

### 6.2 Detección de Alta Traición en `PlayerSinBehaviorEvents.java`
```java
@SubscribeEvent
public static void onLivingHurt(LivingDamageEvent.Pre event) {
    if (event.getSource().getEntity() instanceof ServerPlayer player) {
        if (event.getEntity() instanceof GodAcolyteEntity acolyte && acolyte.level() instanceof ServerLevel level) {
            SinGod god = acolyte.getGod();
            int currentFavor = SinFavorManager.getFavor(player, god);

            if (currentFavor >= 0) {
                // ¡ALTA TRAICIÓN! Pérdida masiva de -60 de favor de un solo golpe
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

## ✅ Criterios de Aceptación y Validación
1. [ ] Usar el cuerno teniendo `favor < 40`; verificar que se niega la invocación.
2. [ ] Subir favor a `+50` con Mammon y usar el cuerno frente a una aldea de Khorvath; verificar que spawnean 6 acólitos de Mammon leales.
3. [ ] Teniendo `favor = +30` con Mammon, atacar a un acólito centinela de Mammon; comprobar que el favor cae a `-30` y el dios envía el mensaje de repudio.
