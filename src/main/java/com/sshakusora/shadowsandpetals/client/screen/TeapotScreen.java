package com.sshakusora.shadowsandpetals.client.screen;
import com.sshakusora.shadowsandpetals.menu.TeapotMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public class TeapotScreen extends AbstractContainerScreen<TeapotMenu> {
 public TeapotScreen(TeapotMenu menu, Inventory inventory, Component title){super(menu,inventory,title);}
 @Override protected void renderBg(GuiGraphics graphics,float partialTick,int mouseX,int mouseY){}
}