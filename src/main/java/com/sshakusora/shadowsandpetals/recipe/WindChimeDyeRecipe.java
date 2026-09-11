package com.sshakusora.shadowsandpetals.recipe;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.item.chime.WindChimeColors;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import com.sshakusora.shadowsandpetals.registries.RecipeSerializerRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class WindChimeDyeRecipe extends CustomRecipe {
    public static final MapCodec<WindChimeDyeRecipe> MAP_CODEC = Target.CODEC
            .fieldOf("target")
            .xmap(WindChimeDyeRecipe::new, WindChimeDyeRecipe::target);
    public static final StreamCodec<RegistryFriendlyByteBuf, WindChimeDyeRecipe> STREAM_CODEC = StreamCodec.of(
            (buffer, recipe) -> buffer.writeEnum(recipe.target),
            buffer -> new WindChimeDyeRecipe(buffer.readEnum(Target.class))
    );
    public static final RecipeSerializer<WindChimeDyeRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<WindChimeDyeRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WindChimeDyeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    private final Target target;

    public WindChimeDyeRecipe(Target target) {
        super(CraftingBookCategory.MISC);
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
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
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
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 1 && height >= 1;
    }

    @Override
    public RecipeSerializer<WindChimeDyeRecipe> getSerializer() {
        return RecipeSerializerRegistry.WIND_CHIME_DYEING.get();
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

                DyeColor candidate = DyeColor.getColor(stack);
                if (candidate == null || x != chimeX) {
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
