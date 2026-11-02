package com.volcov.sevendeadlygods.registry;

import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.god.SinGod;
import com.volcov.sevendeadlygods.item.DivineWarHornItem;
import com.volcov.sevendeadlygods.lore.SinLoreLibrary;
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

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SEVEN_DEADLY_GODS_TAB =
            CREATIVE_MODE_TABS.register("seven_deadly_gods_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.sevendeadlygods"))
                    .icon(() -> new ItemStack(GREED_ALTAR_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(GREED_ALTAR_ITEM.get());
                        output.accept(WRATH_ALTAR_ITEM.get());
                        output.accept(GLUTTONY_ALTAR_ITEM.get());
                        output.accept(PRIDE_ALTAR_ITEM.get());
                        output.accept(ENVY_ALTAR_ITEM.get());
                        output.accept(SLOTH_ALTAR_ITEM.get());
                        output.accept(LUST_ALTAR_ITEM.get());
                        output.accept(DIVINE_WAR_HORN.get());
                        // Incluir todos los 42 libros sagrados de lore en la pestana creativa para facil inspeccion
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
