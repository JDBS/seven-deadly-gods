package com.volcov.sevendeadlygods.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.volcov.sevendeadlygods.SevenDeadlyGodsMod;
import com.volcov.sevendeadlygods.client.renderer.GodAcolyteRenderState;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Modelo del Acolito / Sirviente de un Dios del Pecado:
 * - Proporcion de aldeano/illager con nariz, tunica larga y capucha profunda.
 * - Par de alas articuladas en la espalda (left_wing y right_wing).
 * - Brazos articulados para empuñar armas cuerpo a cuerpo.
 */
public class GodAcolyteModel extends EntityModel<GodAcolyteRenderState> implements ArmedModel<GodAcolyteRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(SevenDeadlyGodsMod.MOD_ID, "god_acolyte"),
            "main"
    );

    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public GodAcolyteModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Cabeza tipo aldeano + nariz + capucha exterior profunda
        PartDefinition head = root.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F)
                        .texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.55F)),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        head.addOrReplaceChild(
                "nose",
                CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, -2.0F, 0.0F)
        );

        // Torso y tunica ceremonial de aldeano
        root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F)
                        .texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 18.0F, 6.0F, new CubeDeformation(0.45F)),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // Brazos para sostener armas
        root.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create().texOffs(40, 46).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F)
        );
        root.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(40, 46).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F)
        );

        // Piernas
        root.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-2.0F, 12.0F, 0.0F)
        );
        root.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(2.0F, 12.0F, 0.0F)
        );

        // Alas divinas en la espalda
        root.addOrReplaceChild(
                "left_wing",
                CubeListBuilder.create().texOffs(36, 20).mirror().addBox(0.0F, -4.0F, 0.0F, 12.0F, 16.0F, 1.0F),
                PartPose.offset(1.5F, 3.0F, 3.2F)
        );
        root.addOrReplaceChild(
                "right_wing",
                CubeListBuilder.create().texOffs(36, 20).addBox(-12.0F, -4.0F, 0.0F, 12.0F, 16.0F, 1.0F),
                PartPose.offset(-1.5F, 3.0F, 3.2F)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(GodAcolyteRenderState state) {
        super.setupAnim(state);

        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;

        float walkPos = state.walkAnimationPos;
        float walkSpeed = state.walkAnimationSpeed;
        this.rightArm.xRot = Mth.cos(walkPos * 0.6662F + (float) Math.PI) * 1.6F * walkSpeed * 0.5F;
        this.leftArm.xRot = Mth.cos(walkPos * 0.6662F) * 1.6F * walkSpeed * 0.5F;
        this.rightLeg.xRot = Mth.cos(walkPos * 0.6662F) * 1.4F * walkSpeed;
        this.leftLeg.xRot = Mth.cos(walkPos * 0.6662F + (float) Math.PI) * 1.4F * walkSpeed;

        // Batido constante de las alas en la espalda
        float flap = Mth.cos(state.ageInTicks * 0.25F) * 0.35F + 0.45F;
        this.leftWing.yRot = -flap;
        this.rightWing.yRot = flap;
        this.leftWing.zRot = 0.12F;
        this.rightWing.zRot = -0.12F;
    }

    @Override
    public void translateToHand(GodAcolyteRenderState state, HumanoidArm arm, PoseStack poseStack) {
        ModelPart part = arm == HumanoidArm.LEFT ? this.leftArm : this.rightArm;
        part.translateAndRotate(poseStack);
    }
}
