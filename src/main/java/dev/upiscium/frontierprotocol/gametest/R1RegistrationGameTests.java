package dev.upiscium.frontierprotocol.gametest;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class R1RegistrationGameTests {
    private static final String[] OLD_CONTENT = {
        "stabilization_beacon", "infection_core", "infection_nest", "resource_node", "oil_well"
    };

    private R1RegistrationGameTests() {}

    @GameTest(template = "empty")
    public static void oldGameplayContentIsNotRegistered(GameTestHelper helper) {
        for (String path : OLD_CONTENT) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(FrontierProtocolMod.MOD_ID, path);
            helper.assertTrue(!BuiltInRegistries.BLOCK.containsKey(id), "old block remains registered: " + id);
            helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(id), "old item remains registered: " + id);
        }
        helper.succeed();
    }
}
