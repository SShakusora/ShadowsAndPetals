package com.sshakusora.shadowsandpetals.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/**
 * Raw concrete keeps the selected surface pattern as a normal block state so
 * the choice is saved with the chunk and synchronised to clients.
 */
public class RawConcreteBlock extends Block {
    public static final MapCodec<RawConcreteBlock> CODEC = simpleCodec(RawConcreteBlock::new);
    public static final EnumProperty<TextureVariant> TEXTURE =
            EnumProperty.create("texture", TextureVariant.class);

    public RawConcreteBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TEXTURE, TextureVariant.BLANK));
    }

    @Override
    protected MapCodec<RawConcreteBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TEXTURE);
    }

    /**
     * Maps the selected raw-concrete pattern to the connected-texture sheet
     * registered for the block. The position and face are part of the selector
     * contract, but deliberately do not affect this block's pattern.
     */
    public static int selectTextureIndex(@Nullable BlockState state, BlockPos pos, Direction face) {
        if (state == null || !state.hasProperty(TEXTURE)) {
            return TextureVariant.BLANK.textureIndex();
        }
        return textureIndexForVariant(state.getValue(TEXTURE), pos, face);
    }

    public static int textureIndexForVariant(TextureVariant variant, BlockPos pos, Direction face) {
        return variant == null ? TextureVariant.BLANK.textureIndex() : variant.textureIndex();
    }

    public enum TextureVariant implements StringRepresentable {
        BLANK("blank", 0),
        SINGLE_HOLE("single_hole", 1),
        FOUR_HOLE("four_hole", 2);

        private final String serializedName;
        private final int textureIndex;

        TextureVariant(String serializedName, int textureIndex) {
            this.serializedName = serializedName;
            this.textureIndex = textureIndex;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        public int textureIndex() {
            return textureIndex;
        }
    }
}
