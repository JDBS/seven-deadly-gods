package com.volcov.sevendeadlygods.registry;

import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.block.SinAltarBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SevenDeadlyGodsMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SinAltarBlockEntity>> SIN_ALTAR_BE =
            BLOCK_ENTITIES.register(
                    "sin_altar",
                    () -> new BlockEntityType<>(
                            SinAltarBlockEntity::new,
                            Set.of(
                                    ModBlocks.GREED_ALTAR.get(),
                                    ModBlocks.WRATH_ALTAR.get(),
                                    ModBlocks.GLUTTONY_ALTAR.get(),
                                    ModBlocks.PRIDE_ALTAR.get(),
                                    ModBlocks.ENVY_ALTAR.get(),
                                    ModBlocks.SLOTH_ALTAR.get(),
                                    ModBlocks.LUST_ALTAR.get()
                            )
                    )
            );

    private ModBlockEntities() {
    }
}
