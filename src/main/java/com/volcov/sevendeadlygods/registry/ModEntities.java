package com.volcov.sevendeadlygods.registry;

import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.entity.GodAcolyteEntity;
import com.volcov.sevendeadlygods.entity.GodAvatarEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, SevenDeadlyGodsMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GodAcolyteEntity>> GOD_ACOLYTE =
            ENTITY_TYPES.register(
                    "god_acolyte",
                    () -> EntityType.Builder.of(GodAcolyteEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(10)
                            .build(ResourceKey.create(
                                    Registries.ENTITY_TYPE,
                                    Identifier.fromNamespaceAndPath(SevenDeadlyGodsMod.MOD_ID, "god_acolyte")
                            ))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<GodAvatarEntity>> GOD_AVATAR =
            ENTITY_TYPES.register(
                    "god_avatar",
                    () -> EntityType.Builder.of(GodAvatarEntity::new, MobCategory.MONSTER)
                            .sized(0.9F, 2.9F)
                            .fireImmune()
                            .clientTrackingRange(12)
                            .build(ResourceKey.create(
                                    Registries.ENTITY_TYPE,
                                    Identifier.fromNamespaceAndPath(SevenDeadlyGodsMod.MOD_ID, "god_avatar")
                            ))
            );

    private ModEntities() {
    }

    public static void onRegisterAttributes(EntityAttributeCreationEvent event) {
        event.put(GOD_ACOLYTE.get(), GodAcolyteEntity.createAttributes().build());
        event.put(GOD_AVATAR.get(), GodAvatarEntity.createAttributes().build());
    }
}
