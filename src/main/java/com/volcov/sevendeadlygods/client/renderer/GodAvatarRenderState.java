package com.volcov.sevendeadlygods.client.renderer;

import com.volcov.sevendeadlygods.god.SinGod;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class GodAvatarRenderState extends HumanoidRenderState {
    public SinGod god = SinGod.PRIDE;
    public boolean isWrathHunter = false;
}
