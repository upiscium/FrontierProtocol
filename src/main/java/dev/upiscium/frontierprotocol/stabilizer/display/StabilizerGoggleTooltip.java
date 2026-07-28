package dev.upiscium.frontierprotocol.stabilizer.display;

import dev.upiscium.frontierprotocol.stabilizer.StabilizerStatus;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

public final class StabilizerGoggleTooltip {
    private StabilizerGoggleTooltip() {}

    public static boolean add(
            List<Component> tooltip,
            boolean sneaking,
            StabilizerDisplaySnapshot snapshot,
            ChunkPos center,
            float theoreticalRpm,
            boolean overStressed) {
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("frontier_protocol.goggles.title").withStyle(ChatFormatting.GRAY));
        addLine(tooltip, "frontier_protocol.goggles.tier", tierName(snapshot));
        addLine(
                tooltip,
                "frontier_protocol.goggles.state",
                statusName(snapshot.status()).copy().withStyle(statusColor(snapshot.status())));
        if (overStressed) {
            addLine(tooltip, "frontier_protocol.goggles.rotation_effective", Component.literal("0 RPM"));
            addLine(
                    tooltip,
                    "frontier_protocol.goggles.theoretical_rpm",
                    Component.literal(formatRpm(theoreticalRpm) + " RPM"));
            addLine(
                    tooltip,
                    "frontier_protocol.goggles.required_rpm",
                    Component.literal(snapshot.minimumRpm() + " RPM"));
        } else {
            tooltip.add(Component.translatable(
                            "frontier_protocol.goggles.rotation",
                            formatRpm(theoreticalRpm),
                            snapshot.minimumRpm())
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable(
                        "frontier_protocol.goggles.cell_buffer", snapshot.cellCount(), snapshot.cellCapacity())
                .withStyle(ChatFormatting.GRAY));
        addLine(tooltip, timeLabel(snapshot.status()), Component.literal(DisplayDurationFormatter.formatTicks(timeTicks(snapshot))));
        tooltip.add(Component.translatable(
                        "frontier_protocol.goggles.coverage", snapshot.coverageWidth(), snapshot.coverageWidth())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                        "frontier_protocol.goggles.suppressed",
                        snapshot.suppressedChunkCount(),
                        snapshot.coverageChunkCount())
                .withStyle(ChatFormatting.GRAY));
        StabilizerDiagnostic diagnostic = StabilizerDiagnostic.evaluate(
                overStressed,
                theoreticalRpm,
                snapshot.minimumRpm(),
                snapshot.cellCount(),
                snapshot.cellRemainingTicks(),
                snapshot.status());
        addLine(
                tooltip,
                "frontier_protocol.goggles.diagnostic",
                Component.translatable(diagnostic.translationKey()).withStyle(statusColor(snapshot.status())));

        if (sneaking) addDetails(tooltip, snapshot, center);
        return true;
    }

    public static boolean addSynchronizing(List<Component> tooltip) {
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("frontier_protocol.goggles.title").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("frontier_protocol.goggles.synchronizing").withStyle(ChatFormatting.YELLOW));
        return true;
    }

    private static void addDetails(
            List<Component> tooltip, StabilizerDisplaySnapshot snapshot, ChunkPos center) {
        int radius = snapshot.chunkRadius();
        addLine(
                tooltip,
                "frontier_protocol.goggles.center_chunk",
                Component.literal(center.x + ", " + center.z));
        addLine(
                tooltip,
                "frontier_protocol.goggles.chunk_x_range",
                Component.literal((center.x - radius) + " .. " + (center.x + radius)));
        addLine(
                tooltip,
                "frontier_protocol.goggles.chunk_z_range",
                Component.literal((center.z - radius) + " .. " + (center.z + radius)));
        tooltip.add(Component.translatable("frontier_protocol.goggles.full_height").withStyle(ChatFormatting.GRAY));
        addLine(
                tooltip,
                "frontier_protocol.goggles.stress_impact",
                Component.literal(String.format(Locale.ROOT, "%.1f", snapshot.stressImpact())));
        addLine(
                tooltip,
                "frontier_protocol.goggles.cell_duration",
                Component.literal(DisplayDurationFormatter.formatTicks(snapshot.cellDurationTicks())));
    }

    private static Component tierName(StabilizerDisplaySnapshot snapshot) {
        return Component.translatable("frontier_protocol.tier." + snapshot.tier().serializedName());
    }

    private static Component statusName(StabilizerStatus status) {
        return Component.translatable("frontier_protocol.status." + status.getSerializedName());
    }

    private static String timeLabel(StabilizerStatus status) {
        return switch (status) {
            case ACTIVE -> "frontier_protocol.goggles.cell_time";
            case GRACE_PERIOD -> "frontier_protocol.goggles.grace_time";
            case OFFLINE -> "frontier_protocol.goggles.stored_time";
        };
    }

    private static int timeTicks(StabilizerDisplaySnapshot snapshot) {
        return snapshot.status() == StabilizerStatus.GRACE_PERIOD
                ? snapshot.graceRemainingTicks()
                : snapshot.cellRemainingTicks();
    }

    private static ChatFormatting statusColor(StabilizerStatus status) {
        return switch (status) {
            case ACTIVE -> ChatFormatting.GREEN;
            case GRACE_PERIOD -> ChatFormatting.GOLD;
            case OFFLINE -> ChatFormatting.RED;
        };
    }

    private static String formatRpm(float rpm) {
        return String.format(Locale.ROOT, "%.1f", Math.abs(rpm));
    }

    private static void addLine(List<Component> tooltip, String key, Component value) {
        tooltip.add(Component.translatable(key, value.copy().withStyle(ChatFormatting.AQUA))
                .withStyle(ChatFormatting.GRAY));
    }
}
