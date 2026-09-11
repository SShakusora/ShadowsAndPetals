package com.sshakusora.shadowsandpetals.client.screen;
import com.sshakusora.shadowsandpetals.menu.IroriMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public class IroriScreen extends AbstractContainerScreen<IroriMenu> {
 public IroriScreen(IroriMenu menu, Inventory inventory, Component title){super(menu,inventory,title);}
 @Override protected void renderBg(GuiGraphics graphics,float partialTick,int mouseX,int mouseY){}
}