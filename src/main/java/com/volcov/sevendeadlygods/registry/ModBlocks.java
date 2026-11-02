package com.volcov.sevendeadlygods.registry;

import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.block.SinAltarBlock;
import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(SevenDeadlyGodsMod.MOD_ID);

    public static final DeferredBlock<SinAltarBlock> GREED_ALTAR = registerAltar("greed_altar", SinGod.GREED, MapColor.GOLD);
    public static final DeferredBlock<SinAltarBlock> WRATH_ALTAR = registerAltar("wrath_altar", SinGod.WRATH, MapColor.NETHER);
    public static final DeferredBlock<SinAltarBlock> GLUTTONY_ALTAR = registerAltar("gluttony_altar", SinGod.GLUTTONY, MapColor.COLOR_ORANGE);
    public static final DeferredBlock<SinAltarBlock> PRIDE_ALTAR = registerAltar("pride_altar", SinGod.PRIDE, MapColor.QUARTZ);
    public static final DeferredBlock<SinAltarBlock> ENVY_ALTAR = registerAltar("envy_altar", SinGod.ENVY, MapColor.COLOR_CYAN);
    public static final DeferredBlock<SinAltarBlock> SLOTH_ALTAR = registerAltar("sloth_altar", SinGod.SLOTH, MapColor.COLOR_BLUE);
    public static final DeferredBlock<SinAltarBlock> LUST_ALTAR = registerAltar("lust_altar", SinGod.LUST, MapColor.COLOR_PINK);

    private ModBlocks() {
    }

    private static DeferredBlock<SinAltarBlock> registerAltar(String name, SinGod god, MapColor color) {
        return BLOCKS.registerBlock(
                name,
                props -> new SinAltarBlock(god, props),
                () -> BlockBehaviour.Properties.of()
                        .mapColor(color)
                        .strength(3.5F, 12.0F)
                        .sound(SoundType.DEEPSLATE_TILES)
                        .lightLevel(state -> 7)
                        .noOcclusion()
        );
    }

    public static Block getAltarFor(SinGod god) {
        return switch (god) {
            case GREED -> GREED_ALTAR.get();
            case WRATH -> WRATH_ALTAR.get();
            case GLUTTONY -> GLUTTONY_ALTAR.get();
            case PRIDE -> PRIDE_ALTAR.get();
            case ENVY -> ENVY_ALTAR.get();
            case SLOTH -> SLOTH_ALTAR.get();
            case LUST -> LUST_ALTAR.get();
        };
    }
}
