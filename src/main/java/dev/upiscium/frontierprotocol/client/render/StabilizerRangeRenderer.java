package dev.upiscium.frontierprotocol.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import dev.upiscium.frontierprotocol.config.FrontierProtocolClientConfig;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerBlockEntity;
import dev.upiscium.frontierprotocol.stabilizer.StabilizerStatus;
import dev.upiscium.frontierprotocol.stabilizer.display.StabilizerDisplaySnapshot;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class StabilizerRangeRenderer {
    private static final RenderType LINES = RenderType.lines();
    private static final double VERTICAL_DISTANCE = 32.0;

    private StabilizerRangeRenderer() {}

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || !FrontierProtocolClientConfig.SHOW_STABILIZER_RANGE_OVERLAY.get()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui
                || minecraft.screen != null
                || minecraft.player == null
                || minecraft.player.isSpectator()
                || minecraft.level == null
                || !GogglesItem.isWearingGoggles(minecraft.player)
                || (FrontierProtocolClientConfig.RANGE_OVERLAY_REQUIRES_SNEAKING.get()
                        && !minecraft.player.isShiftKeyDown())) return;
        if (!(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || !(minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof StabilizerBlockEntity stabilizer)
                || stabilizer.isVirtual()
                || stabilizer.getLevel() != minecraft.level) return;
        StabilizerDisplaySnapshot snapshot = stabilizer.displaySnapshot();
        if (snapshot == null) return;

        StabilizerRangeGeometry.RangeBounds bounds = StabilizerRangeGeometry.from(
                new ChunkPos(hit.getBlockPos()),
                snapshot.chunkRadius(),
                minecraft.level.getMinBuildHeight(),
                minecraft.level.getMaxBuildHeight());
        double horizontalY = Mth.clamp(
                        minecraft.player.getY(), bounds.minBuildHeight(), bounds.maxBuildHeight() - 1)
                + 0.02;
        Color color = color(snapshot.status());
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(LINES);
        Vec3 camera = event.getCamera().getPosition();

        poseStack.pushPose();
        try {
            emitAll(poseStack, consumer, bounds.horizontalLines(horizontalY), camera, color);
            if (FrontierProtocolClientConfig.SHOW_RANGE_VERTICAL_CORNERS.get()) {
                double minY = Math.max(bounds.minBuildHeight(), minecraft.player.getY() - VERTICAL_DISTANCE);
                double maxY = Math.min(bounds.maxBuildHeight(), minecraft.player.getY() + VERTICAL_DISTANCE);
                emitAll(poseStack, consumer, bounds.verticalCorners(minY, maxY), camera, color);
            }
        } finally {
            buffers.endBatch(LINES);
            poseStack.popPose();
        }
    }

    private static void emitAll(
            PoseStack poseStack,
            VertexConsumer consumer,
            List<StabilizerRangeGeometry.LineSegment> lines,
            Vec3 camera,
            Color color) {
        for (StabilizerRangeGeometry.LineSegment line : lines) {
            float alpha = line.kind() == StabilizerRangeGeometry.LineKind.INTERNAL ? 0.65F : 0.95F;
            emitLine(poseStack, consumer, line, camera, color, alpha);
        }
    }

    private static void emitLine(
            PoseStack poseStack,
            VertexConsumer consumer,
            StabilizerRangeGeometry.LineSegment line,
            Vec3 camera,
            Color color,
            float alpha) {
        float startX = (float) (line.startX() - camera.x);
        float startY = (float) (line.startY() - camera.y);
        float startZ = (float) (line.startZ() - camera.z);
        float endX = (float) (line.endX() - camera.x);
        float endY = (float) (line.endY() - camera.y);
        float endZ = (float) (line.endZ() - camera.z);
        float dx = endX - startX;
        float dy = endY - startY;
        float dz = endZ - startZ;
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (length <= Mth.EPSILON) return;
        dx /= length;
        dy /= length;
        dz /= length;
        PoseStack.Pose pose = poseStack.last();
        consumer.addVertex(pose, startX, startY, startZ)
                .setColor(color.red(), color.green(), color.blue(), alpha)
                .setNormal(pose, dx, dy, dz);
        consumer.addVertex(pose, endX, endY, endZ)
                .setColor(color.red(), color.green(), color.blue(), alpha)
                .setNormal(pose, dx, dy, dz);
    }

    private static Color color(StabilizerStatus status) {
        return switch (status) {
            case ACTIVE -> new Color(0.18F, 0.88F, 0.65F);
            case GRACE_PERIOD -> new Color(1.0F, 0.62F, 0.12F);
            case OFFLINE -> new Color(0.95F, 0.20F, 0.18F);
        };
    }

    private record Color(float red, float green, float blue) {}
}
