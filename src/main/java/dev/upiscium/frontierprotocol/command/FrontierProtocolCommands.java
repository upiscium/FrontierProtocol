package dev.upiscium.frontierprotocol.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.data.TraitReloadListener;
import dev.upiscium.frontierprotocol.sector.SectorPos;
import dev.upiscium.frontierprotocol.sector.SectorServices;
import dev.upiscium.frontierprotocol.world.FrontierProtocolWorldData;
import dev.upiscium.frontierprotocol.protection.ProtectionSource;
import dev.upiscium.frontierprotocol.protection.ServerProtectionService;
import dev.upiscium.frontierprotocol.infection.ChunkInfectionState;
import dev.upiscium.frontierprotocol.infection.InfectionService;
import dev.upiscium.frontierprotocol.nutrition.FoodHistoryState;
import dev.upiscium.frontierprotocol.registry.ModAttachments;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class FrontierProtocolCommands {
    private FrontierProtocolCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("frontierprotocol").requires(source -> source.hasPermission(2));
        root.then(Commands.literal("sector")
                .then(Commands.literal("info")
                        .executes(context -> info(context, currentSector(context.getSource())))
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(context -> info(context, argumentSector(context))))))
                .then(Commands.literal("locate")
                        .then(Commands.argument("trait", StringArgumentType.word())
                                .executes(FrontierProtocolCommands::locate)))
                .then(Commands.literal("set")
                        .then(Commands.argument("trait", StringArgumentType.word())
                                .executes(context -> set(context, currentSector(context.getSource())))
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(context -> set(context, argumentSector(context))))))));
        root.then(Commands.literal("protection")
                .then(Commands.literal("status")
                        .executes(context -> protectionStatus(context, currentChunk(context.getSource())))
                        .then(chunkArguments((context, chunk) -> protectionStatus(context, chunk)))));
        root.then(Commands.literal("infection")
                .then(Commands.literal("get")
                        .executes(context -> infectionGet(context, currentChunk(context.getSource())))
                        .then(chunkArguments(FrontierProtocolCommands::infectionGet)))
                .then(Commands.literal("set")
                        .then(Commands.argument("pressure", IntegerArgumentType.integer(0))
                                .executes(context -> infectionSet(context, currentChunk(context.getSource())))
                                .then(chunkArguments(FrontierProtocolCommands::infectionSet))))
                .then(Commands.literal("clear")
                        .executes(context -> infectionClear(context, currentChunk(context.getSource())))
                        .then(chunkArguments(FrontierProtocolCommands::infectionClear))));
        root.then(Commands.literal("nutrition")
                .then(Commands.literal("inspect")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> nutritionInspect(context,
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> nutritionClear(context,
                                        EntityArgument.getPlayer(context, "player"))))));
        dispatcher.register(root);
    }

    private static int info(CommandContext<CommandSourceStack> context, SectorPos sector) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getServer().overworld();
        FrontierProtocolWorldData data = FrontierProtocolWorldData.get(level);
        Optional<ResourceLocation> trait = SectorServices.PLACEMENT.resolve(
                level.getSeed(), sector, data.originSector(), data.forcedTraitOverrides());
        sendSectorResult(source, data, sector, trait.map(ResourceLocation::toString).orElse("none"));
        return trait.isPresent() ? 1 : 0;
    }

    private static int locate(CommandContext<CommandSourceStack> context) {
        ResourceLocation trait = ResourceLocation.tryParse(StringArgumentType.getString(context, "trait"));
        if (trait == null || TraitReloadListener.definitions().stream().noneMatch(definition -> definition.id().equals(trait))) {
            context.getSource().sendFailure(Component.literal("Unknown sector trait: " + StringArgumentType.getString(context, "trait")));
            return 0;
        }
        ServerLevel level = context.getSource().getServer().overworld();
        FrontierProtocolWorldData data = FrontierProtocolWorldData.get(level);
        SectorPos origin = currentSector(context.getSource());
        int radius = FrontierProtocolServerConfig.LOCATE_RADIUS.getAsInt();
        for (int distance = 0; distance <= radius; distance++) {
            for (int dx = -distance; dx <= distance; dx++) {
                for (int dz = -distance; dz <= distance; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != distance) continue;
                    SectorPos candidate = offset(origin, dx, dz);
                    if (candidate != null && SectorServices.PLACEMENT.resolve(level.getSeed(), candidate,
                            data.originSector(), data.forcedTraitOverrides()).filter(trait::equals).isPresent()) {
                        sendSectorResult(context.getSource(), data, candidate, trait.toString());
                        return 1;
                    }
                }
            }
        }
        context.getSource().sendFailure(Component.literal("No " + trait + " sector within radius " + radius));
        return 0;
    }

    private static int set(CommandContext<CommandSourceStack> context, SectorPos sector) {
        String value = StringArgumentType.getString(context, "trait");
        ServerLevel level = context.getSource().getServer().overworld();
        FrontierProtocolWorldData data = FrontierProtocolWorldData.get(level);
        if (value.equals("none")) {
            data.clearOverride(sector);
            context.getSource().sendSuccess(() -> Component.literal("Cleared sector override at " + sector.x() + ", " + sector.z()), true);
            return 1;
        }
        ResourceLocation trait = ResourceLocation.tryParse(value);
        if (trait == null || TraitReloadListener.definitions().stream().noneMatch(definition -> definition.id().equals(trait))) {
            context.getSource().sendFailure(Component.literal("Unknown sector trait: " + value));
            return 0;
        }
        data.setOverride(sector, trait);
        context.getSource().sendSuccess(() -> Component.literal("Set sector " + sector.x() + ", " + sector.z() + " to " + trait), true);
        return 1;
    }

    private static void sendSectorResult(CommandSourceStack source, FrontierProtocolWorldData data, SectorPos sector, String trait) {
        long minChunkX = (long) sector.x() * data.sectorSizeChunks();
        long minChunkZ = (long) sector.z() * data.sectorSizeChunks();
        long maxChunkX = minChunkX + data.sectorSizeChunks() - 1L;
        long maxChunkZ = minChunkZ + data.sectorSizeChunks() - 1L;
        long centerBlockX = (minChunkX + maxChunkX + 1L) * 8L;
        long centerBlockZ = (minChunkZ + maxChunkZ + 1L) * 8L;
        source.sendSuccess(() -> Component.literal("Sector " + sector.x() + ", " + sector.z() + " trait=" + trait
                + " chunks=[" + minChunkX + ".." + maxChunkX + ", " + minChunkZ + ".." + maxChunkZ + "]"
                + " center=" + centerBlockX + ", " + centerBlockZ), false);
    }

    private static SectorPos currentSector(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();
        FrontierProtocolWorldData data = FrontierProtocolWorldData.get(level);
        return SectorPos.fromChunk(new ChunkPos(BlockPos.containing(source.getPosition())), data.sectorSizeChunks());
    }

    private static ChunkPos currentChunk(CommandSourceStack source) {
        return new ChunkPos(BlockPos.containing(source.getPosition()));
    }

    private static int protectionStatus(CommandContext<CommandSourceStack> context, ChunkPos chunk) {
        ServerLevel level = context.getSource().getServer().overworld();
        Optional<ProtectionSource> source = ServerProtectionService.INSTANCE.findSource(level, chunk);
        if (source.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "Chunk " + chunk.x + ", " + chunk.z + " is not protected"), false);
            return 0;
        }
        ProtectionSource protection = source.get();
        String detail = protection.blockPos().map(pos -> " at " + pos.toShortString()).orElse("");
        context.getSource().sendSuccess(() -> Component.literal(
                "Chunk " + chunk.x + ", " + chunk.z + " is protected by "
                        + protection.type().name().toLowerCase(java.util.Locale.ROOT) + detail), false);
        return 1;
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> chunkArguments(ChunkCommand command) {
        return Commands.argument("x", IntegerArgumentType.integer())
                .then(Commands.argument("z", IntegerArgumentType.integer())
                        .executes(context -> command.run(context, new ChunkPos(
                                IntegerArgumentType.getInteger(context, "x"),
                                IntegerArgumentType.getInteger(context, "z")))));
    }

    private static int infectionGet(CommandContext<CommandSourceStack> context, ChunkPos chunk) {
        ChunkInfectionState state = InfectionService.getState(context.getSource().getServer().overworld(), chunk);
        context.getSource().sendSuccess(() -> Component.literal("Chunk " + chunk.x + ", " + chunk.z
                + " pressure=" + state.pressure() + " stage="
                + state.stage(FrontierProtocolServerConfig.INFECTION_CORE_THRESHOLD.getAsInt()).name().toLowerCase(java.util.Locale.ROOT)
                + " activeLoadedTicks=" + state.activeLoadedTicks()), false);
        return state.pressure();
    }

    private static int infectionSet(CommandContext<CommandSourceStack> context, ChunkPos chunk) {
        int pressure = IntegerArgumentType.getInteger(context, "pressure");
        if (!InfectionService.setPressure(context.getSource().getServer().overworld(), chunk, pressure)) {
            context.getSource().sendFailure(Component.literal("Chunk must be loaded"));
            return 0;
        }
        return infectionGet(context, chunk);
    }

    private static int infectionClear(CommandContext<CommandSourceStack> context, ChunkPos chunk) {
        InfectionService.clear(context.getSource().getServer().overworld(), chunk);
        context.getSource().sendSuccess(() -> Component.literal("Cleared infection in chunk " + chunk.x + ", " + chunk.z), true);
        return 1;
    }

    private static int nutritionInspect(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        FoodHistoryState history = player.getData(ModAttachments.FOOD_HISTORY);
        String entries = history.entries().isEmpty() ? "empty" : history.entries().stream()
                .map(entry -> entry.item() + entry.category().map(category -> "[" + category + "]").orElse(""))
                .collect(java.util.stream.Collectors.joining(", "));
        context.getSource().sendSuccess(() -> Component.literal(
                "Nutrition history for " + player.getGameProfile().getName() + ": " + entries), false);
        return history.entries().size();
    }

    private static int nutritionClear(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        int removed = player.getData(ModAttachments.FOOD_HISTORY).entries().size();
        player.setData(ModAttachments.FOOD_HISTORY, FoodHistoryState.EMPTY);
        context.getSource().sendSuccess(() -> Component.literal(
                "Cleared " + removed + " nutrition entries for " + player.getGameProfile().getName()), true);
        return 1;
    }

    @FunctionalInterface
    private interface ChunkCommand {
        int run(CommandContext<CommandSourceStack> context, ChunkPos chunk);
    }

    private static SectorPos argumentSector(CommandContext<CommandSourceStack> context) {
        return new SectorPos(IntegerArgumentType.getInteger(context, "x"), IntegerArgumentType.getInteger(context, "z"));
    }

    private static SectorPos offset(SectorPos origin, int dx, int dz) {
        long x = (long) origin.x() + dx;
        long z = (long) origin.z() + dz;
        if (x < Integer.MIN_VALUE || x > Integer.MAX_VALUE || z < Integer.MIN_VALUE || z > Integer.MAX_VALUE) return null;
        return new SectorPos((int) x, (int) z);
    }
}
