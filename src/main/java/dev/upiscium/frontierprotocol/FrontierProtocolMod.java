package dev.upiscium.frontierprotocol;

import com.mojang.logging.LogUtils;
import dev.upiscium.frontierprotocol.config.FrontierProtocolClientConfig;
import dev.upiscium.frontierprotocol.config.FrontierProtocolServerConfig;
import dev.upiscium.frontierprotocol.network.NetworkRegistration;
import dev.upiscium.frontierprotocol.registry.ModBlocks;
import dev.upiscium.frontierprotocol.registry.ModBlockEntities;
import dev.upiscium.frontierprotocol.registry.ModDataMaps;
import dev.upiscium.frontierprotocol.registry.ModAttachments;
import dev.upiscium.frontierprotocol.registry.ModItems;
import dev.upiscium.frontierprotocol.registry.ModMenus;
import org.slf4j.Logger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(FrontierProtocolMod.MOD_ID)
public final class FrontierProtocolMod {
    public static final String MOD_ID = "frontier_protocol";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FrontierProtocolMod(IEventBus modBus, ModContainer container) {
        modBus.addListener(NetworkRegistration::register);
        modBus.addListener(ModDataMaps::register);
        modBus.addListener(ModBlockEntities::registerCapabilities);
        ModBlocks.register(modBus);
        ModAttachments.register(modBus);
        ModBlockEntities.register(modBus);
        ModItems.register(modBus);
        ModMenus.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, FrontierProtocolServerConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, FrontierProtocolClientConfig.SPEC);
    }
}
