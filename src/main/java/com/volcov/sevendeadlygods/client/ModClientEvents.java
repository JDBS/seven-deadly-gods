package com.volcov.sevendeadlygods.client;

import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.client.model.GodAcolyteModel;
import com.volcov.sevendeadlygods.client.renderer.GodAcolyteRenderer;
import com.volcov.sevendeadlygods.client.renderer.GodAvatarRenderer;
import com.volcov.sevendeadlygods.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = SevenDeadlyGodsMod.MOD_ID, value = Dist.CLIENT)
public final class ModClientEvents {
    private ModClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GodAcolyteModel.LAYER_LOCATION, GodAcolyteModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GOD_ACOLYTE.get(), GodAcolyteRenderer::new);
        event.registerEntityRenderer(ModEntities.GOD_AVATAR.get(), GodAvatarRenderer::new);
    }
}
