package com.volcov.sevendeadlygods.registry;

import com.mojang.serialization.MapCodec;
import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.lore.SinLoreLootModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, SevenDeadlyGodsMod.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<SinLoreLootModifier>> SIN_LORE_CHEST_MODIFIER =
            LOOT_MODIFIER_SERIALIZERS.register("sin_lore_chest_modifier", () -> SinLoreLootModifier.CODEC);

    private ModLootModifiers() {
    }
}
