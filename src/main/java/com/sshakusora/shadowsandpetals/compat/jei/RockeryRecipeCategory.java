package com.sshakusora.shadowsandpetals.compat.jei;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.client.tooltip.ClientRockeryTooltip.RockeryPreviewText;
import com.sshakusora.shadowsandpetals.client.tooltip.RockeryPreviewState;
import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import com.sshakusora.shadowsandpetals.item.hammer.HammerItem;
import com.sshakusora.shadowsandpetals.registries.ItemRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

public class RockeryRecipeCategory implements IRecipeCategory<RockeryInfoRecipe> {

    public static final IRecipeType<RockeryInfoRecipe> TYPE =
            IRecipeType.create(ShadowsAndPetals.asResource("rockery_carving"), RockeryInfoRecipe.class);

    private static final int WIDTH = 210;
    private static final int HEIGHT = 96;
    private static final int PIP_SIZE = 64;
    private static final int INPUT_PIP_X = 2;
    private static final int OUTPUT_PIP_X = WIDTH - PIP_SIZE - 2;
    private static final int PIP_Y = (HEIGHT - PIP_SIZE) / 2;
    private static final int CENTER_X = WIDTH / 2;
    private static final int TOOL_Y = 11;
    private static final int ARROW_Y = 35;
    private static final int BOOKMARK_OUTPUT_SLOT_X = -1000;

    private final IDrawable icon;

    public RockeryRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ItemRegistry.HAMMER.get()));
    }

    @Override
    public IRecipeType<RockeryInfoRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(BuiltinLanguageKeys.JEI_ROCKERY_CARVING_TITLE.key());
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RockeryInfoRecipe recipe, IFocusGroup focuses) {
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).add(recipe.inputStack());
        builder.addSlot(RecipeIngredientRole.OUTPUT, BOOKMARK_OUTPUT_SLOT_X, 0)
                .add(recipe.outputStack());
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, CENTER_X - 17, TOOL_Y)
                .setStandardSlotBackground()
                .add(new ItemStack(ItemRegistry.HAMMER.get()));
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, CENTER_X + 1, TOOL_Y)
                .setStandardSlotBackground()
                .add(new ItemStack(ItemRegistry.CHISEL.get()));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RockeryInfoRecipe recipe, IFocusGroup focuses) {
        RockeryPreviewWidget previewWidget = new RockeryPreviewWidget(recipe);
        builder.addWidget(previewWidget);
        builder.addGuiEventListener(previewWidget);
    }

    private static final class RockeryPreviewWidget implements IRecipeWidget, IJeiGuiEventListener {
        private static final float DRAG_SENSITIVITY = 1.5F;

        private final RockeryInfoRecipe recipe;
        private float yawDegrees;

        private RockeryPreviewWidget(RockeryInfoRecipe recipe) {
            this.recipe = recipe;
        }

        @Override
        public ScreenPosition getPosition() {
            return new ScreenPosition(0, 0);
        }

        @Override
        public ScreenRectangle getArea() {
            return new ScreenRectangle(0, 0, WIDTH, HEIGHT);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return button == 0 && isOverPreview(mouseX, mouseY);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (button != 0) {
                return false;
            }
            yawDegrees = (yawDegrees + (float) dragX * DRAG_SENSITIVITY) % 360.0F;
            return true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return button == 0;
        }

        @Override
        public void drawWidget(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
            var scissor = guiGraphics.peekScissorStack();
            int selectedPart = findHoveredStone(mouseX, mouseY);

            guiGraphics.submitPictureInPictureRenderState(
                    new RockeryPreviewState(
                            recipe.block(),
                            recipe.dimensions(),
                            RockeryPreviewState.Content.STONE_STRUCTURE,
                            yawDegrees, false, selectedPart,
                            INPUT_PIP_X, PIP_Y,
                            PIP_SIZE, PIP_SIZE,
                            guiGraphics.pose(),
                            scissor
                    )
            );

            guiGraphics.submitPictureInPictureRenderState(
                    new RockeryPreviewState(
                            recipe.block(),
                            recipe.dimensions(),
                            RockeryPreviewState.Content.ROCKERY,
                            yawDegrees, false, -1,
                            OUTPUT_PIP_X, PIP_Y,
                            PIP_SIZE, PIP_SIZE,
                            guiGraphics.pose(),
                            scissor
                    )
            );

            var font = Minecraft.getInstance().font;
            int arrowStart = INPUT_PIP_X + PIP_SIZE + 4;
            int arrowEnd = OUTPUT_PIP_X - 4;
            int arrowColor = 0xFF_AAAAAA;
            guiGraphics.fill(arrowStart, ARROW_Y - 1, arrowEnd - 5, ARROW_Y + 1, arrowColor);
            guiGraphics.fill(arrowEnd - 6, ARROW_Y - 5, arrowEnd - 4, ARROW_Y + 6, arrowColor);
            guiGraphics.fill(arrowEnd - 4, ARROW_Y - 3, arrowEnd - 2, ARROW_Y + 4, arrowColor);
            guiGraphics.fill(arrowEnd - 2, ARROW_Y - 1, arrowEnd, ARROW_Y + 2, arrowColor);

            int durationTicks = HammerItem.getEffectiveUseDuration(recipe.dimensions());
            String seconds = String.format(Locale.ROOT, "%.2f", durationTicks / 20.0F);
            Component timeLabel = Component.translatable(
                    BuiltinLanguageKeys.JEI_ROCKERY_HAMMERING_TIME.key(), seconds);
            int labelX = CENTER_X - font.width(timeLabel) / 2;
            guiGraphics.text(font, timeLabel, labelX, ARROW_Y + 10, 0xFF_777777);

            Component dimensions = RockeryPreviewText.dimensionLabel(recipe.dimensions());
            int dimensionsX = CENTER_X - font.width(dimensions) / 2;
            guiGraphics.text(font, dimensions, dimensionsX, HEIGHT - font.lineHeight - 2, 0xFFFFFFFF);
        }

        private int findHoveredStone(double mouseX, double mouseY) {
            if (!isOverInputPreview(mouseX, mouseY)) {
                return -1;
            }

            var dimensions = recipe.dimensions();
            float scale = RockeryPreviewState.scaleFor(dimensions, PIP_SIZE, PIP_SIZE);
            Matrix4f rotation = new Matrix4f()
                    .rotateX((float) Math.toRadians(-30.0))
                    .rotateY((float) Math.toRadians(-45.0 + yawDegrees));
            float centerX = INPUT_PIP_X + PIP_SIZE / 2.0F;
            float centerY = PIP_Y + PIP_SIZE / 2.0F;
            float radius = Math.max(7.0F, scale * 0.72F);
            float bestDistance = radius * radius;
            int bestPart = -1;

            for (int part = 0; part < dimensions.partCount(); part++) {
                var local = dimensions.localPos(part);
                Vector3f projected = rotation.transformPosition(new Vector3f(
                        local.getX() + 0.5F - dimensions.width() / 2.0F,
                        local.getY() + 0.5F - dimensions.height() / 2.0F,
                        local.getZ() + 0.5F - dimensions.depth() / 2.0F
                ));
                float dx = (float) mouseX - (centerX - projected.x * scale);
                float dy = (float) mouseY - (centerY - projected.y * scale);
                float distance = dx * dx + dy * dy;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestPart = part;
                }
            }
            return bestPart;
        }

        private static boolean isOverPreview(double mouseX, double mouseY) {
            boolean withinY = mouseY >= PIP_Y && mouseY < PIP_Y + PIP_SIZE;
            boolean overInput = mouseX >= INPUT_PIP_X && mouseX < INPUT_PIP_X + PIP_SIZE;
            boolean overOutput = mouseX >= OUTPUT_PIP_X && mouseX < OUTPUT_PIP_X + PIP_SIZE;
            return withinY && (overInput || overOutput);
        }

        private static boolean isOverInputPreview(double mouseX, double mouseY) {
            return mouseY >= PIP_Y && mouseY < PIP_Y + PIP_SIZE
                    && mouseX >= INPUT_PIP_X && mouseX < INPUT_PIP_X + PIP_SIZE;
        }
    }

    @Override
    public @Nullable Identifier getIdentifier(RockeryInfoRecipe recipe) {
        return ShadowsAndPetals.asResource(
                "rockery_" + recipe.dimensions().width()
                + "_" + recipe.dimensions().height()
                + "_" + recipe.dimensions().depth()
        );
    }
}
