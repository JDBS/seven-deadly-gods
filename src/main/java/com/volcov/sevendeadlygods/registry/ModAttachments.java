package com.volcov.sevendeadlygods.registry;

import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.favor.PlayerSinData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SevenDeadlyGodsMod.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerSinData>> PLAYER_SIN_DATA =
            ATTACHMENT_TYPES.register(
                    "player_sin_data",
                    () -> AttachmentType.serializable(PlayerSinData::new)
                            .copyOnDeath()
                            .build()
            );

    private ModAttachments() {
    }
}
