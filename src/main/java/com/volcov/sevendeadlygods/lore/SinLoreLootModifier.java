package com.volcov.sevendeadlygods.lore;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.volcov.sevendeadlygods.config.ModConfig;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.Optional;

/**
 * GlobalLootModifier que inyecta los 42 Libros Sagrados de Relatos de los 7 Pecados Capitales
 * en los cofres de estructuras del mundo (mazmorras, templos, aldeas, bastiones, mansiones, etc.).
 */
public class SinLoreLootModifier extends LootModifier {
    @SuppressWarnings("null")
    public static final MapCodec<SinLoreLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).apply(instance, SinLoreLootModifier::new)
    );

    public SinLoreLootModifier(Optional<Holder<LootItemCondition>> condition, int priority) {
        super(condition, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Identifier tableId = context.getQueriedLootTableId();
        if (tableId != null && tableId.getPath().contains("chests/")) {
            double chance = ModConfig.LORE_BOOK_CHEST_CHANCE.get();
            if (context.getRandom().nextDouble() < chance) {
                ItemStack loreBook = SinLoreLibrary.createRandomLoreBook(context.getRandom());
                if (!loreBook.isEmpty()) {
                    generatedLoot.add(loreBook);
                }
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
