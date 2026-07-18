package dev.upiscium.frontierprotocol.gametest;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.world.FrontierProtocolWorldData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FrontierProtocolMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SectorGameTests {
    private SectorGameTests() {}

    @GameTest(template = "empty")
    public static void worldDataStoresOriginAndSectorSize(GameTestHelper helper) {
        FrontierProtocolWorldData data = FrontierProtocolWorldData.get(helper.getLevel().getServer().overworld());
        helper.assertTrue(data.isInitialized(), "world data was not initialized");
        helper.assertTrue(data.sectorSizeChunks() > 0, "sector size was not stored");
        helper.succeed();
    }
}
