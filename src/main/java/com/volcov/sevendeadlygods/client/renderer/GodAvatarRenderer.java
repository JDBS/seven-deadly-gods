package com.volcov.sevendeadlygods.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.entity.GodAvatarEntity;
import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.Map;

/**
 * Renderer del Avatar de un Dios del Pecado:
 * - Modelo humanoide vanilla a escala 1.5x de altura en comparacion con el jugador.
 * - Capa emisiva de brillo en los ojos (EyesLayer).
 * - Soporte completo para armaduras y armas sobre-encantadas equipadas.
 */
public class GodAvatarRenderer extends HumanoidMobRenderer<GodAvatarEntity, GodAvatarRenderState, HumanoidModel<GodAvatarRenderState>> {
    private static final Map<SinGod, Identifier> SKIN_TEXTURES = new EnumMap<>(SinGod.class);
    private static final RenderType GLOWING_EYES_RENDER_TYPE = RenderTypes.eyes(
            Identifier.fromNamespaceAndPath(SevenDeadlyGodsMod.MOD_ID, "textures/entity/avatar/avatar_glowing_eyes.png")
    );

    static {
        for (SinGod god : SinGod.values()) {
            SKIN_TEXTURES.put(god, Identifier.fromNamespaceAndPath(
                    SevenDeadlyGodsMod.MOD_ID,
                    "textures/entity/avatar/" + god.getId() + "_avatar.png"
            ));
        }
    }

    public GodAvatarRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.75F);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new),
                context.getEquipmentRenderer()
        ));
        // Capa de ojos brillantes divinos
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOWING_EYES_RENDER_TYPE;
            }
        });
    }

    @Override
    public GodAvatarRenderState createRenderState() {
        return new GodAvatarRenderState();
    }

    @Override
    public void extractRenderState(GodAvatarEntity entity, GodAvatarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.god = entity.getGod();
        state.isWrathHunter = entity.isWrathHunter();
    }

    @Override
    protected void scale(GodAvatarRenderState state, PoseStack poseStack) {
        // Escala 1.5x de altura en comparacion con el jugador
        poseStack.scale(1.5F, 1.5F, 1.5F);
    }

    @Override
    public Identifier getTextureLocation(GodAvatarRenderState state) {
        return SKIN_TEXTURES.getOrDefault(state.god, SKIN_TEXTURES.get(SinGod.PRIDE));
    }
}
