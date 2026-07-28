package dev.upiscium.frontierprotocol.client.ponder;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class FrontierProtocolPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return FrontierProtocolMod.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        FrontierProtocolPonderScenes.register(helper);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        FrontierProtocolPonderTags.register(helper);
    }
}
