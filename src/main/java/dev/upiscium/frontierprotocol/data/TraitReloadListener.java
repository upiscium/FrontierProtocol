package dev.upiscium.frontierprotocol.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.sector.SectorTraitDefinition;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class TraitReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile List<SectorTraitDefinition> definitions = List.of();
    private static volatile long revision;

    public TraitReloadListener() {
        super(GSON, "frontier_protocol/sector_traits");
    }

    public static List<SectorTraitDefinition> definitions() {
        return definitions;
    }

    public static long revision() {
        return revision;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<SectorTraitDefinition> loaded = jsons.entrySet().stream()
                .map(entry -> parse(entry.getKey(), entry.getValue()))
                .toList();
        definitions = List.copyOf(loaded);
        revision++;
        FrontierProtocolMod.LOGGER.info("Loaded {} sector trait definitions", loaded.size());
    }

    private static SectorTraitDefinition parse(ResourceLocation id, JsonElement json) {
        return SectorTraitDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(message -> new JsonParseException("Invalid sector trait " + id + ": " + message))
                .withId(id);
    }
}
