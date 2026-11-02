package com.volcov.sevendeadlygods.client.renderer;

import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.client.model.GodAcolyteModel;
import com.volcov.sevendeadlygods.entity.GodAcolyteEntity;
import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.Map;

public class GodAcolyteRenderer extends MobRenderer<GodAcolyteEntity, GodAcolyteRenderState, GodAcolyteModel> {
    private static final Map<SinGod, Identifier> TEXTURES = new EnumMap<>(SinGod.class);

    static {
        for (SinGod god : SinGod.values()) {
            TEXTURES.put(god, Identifier.fromNamespaceAndPath(
                    SevenDeadlyGodsMod.MOD_ID,
                    "textures/entity/acolyte/" + god.getId() + "_acolyte.png"
            ));
        }
    }

    public GodAcolyteRenderer(EntityRendererProvider.Context context) {
        super(context, new GodAcolyteModel(context.bakeLayer(GodAcolyteModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public GodAcolyteRenderState createRenderState() {
        return new GodAcolyteRenderState();
    }

    @Override
    public void extractRenderState(GodAcolyteEntity entity, GodAcolyteRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver, partialTick);
        state.god = entity.getGod();
        state.isAllied = entity.isAlliedProtector();
    }

    @Override
    public Identifier getTextureLocation(GodAcolyteRenderState state) {
        return TEXTURES.getOrDefault(state.god, TEXTURES.get(SinGod.WRATH));
    }
}
