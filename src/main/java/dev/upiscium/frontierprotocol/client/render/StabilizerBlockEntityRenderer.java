package dev.upiscium.frontierprotocol.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerBlockEntity;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerStatus;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerTier;
import dev.upiscium.frontierprotocol.stabilizer.Tier1StabilizerAnimation;
import dev.upiscium.frontierprotocol.stabilizer.TierOneStabilizerBlock;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class StabilizerBlockEntityRenderer implements BlockEntityRenderer<StabilizerBlockEntity> {
    private static final ResourceLocation CORE_TEXTURE = texture("tier_1_stabilizer_core.png");
    private static final ResourceLocation GEAR_TEXTURE = texture("tier_1_stabilizer_gear.png");
    private static final ResourceLocation LIGHT_TEXTURE = texture("tier_1_stabilizer_light_mask.png");
    private final ModelPart core;
    private final ModelPart gear;
    private final ModelPart lights;

    public StabilizerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart root = context.bakeLayer(StabilizerRenderModels.TIER_1);
        core = root.getChild("core");
        gear = root.getChild("gear");
        lights = root.getChild("lights");
    }

    @Override
    public void render(
            StabilizerBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        if (blockEntity.tier() != StabilizerTier.TIER_1
                || !blockEntity.getBlockState().hasProperty(TierOneStabilizerBlock.FACING)) return;
        var snapshot = blockEntity.displaySnapshot();
        StabilizerStatus status = snapshot == null ? StabilizerStatus.OFFLINE : snapshot.status();

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotation(blockEntity.getBlockState().getValue(TierOneStabilizerBlock.FACING))));
        poseStack.translate(-0.5F, 0.5F, -0.5F);
        poseStack.scale(1.0F, -1.0F, 1.0F);

        gear.xRot = 0.0F;
        gear.yRot = 0.0F;
        gear.zRot = -Mth.DEG_TO_RAD
                * Tier1StabilizerAnimation.renderedAngle(blockEntity.clientCoreAngle(partialTick));
        VertexConsumer gearBuffer = buffers.getBuffer(RenderType.entityCutoutNoCull(GEAR_TEXTURE));
        gear.render(poseStack, gearBuffer, packedLight, OverlayTexture.NO_OVERLAY);

        if (status == StabilizerStatus.ACTIVE) {
            float pulse = Tier1StabilizerAnimation.corePulse(gameTime(blockEntity, partialTick));
            core.render(
                    poseStack,
                    buffers.getBuffer(RenderType.entityTranslucentEmissive(CORE_TEXTURE)),
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    color(0xD8FCFF, pulse));
        }

        float alpha = switch (status) {
            case ACTIVE -> 1.0F;
            case OFFLINE -> 0.55F;
            case GRACE_PERIOD -> snapshot == null
                    ? 0.25F
                    : Tier1StabilizerAnimation.graceLightAlpha(
                            gameTime(blockEntity, partialTick),
                            snapshot.graceRemainingTicks(),
                            snapshot.graceDurationTicks());
        };
        lights.render(
                poseStack,
                buffers.getBuffer(RenderType.entityTranslucentEmissive(LIGHT_TEXTURE)),
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                color(Tier1StabilizerAnimation.stateColor(status), alpha));
        poseStack.popPose();
    }

    private static float yRotation(Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> throw new IllegalArgumentException("Tier 1 facing must be horizontal");
        };
    }

    private static double gameTime(StabilizerBlockEntity blockEntity, float partialTick) {
        return (blockEntity.getLevel().getGameTime() + partialTick) / 20.0;
    }

    private static int color(int rgb, float alpha) {
        return Math.clamp(Math.round(alpha * 255.0F), 0, 255) << 24 | rgb;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                FrontierProtocolMod.MOD_ID, "textures/block/" + name);
    }
}
