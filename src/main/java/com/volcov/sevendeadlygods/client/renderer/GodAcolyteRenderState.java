package com.volcov.sevendeadlygods.client.renderer;

import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;

public class GodAcolyteRenderState extends ArmedEntityRenderState {
    public SinGod god = SinGod.WRATH;
    public boolean isAllied = false;
}
