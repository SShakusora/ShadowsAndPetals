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
    private static final int FLAME_X = 62;
    private static final int FLAME_Y = 5;
    private static final int FLAME_U = 176;
    private static final int FLAME_V = 0;
    private static final int FLAME_WIDTH = 54;
    private static final int FLAME_HEIGHT = 45;

    public IroriScreen(IroriMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = -999;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        if (this.menu.isLit()) {
            int litProgressHeight = Mth.ceil(this.menu.getLitProgress() * (FLAME_HEIGHT - 1)) + 1;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    xo + FLAME_X,
                    yo + FLAME_Y + FLAME_HEIGHT - litProgressHeight,
                    (float) FLAME_U,
                    (float) (FLAME_V + FLAME_HEIGHT - litProgressHeight),
                    FLAME_WIDTH,
                    litProgressHeight,
                    256,
                    256
            );
        }
    }
}
