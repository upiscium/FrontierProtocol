package dev.upiscium.frontierprotocol.stabilizer;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public enum StabilizerTier {
    TIER_1("tier_1"),
    TIER_2("tier_2"),
    TIER_3("tier_3");

    private final String serializedName;

    StabilizerTier(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public String registryPrefix() {
        return serializedName;
    }

    public static StabilizerTier fromBlock(Block block) {
        if (block == null) throw new IllegalArgumentException("block must not be null");
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (!id.getNamespace().equals(FrontierProtocolMod.MOD_ID)) {
            throw new IllegalArgumentException("Unknown Stabilizer block: " + id);
        }
        return fromRegistryPath(id.getPath());
    }

    public static StabilizerTier fromRegistryPath(String registryPath) {
        for (StabilizerTier tier : values()) {
            if ((tier.registryPrefix() + "_stabilizer").equals(registryPath)) return tier;
        }
        throw new IllegalArgumentException("Unknown Stabilizer block registry path: " + registryPath);
    }
}
