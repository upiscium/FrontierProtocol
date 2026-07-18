package dev.upiscium.frontierprotocol.registry;

import dev.upiscium.frontierprotocol.FrontierProtocolMod;
import dev.upiscium.frontierprotocol.resource.ResourceNodeMenu;
import dev.upiscium.frontierprotocol.oil.OilWellMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, FrontierProtocolMod.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<ResourceNodeMenu>> RESOURCE_NODE = MENUS.register(
            "resource_node", () -> IMenuTypeExtension.create(ResourceNodeMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<OilWellMenu>> OIL_WELL = MENUS.register(
            "oil_well", () -> IMenuTypeExtension.create(OilWellMenu::new));

    private ModMenus() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
