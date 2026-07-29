package dev.upiscium.frontierprotocol.client.render;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
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
                CubeListBuilder.create().texOffs(0, 0).addBox(6.0F, 5.0F, -0.75F, 4.0F, 6.0F, 1.0F),
                PartPose.ZERO);
        var gear = root.addOrReplaceChild("gear", CubeListBuilder.create(), PartPose.offset(8.0F, 8.0F, 0.0F));
        gear.addOrReplaceChild(
                "top", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -5.0F, -1.0F, 10.0F, 1.0F, 1.0F), PartPose.ZERO);
        gear.addOrReplaceChild(
                "bottom", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 4.0F, -1.0F, 10.0F, 1.0F, 1.0F), PartPose.ZERO);
        gear.addOrReplaceChild(
                "left", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -4.0F, -1.0F, 1.0F, 8.0F, 1.0F), PartPose.ZERO);
        gear.addOrReplaceChild(
                "right", CubeListBuilder.create().texOffs(0, 0).addBox(4.0F, -4.0F, -1.0F, 1.0F, 8.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild(
                "lights",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(2.0F, 2.0F, -1.25F, 12.0F, 1.0F, 0.5F)
                        .addBox(2.0F, 13.0F, -1.25F, 12.0F, 1.0F, 0.5F)
                        .addBox(2.0F, 3.0F, -1.25F, 1.0F, 10.0F, 0.5F)
                        .addBox(13.0F, 3.0F, -1.25F, 1.0F, 10.0F, 0.5F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }
}
