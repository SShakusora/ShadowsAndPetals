package com.sshakusora.shadowsandpetals.client.screen;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.menu.TeapotMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

public class TeapotScreen extends AbstractContainerScreen<TeapotMenu> {
    private static final Identifier BASIC_TEXTURE =
            ShadowsAndPetals.asResource("textures/gui/teapot/basic.png");
    private static final Identifier WATER_TEXTURE =
            ShadowsAndPetals.asResource("textures/gui/teapot/water.png");
    private static final Identifier TEA_TEXTURE =
            ShadowsAndPetals.asResource("textures/gui/teapot/tea.png");

    private static final int TEXTURE_SIZE = 256;
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 182;

    public TeapotScreen(TeapotMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = -999;
        this.inventoryLabelY = -999;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BASIC_TEXTURE,
                this.leftPos,
                this.topPos,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );

        Identifier fluidTexture = getFluidTexture();
        if (fluidTexture != null) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    fluidTexture,
                    this.leftPos,
                    this.topPos,
                    0.0F,
                    0.0F,
                    this.imageWidth,
                    this.imageHeight,
                    TEXTURE_SIZE,
                    TEXTURE_SIZE
            );
        }
    }

    private @Nullable Identifier getFluidTexture() {
        if (!this.menu.hasFluid()) {
            return null;
        }
        return this.menu.hasWater() ? WATER_TEXTURE : TEA_TEXTURE;
    }
}
