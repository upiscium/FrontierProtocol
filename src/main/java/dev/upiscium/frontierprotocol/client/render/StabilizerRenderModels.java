package dev.upiscium.frontierprotocol.client.render;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public final class StabilizerRenderModels {
    public static final ModelLayerLocation TIER_1 = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, "tier_1_stabilizer"), "dynamic");

    private StabilizerRenderModels() {}

    public static LayerDefinition createTierOneLayer() {
        MeshDefinition mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild(
                "core",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F)
                        .texOffs(0, 0)
                        .addBox(-1.0F, -3.0F, -1.0F, 2.0F, 1.0F, 2.0F)
                        .texOffs(0, 0)
                        .addBox(-1.0F, 2.0F, -1.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offset(8.0F, 8.0F, 8.0F));
        var gear = root.addOrReplaceChild("gear", CubeListBuilder.create(), PartPose.offset(8.0F, 8.0F, 0.0F));
        gear.addOrReplaceChild(
                "top", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -4.0F, -0.75F, 6.0F, 1.0F, 0.75F), PartPose.ZERO);
        gear.addOrReplaceChild(
                "bottom", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, 3.0F, -0.75F, 6.0F, 1.0F, 0.75F), PartPose.ZERO);
        gear.addOrReplaceChild(
                "left", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -3.0F, -0.75F, 1.0F, 6.0F, 0.75F), PartPose.ZERO);
        gear.addOrReplaceChild(
                "right", CubeListBuilder.create().texOffs(0, 0).addBox(3.0F, -3.0F, -0.75F, 1.0F, 6.0F, 0.75F), PartPose.ZERO);
        addGearTeeth(gear);

        var lights = root.addOrReplaceChild("lights", CubeListBuilder.create(), PartPose.ZERO);
        lights.addOrReplaceChild("front", frontLights(), PartPose.ZERO);
        lights.addOrReplaceChild("left", leftLights(), PartPose.ZERO);
        lights.addOrReplaceChild("right", rightLights(), PartPose.ZERO);
        lights.addOrReplaceChild("top", topLights(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    private static void addGearTeeth(PartDefinition gear) {
        gear.addOrReplaceChild(
                "tooth_top", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -5.0F, -0.75F, 2.0F, 1.0F, 0.75F), PartPose.ZERO);
        gear.addOrReplaceChild(
                "tooth_bottom", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 4.0F, -0.75F, 2.0F, 1.0F, 0.75F), PartPose.ZERO);
        gear.addOrReplaceChild(
                "tooth_left", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -1.0F, -0.75F, 1.0F, 2.0F, 0.75F), PartPose.ZERO);
        gear.addOrReplaceChild(
                "tooth_right", CubeListBuilder.create().texOffs(0, 0).addBox(4.0F, -1.0F, -0.75F, 1.0F, 2.0F, 0.75F), PartPose.ZERO);
        gear.addOrReplaceChild(
                "tooth_nw", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -4.5F, -0.75F, 1.5F, 1.5F, 0.75F), PartPose.ZERO);
        gear.addOrReplaceChild(
                "tooth_ne", CubeListBuilder.create().texOffs(0, 0).addBox(3.0F, -4.5F, -0.75F, 1.5F, 1.5F, 0.75F), PartPose.ZERO);
        gear.addOrReplaceChild(
                "tooth_sw", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, 3.0F, -0.75F, 1.5F, 1.5F, 0.75F), PartPose.ZERO);
        gear.addOrReplaceChild(
                "tooth_se", CubeListBuilder.create().texOffs(0, 0).addBox(3.0F, 3.0F, -0.75F, 1.5F, 1.5F, 0.75F), PartPose.ZERO);
    }

    private static CubeListBuilder frontLights() {
        return CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(4.0F, 3.0F, -0.5F, 8.0F, 0.5F, 0.5F)
                .addBox(4.0F, 12.5F, -0.5F, 8.0F, 0.5F, 0.5F)
                .addBox(3.0F, 4.0F, -0.5F, 0.5F, 8.0F, 0.5F)
                .addBox(12.5F, 4.0F, -0.5F, 0.5F, 8.0F, 0.5F)
                .addBox(2.0F, 2.0F, -0.5F, 2.0F, 0.5F, 0.5F)
                .addBox(12.0F, 13.5F, -0.5F, 2.0F, 0.5F, 0.5F);
    }

    private static CubeListBuilder leftLights() {
        return CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-0.5F, 3.0F, 4.0F, 0.5F, 0.5F, 8.0F)
                .addBox(-0.5F, 12.5F, 4.0F, 0.5F, 0.5F, 8.0F)
                .addBox(-0.5F, 4.0F, 3.0F, 0.5F, 8.0F, 0.5F)
                .addBox(-0.5F, 4.0F, 12.5F, 0.5F, 8.0F, 0.5F)
                .addBox(-0.5F, 2.0F, 2.0F, 0.5F, 0.5F, 2.0F)
                .addBox(-0.5F, 13.5F, 12.0F, 0.5F, 0.5F, 2.0F);
    }

    private static CubeListBuilder rightLights() {
        return CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(16.0F, 3.0F, 4.0F, 0.5F, 0.5F, 8.0F)
                .addBox(16.0F, 12.5F, 4.0F, 0.5F, 0.5F, 8.0F)
                .addBox(16.0F, 4.0F, 3.0F, 0.5F, 8.0F, 0.5F)
                .addBox(16.0F, 4.0F, 12.5F, 0.5F, 8.0F, 0.5F)
                .addBox(16.0F, 2.0F, 2.0F, 0.5F, 0.5F, 2.0F)
                .addBox(16.0F, 13.5F, 12.0F, 0.5F, 0.5F, 2.0F);
    }

    private static CubeListBuilder topLights() {
        return CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(4.0F, -0.5F, 3.0F, 8.0F, 0.5F, 0.5F)
                .addBox(4.0F, -0.5F, 12.5F, 8.0F, 0.5F, 0.5F)
                .addBox(3.0F, -0.5F, 4.0F, 0.5F, 0.5F, 8.0F)
                .addBox(12.5F, -0.5F, 4.0F, 0.5F, 0.5F, 8.0F)
                .addBox(2.0F, -0.5F, 2.0F, 2.0F, 0.5F, 0.5F)
                .addBox(12.0F, -0.5F, 13.5F, 2.0F, 0.5F, 0.5F);
    }
}
