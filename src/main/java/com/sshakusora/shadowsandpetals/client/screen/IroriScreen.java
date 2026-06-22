package com.sshakusora.shadowsandpetals.client.screen;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.menu.IroriMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class IroriScreen extends AbstractContainerScreen<IroriMenu> {
    private static final Identifier TEXTURE = ShadowsAndPetals.asResource("textures/gui/irori.png");
    private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final int FLAME_X = 80;
    private static final int FLAME_Y = 36;

    public IroriScreen(IroriMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        if (this.menu.isLit()) {
            int litProgressHeight = Mth.ceil(this.menu.getLitProgress() * 13.0F) + 1;
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    LIT_PROGRESS_SPRITE,
                    14,
                    14,
                    0,
                    14 - litProgressHeight,
                    xo + FLAME_X,
                    yo + FLAME_Y + 14 - litProgressHeight,
                    14,
                    litProgressHeight
            );
        }
    }
}
