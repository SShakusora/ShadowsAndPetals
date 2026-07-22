package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.menu.IroriMenu;
import com.sshakusora.shadowsandpetals.menu.TeapotMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

public class MenuRegistry {
    public static final DeferredHolder<MenuType<?>, MenuType<IroriMenu>> IRORI = SAPRegistries.MENUS.register(
            "irori",
            () -> IMenuTypeExtension.create(IroriMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<TeapotMenu>> TEAPOT = SAPRegistries.MENUS.register(
            "teapot",
            () -> IMenuTypeExtension.create(TeapotMenu::new));

    public static void init() {}
}
