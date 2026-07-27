package com.sshakusora.shadowsandpetals.recipe;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.item.chime.WindChimeColors;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import com.sshakusora.shadowsandpetals.registries.RecipeSerializerRegistry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class WindChimeDyeRecipe extends CustomRecipe {
    public static final MapCodec<WindChimeDyeRecipe> MAP_CODEC = Target.CODEC
            .fieldOf("target")
            .xmap(WindChimeDyeRecipe::new, WindChimeDyeRecipe::target);
    public static final StreamCodec<RegistryFriendlyByteBuf, WindChimeDyeRecipe> STREAM_CODEC = StreamCodec.of(
            (buffer, recipe) -> buffer.writeEnum(recipe.target),
            buffer -> new WindChimeDyeRecipe(buffer.readEnum(Target.class))
    );
    public static final RecipeSerializer<WindChimeDyeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Target target;

    public WindChimeDyeRecipe(Target target) {
        this.target = target;
    }

    public Target target() {
        return target;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return parse(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Layout layout = parse(input);
        if (layout == null) {
            return ItemStack.EMPTY;
        }

        WindChimeColors colors = WindChimeColors.fromStack(layout.windChime());
        ItemStack result = layout.windChime().copyWithCount(1);
        if (layout.ribbonDye() != null) {
            colors = colors.withRibbon(layout.ribbonDye());
        }
        if (layout.vaneDye() != null) {
            colors = colors.withVane(layout.vaneDye());
        }
        colors.applyToStack(result);
        return result;
    }

    @Override
    public RecipeSerializer<WindChimeDyeRecipe> getSerializer() {
        return RecipeSerializerRegistry.WIND_CHIME_DYEING.get();
    }

    @Override
    public List<RecipeDisplay> display() {
        SlotDisplay dye = new SlotDisplay.TagSlotDisplay(Tags.Items.DYES);
        SlotDisplay windChime = new SlotDisplay.ItemSlotDisplay(BlockRegistry.WIND_CHIME.asItem());
        List<SlotDisplay> ingredients = switch (target) {
            case RIBBON -> List.of(dye, windChime);
            case VANE -> List.of(windChime, dye);
            case BOTH -> List.of(dye, windChime, dye);
        };
        List<SlotDisplay> results = displayResults().stream()
                .map(SlotDisplay.ItemStackSlotDisplay::new)
                .map(SlotDisplay.class::cast)
                .toList();
        return List.of(new ShapedCraftingRecipeDisplay(
                1,
                target == Target.BOTH ? 3 : 2,
                ingredients,
                new SlotDisplay.Composite(results),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        ));
    }

    private List<ItemStackTemplate> displayResults() {
        if (target == Target.BOTH) {
            return DyeColor.VALUES.stream()
                    .flatMap(ribbon -> DyeColor.VALUES.stream()
                            .map(vane -> displayResult(ribbon, vane)))
                    .toList();
        }
        return DyeColor.VALUES.stream()
                .map(color -> target == Target.RIBBON
                        ? displayResult(color, WindChimeColors.DEFAULT_COLOR)
                        : displayResult(WindChimeColors.DEFAULT_COLOR, color))
                .toList();
    }

    private ItemStackTemplate displayResult(DyeColor ribbon, DyeColor vane) {
        WindChimeColors colors = new WindChimeColors(ribbon, vane);
        CompoundTag tag = new CompoundTag();
        colors.saveToTag(tag);
        DataComponentPatch components = DataComponentPatch.builder()
                .set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
                .set(DataComponents.ITEM_MODEL, colors.itemModelId())
                .build();
        return new ItemStackTemplate(BlockRegistry.WIND_CHIME.asItem(), components);
    }

    private @Nullable Layout parse(CraftingInput input) {
        if (input.ingredientCount() != (target == Target.BOTH ? 3 : 2)) {
            return null;
        }

        int chimeX = -1;
        int chimeY = -1;
        ItemStack windChime = ItemStack.EMPTY;
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                if (stack.is(BlockRegistry.WIND_CHIME.asItem())) {
                    if (!windChime.isEmpty()) {
                        return null;
                    }
                    windChime = stack;
                    chimeX = x;
                    chimeY = y;
                }
            }
        }
        if (windChime.isEmpty()) {
            return null;
        }

        DyeColor ribbonDye = null;
        DyeColor vaneDye = null;
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                if (stack.isEmpty() || x == chimeX && y == chimeY) {
                    continue;
                }

                DyeColor candidate = stack.get(DataComponents.DYE);
                if (candidate == null) {
                    return null;
                }

                if (x != chimeX) {
                    return null;
                }
                if (y == chimeY - 1 && target.dyesRibbon() && ribbonDye == null) {
                    ribbonDye = candidate;
                } else if (y == chimeY + 1 && target.dyesVane() && vaneDye == null) {
                    vaneDye = candidate;
                } else {
                    return null;
                }
            }
        }

        if (target.dyesRibbon() != (ribbonDye != null) || target.dyesVane() != (vaneDye != null)) {
            return null;
        }
        return new Layout(windChime, ribbonDye, vaneDye);
    }

    public enum Target implements StringRepresentable {
        RIBBON("ribbon", true, false),
        VANE("vane", false, true),
        BOTH("both", true, true);

        public static final StringRepresentable.EnumCodec<Target> CODEC =
                StringRepresentable.fromEnum(Target::values);

        private final String name;
        private final boolean dyesRibbon;
        private final boolean dyesVane;

        Target(String name, boolean dyesRibbon, boolean dyesVane) {
            this.name = name;
            this.dyesRibbon = dyesRibbon;
            this.dyesVane = dyesVane;
        }

        boolean dyesRibbon() {
            return dyesRibbon;
        }

        boolean dyesVane() {
            return dyesVane;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private record Layout(ItemStack windChime, @Nullable DyeColor ribbonDye, @Nullable DyeColor vaneDye) {
    }
}
