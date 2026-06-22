package com.sshakusora.shadowsandpetals.registries;

import com.sshakusora.shadowsandpetals.menu.IroriMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

public class MenuRegistry {
    public static final DeferredHolder<MenuType<?>, MenuType<IroriMenu>> IRORI = SAPRegistries.MENUS.register(
            "irori",
            () -> IMenuTypeExtension.create(IroriMenu::new));

    public static void init() {}
}
