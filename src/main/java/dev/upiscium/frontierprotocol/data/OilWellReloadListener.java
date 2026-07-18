package dev.upiscium.frontierprotocol.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.oil.OilWellDefinition;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class OilWellReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile List<OilWellDefinition> definitions = List.of();

    public OilWellReloadListener() {
        super(GSON, "frontier_protocol/oil_wells");
    }

    public static Optional<OilWellDefinition> find(ResourceLocation id) {
        return definitions.stream().filter(definition -> definition.id().equals(id)).findFirst();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager manager, ProfilerFiller profiler) {
        List<OilWellDefinition> loaded = jsons.entrySet().stream()
                .map(entry -> parse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(definition -> definition.id().toString()))
                .toList();
        definitions = List.copyOf(loaded);
        FrontierProtocolMod.LOGGER.info("Loaded {} oil well definitions", loaded.size());
    }

    private static OilWellDefinition parse(ResourceLocation id, JsonElement json) {
        return OilWellDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(message -> new JsonParseException("Invalid oil well " + id + ": " + message))
                .withId(id);
    }
}
