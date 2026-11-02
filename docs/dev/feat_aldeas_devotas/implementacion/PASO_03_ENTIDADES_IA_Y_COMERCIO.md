# Paso 3: Entidades Guardianas, IA y Comercio Divino

Este paso implementa la IA de los **Acólitos Centinelas (`GodAcolyteEntity`)** para gobernar las reglas de interacción basadas en el **Favor** del jugador con el Dios de la aldea:
* `Favor <= -40`: Hostilidad total (ataque inmediato a muerte).
* `-39 <= Favor < 0`: Tensión y desconfianza (rechazo de comercio con sonido *Villager No*).
* `Favor >= 0`: Devoción y apertura del catálogo de **Comercio Divino** (*Divine Trading*).

---

## 🎯 Objetivos del Paso
1. Añadir el `TargetGoal` en `GodAcolyteEntity` para cazar automáticamente a jugadores con `favor <= -40`.
2. Sobrescribir `mobInteract` para gestionar el rechazo verbal en favor negativo y la apertura de comercio en favor positivo.
3. Crear `DivineTradingRegistry` para construir las listas de `MerchantOffers` específicas para cada uno de los 7 Dioses del Pecado.

---

## 📁 Archivos Involucrados

| Tipo | Ruta del Archivo | Descripción |
| :--- | :--- | :--- |
| **Entidad** | `src/main/java/com/volcov/sevendeadlygods/entity/GodAcolyteEntity.java` | Modificación de goals de combate e interacción. |
| **Comercio** | `src/main/java/com/volcov/sevendeadlygods/village/DivineTradingRegistry.java` | Generador de intercambios comerciales divinos. |

---

## 💻 Implementación Técnica

### 3.1 Actualización de Metas e Interacción en `GodAcolyteEntity.java`
En `registerGoals()`:

```java
// Objetivo de ataque para jugadores con favor <= -40 (Herejes territoriales)
this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
        this,
        Player.class,
        10,
        true,
        false,
        (living, serverLevel) -> {
            if (living instanceof ServerPlayer player) {
                int favor = SinFavorManager.getFavor(player, this.getGod());
                return favor <= -40; // Hostilidad total
            }
            return false;
        }
));
```

En `mobInteract(Player player, InteractionHand hand)`:

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

    // 1. Hereje hostil (<= -40) -> Ataca
    if (favor <= -40) {
        this.setTarget(serverPlayer);
        return InteractionResult.CONSUME;
    }

    // 2. Sospechoso / Pagano (-39 a -1) -> Rechazo
    if (favor < 0) {
        this.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
        serverPlayer.sendSystemMessage(
                god.getFormattedPrefix().append(
                        Component.literal("«No tenemos tratos con impíos. Arrepiéntete y purifica tu alma ante el altar antes de pedir nuestra bendición.»")
                                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC)
                )
        );
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    // 3. Devoto / Fiel (>= 0) -> Abre Comercio Divino
    DivineTradingRegistry.openTrading(this, serverPlayer);
    return InteractionResult.sidedSuccess(this.level().isClientSide());
}
```

### 3.2 Registro de Comercio Divino (`DivineTradingRegistry.java`)
```java
package com.volcov.sevendeadlygods.village;

import com.volcov.sevendeadlygods.entity.GodAcolyteEntity;
import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public final class DivineTradingRegistry {
    private DivineTradingRegistry() {}

    public static void openTrading(GodAcolyteEntity acolyte, ServerPlayer player) {
        MerchantOffers offers = new MerchantOffers();
        SinGod god = acolyte.getGod();

        switch (god) {
            case GREED -> {
                offers.add(new MerchantOffer(new ItemCost(Items.GOLD_INGOT, 8), new ItemStack(Items.EMERALD, 5), 16, 5, 0.05F));
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 32), new ItemStack(Items.DIAMOND, 4), 8, 10, 0.05F));
            }
            case WRATH -> {
                offers.add(new MerchantOffer(new ItemCost(Items.SKELETON_SKULL, 1), new ItemStack(Items.IRON_SWORD, 1), 12, 5, 0.05F));
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 15), new ItemStack(Items.POTION, 1), 6, 8, 0.05F));
            }
            case GLUTTONY -> {
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 4), new ItemStack(Items.GOLDEN_CARROT, 12), 16, 4, 0.05F));
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 8), new ItemStack(Items.CAKE, 1), 8, 8, 0.05F));
            }
            case PRIDE -> {
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 10), new ItemStack(Items.QUARTZ_BLOCK, 16), 16, 6, 0.05F));
                offers.add(new MerchantOffer(new ItemCost(Items.DIAMOND, 3), new ItemStack(Items.TOTEM_OF_UNDYING, 1), 4, 12, 0.05F));
            }
            case ENVY -> {
                offers.add(new MerchantOffer(new ItemCost(Items.ROTTEN_FLESH, 8), new ItemStack(Items.EMERALD, 2), 16, 4, 0.05F));
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 12), new ItemStack(Items.ENDER_PEARL, 4), 8, 8, 0.05F));
            }
            case SLOTH -> {
                offers.add(new MerchantOffer(new ItemCost(Items.WHITE_WOOL, 5), new ItemStack(Items.EMERALD, 2), 16, 3, 0.05F));
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 6), new ItemStack(Items.RED_BED, 1), 8, 5, 0.05F));
            }
            case LUST -> {
                offers.add(new MerchantOffer(new ItemCost(Items.POPPY, 4), new ItemStack(Items.EMERALD, 2), 16, 3, 0.05F));
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 10), new ItemStack(Items.EXPERIENCE_BOTTLE, 8), 12, 6, 0.05F));
            }
        }

        acolyte.openTradingScreen(player, god.getDisplayName().copy().append(" - Mercader del Templo"), 1);
    }
}
```

---

## ✅ Criterios de Aceptación y Validación
1. [ ] Con favor `-50`: acercarse a un acólito y verificar que desenfunda su espada y ataca inmediatamente.
2. [ ] Con favor `-10`: hacer clic derecho en un acólito; verificar que emite el sonido de *Villager No* y envía el mensaje de rechazo al chat.
3. [ ] Con favor `+20`: hacer clic derecho; verificar que se abre la ventana nativa de comercio con los trades exclusivos del dios.
