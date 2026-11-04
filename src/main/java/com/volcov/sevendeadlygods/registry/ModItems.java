package com.volcov.sevendeadlygods.registry;

import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.entity.DivineEquipmentHelper;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.item.DivineWarHornItem;
import com.volcov.sevendeadlygods.item.GodAcolyteSpawnEggItem;
import com.volcov.sevendeadlygods.lore.SinLoreLibrary;
import com.volcov.sevendeadlygods.village.DivineTradingRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(SevenDeadlyGodsMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SevenDeadlyGodsMod.MOD_ID);

    public static final DeferredItem<BlockItem> GREED_ALTAR_ITEM = ITEMS.registerSimpleBlockItem("greed_altar", ModBlocks.GREED_ALTAR);
    public static final DeferredItem<BlockItem> WRATH_ALTAR_ITEM = ITEMS.registerSimpleBlockItem("wrath_altar", ModBlocks.WRATH_ALTAR);
    public static final DeferredItem<BlockItem> GLUTTONY_ALTAR_ITEM = ITEMS.registerSimpleBlockItem("gluttony_altar", ModBlocks.GLUTTONY_ALTAR);
    public static final DeferredItem<BlockItem> PRIDE_ALTAR_ITEM = ITEMS.registerSimpleBlockItem("pride_altar", ModBlocks.PRIDE_ALTAR);
    public static final DeferredItem<BlockItem> ENVY_ALTAR_ITEM = ITEMS.registerSimpleBlockItem("envy_altar", ModBlocks.ENVY_ALTAR);
    public static final DeferredItem<BlockItem> SLOTH_ALTAR_ITEM = ITEMS.registerSimpleBlockItem("sloth_altar", ModBlocks.SLOTH_ALTAR);
    public static final DeferredItem<BlockItem> LUST_ALTAR_ITEM = ITEMS.registerSimpleBlockItem("lust_altar", ModBlocks.LUST_ALTAR);

    public static final DeferredItem<Item> DIVINE_WAR_HORN = ITEMS.registerItem(
            "divine_war_horn",
            props -> new DivineWarHornItem(props.stacksTo(1).rarity(Rarity.RARE))
    );

    // Huevos Generadores (Spawn Eggs) para los Acólitos de cada Dios del Pecado
    public static final DeferredItem<GodAcolyteSpawnEggItem> GREED_ACOLYTE_SPAWN_EGG = ITEMS.registerItem(
            "greed_acolyte_spawn_egg", props -> new GodAcolyteSpawnEggItem(SinGod.GREED, props));
    public static final DeferredItem<GodAcolyteSpawnEggItem> WRATH_ACOLYTE_SPAWN_EGG = ITEMS.registerItem(
            "wrath_acolyte_spawn_egg", props -> new GodAcolyteSpawnEggItem(SinGod.WRATH, props));
    public static final DeferredItem<GodAcolyteSpawnEggItem> GLUTTONY_ACOLYTE_SPAWN_EGG = ITEMS.registerItem(
            "gluttony_acolyte_spawn_egg", props -> new GodAcolyteSpawnEggItem(SinGod.GLUTTONY, props));
    public static final DeferredItem<GodAcolyteSpawnEggItem> PRIDE_ACOLYTE_SPAWN_EGG = ITEMS.registerItem(
            "pride_acolyte_spawn_egg", props -> new GodAcolyteSpawnEggItem(SinGod.PRIDE, props));
    public static final DeferredItem<GodAcolyteSpawnEggItem> ENVY_ACOLYTE_SPAWN_EGG = ITEMS.registerItem(
            "envy_acolyte_spawn_egg", props -> new GodAcolyteSpawnEggItem(SinGod.ENVY, props));
    public static final DeferredItem<GodAcolyteSpawnEggItem> SLOTH_ACOLYTE_SPAWN_EGG = ITEMS.registerItem(
            "sloth_acolyte_spawn_egg", props -> new GodAcolyteSpawnEggItem(SinGod.SLOTH, props));
    public static final DeferredItem<GodAcolyteSpawnEggItem> LUST_ACOLYTE_SPAWN_EGG = ITEMS.registerItem(
            "lust_acolyte_spawn_egg", props -> new GodAcolyteSpawnEggItem(SinGod.LUST, props));

    public static final Map<SinGod, DeferredItem<GodAcolyteSpawnEggItem>> ACOLYTE_SPAWN_EGGS = new EnumMap<>(SinGod.class);
    static {
        ACOLYTE_SPAWN_EGGS.put(SinGod.GREED, GREED_ACOLYTE_SPAWN_EGG);
        ACOLYTE_SPAWN_EGGS.put(SinGod.WRATH, WRATH_ACOLYTE_SPAWN_EGG);
        ACOLYTE_SPAWN_EGGS.put(SinGod.GLUTTONY, GLUTTONY_ACOLYTE_SPAWN_EGG);
        ACOLYTE_SPAWN_EGGS.put(SinGod.PRIDE, PRIDE_ACOLYTE_SPAWN_EGG);
        ACOLYTE_SPAWN_EGGS.put(SinGod.ENVY, ENVY_ACOLYTE_SPAWN_EGG);
        ACOLYTE_SPAWN_EGGS.put(SinGod.SLOTH, SLOTH_ACOLYTE_SPAWN_EGG);
        ACOLYTE_SPAWN_EGGS.put(SinGod.LUST, LUST_ACOLYTE_SPAWN_EGG);
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SEVEN_DEADLY_GODS_TAB =
            CREATIVE_MODE_TABS.register("seven_deadly_gods_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.sevendeadlygods"))
                    .icon(() -> new ItemStack(GREED_ALTAR_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        // 1. Bloques de Altar de los 7 Dioses del Pecado
                        output.accept(GREED_ALTAR_ITEM.get());
                        output.accept(WRATH_ALTAR_ITEM.get());
                        output.accept(GLUTTONY_ALTAR_ITEM.get());
                        output.accept(PRIDE_ALTAR_ITEM.get());
                        output.accept(ENVY_ALTAR_ITEM.get());
                        output.accept(SLOTH_ALTAR_ITEM.get());
                        output.accept(LUST_ALTAR_ITEM.get());

                        // 2. Instrumento de Guerra Santa
                        output.accept(DIVINE_WAR_HORN.get());

                        // 3. Armas Reliquia Sagradas de los 7 Dioses (Armas sobre-encantadas del Avatar)
                        for (ItemStack weapon : DivineEquipmentHelper.getAllGodRelicWeapons(parameters.holders())) {
                            output.accept(weapon);
                        }

                        // 4. Artefactos Sagrados y Armas de Culto del Comercio Divino
                        for (ItemStack artifact : DivineTradingRegistry.getAllDivineTradeArtifacts(parameters.holders())) {
                            output.accept(artifact);
                        }

                        // 5. Trofeos Sagrados de Deicidio
                        for (ItemStack trophy : DivineEquipmentHelper.getAllDeicideTrophies(parameters.holders())) {
                            output.accept(trophy);
                        }

                        // 6. Huevos Generadores de Acólitos
                        for (DeferredItem<GodAcolyteSpawnEggItem> egg : ACOLYTE_SPAWN_EGGS.values()) {
                            output.accept(egg.get());
                        }

                        // 7. Biblioteca Sagrada: 42 libros sagrados de revelaciones y lore
                        for (SinGod god : SinGod.values()) {
                            for (SinLoreLibrary.LoreBookEntry entry : SinLoreLibrary.getBooksForGod(god)) {
                                output.accept(entry.toWrittenBookStack());
                            }
                        }
                    })
                    .build());

    private ModItems() {
    }
}
