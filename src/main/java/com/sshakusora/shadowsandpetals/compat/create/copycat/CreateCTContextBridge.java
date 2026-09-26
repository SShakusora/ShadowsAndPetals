package com.sshakusora.shadowsandpetals.compat.create.copycat;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.sshakusora.shadowsandpetals.client.ct.CTContext;
import com.sshakusora.shadowsandpetals.client.ct.CTContextBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Reuses Create's connection and blocking algorithm for Create and
 * Copycats+-style material render passes.
 */
public final class CreateCTContextBridge implements CTContextBridge {
    private static final String CREATE_COPYCAT_PACKAGE =
            "com.simibubi.create.content.decoration.copycat.";
    private static final String COPYCATS_PLUS_PACKAGE =
            "com.copycatsplus.copycats.foundation.copycat.";
    private static final String CREATE_FILTERED_READER =
            CREATE_COPYCAT_PACKAGE + "FilteredBlockAndTintGetter";
    private static final String COPYCATS_FILTERED_READER =
            COPYCATS_PLUS_PACKAGE + "model.FilteredBlockAndTintGetter";
    private static final String COPYCATS_SCALED_READER =
            COPYCATS_PLUS_PACKAGE + "model.ScaledBlockAndTintGetter";
    private static final String COPYCATS_BLOCK_INTERFACE =
            COPYCATS_PLUS_PACKAGE + "ICopycatBlock";
    private static final String COPYCATS_ENTITY_INTERFACE =
            COPYCATS_PLUS_PACKAGE + "ICopycatBlockEntity";

    private static final ConnectedTextureBehaviour BEHAVIOUR = new ConnectedTextureBehaviour.Base() {
        @Override
        public @Nullable CTSpriteShiftEntry getShift(
                BlockState state,
                Direction direction,
                @Nullable net.minecraft.client.renderer.texture.TextureAtlasSprite sprite
        ) {
            return null;
        }
    };

    private static final ConnectedTextureBehaviour.ContextRequirement ALL_CONTEXT =
            ConnectedTextureBehaviour.ContextRequirement.builder().all().build();

    private CreateCTContextBridge() {
    }

    public static CTContextBridge create() {
        return new CreateCTContextBridge();
    }

    @Override
    public boolean matches(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (hasNamedType(level.getClass(), CREATE_FILTERED_READER)
                || hasNamedType(level.getClass(), COPYCATS_FILTERED_READER)
                || hasNamedType(level.getClass(), COPYCATS_SCALED_READER)) {
            return true;
        }
        if (isCreateCopycatBlock(level.getBlockState(pos).getBlock())) {
            return true;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && implementsNamedInterface(blockEntity.getClass(), COPYCATS_ENTITY_INTERFACE);
    }

    @Override
    public CTContext buildContext(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face) {
        ConnectedTextureBehaviour.CTContext source =
                BEHAVIOUR.buildContext(level, pos, state, face, ALL_CONTEXT);
        CTContext result = new CTContext();
        result.up = source.up;
        result.down = source.down;
        result.left = source.left;
        result.right = source.right;
        result.topLeft = source.topLeft;
        result.topRight = source.topRight;
        result.bottomLeft = source.bottomLeft;
        result.bottomRight = source.bottomRight;
        return result;
    }

    private static boolean isCreateCopycatBlock(Block block) {
        return block instanceof com.simibubi.create.content.decoration.copycat.CopycatBlock
                || implementsNamedInterface(block.getClass(), COPYCATS_BLOCK_INTERFACE);
    }

    private static boolean implementsNamedInterface(Class<?> type, String interfaceName) {
        for (Class<?> candidate = type; candidate != null; candidate = candidate.getSuperclass()) {
            for (Class<?> implemented : candidate.getInterfaces()) {
                if (implemented.getName().equals(interfaceName)
                        || implementsNamedInterface(implemented, interfaceName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasNamedType(Class<?> type, String typeName) {
        for (Class<?> candidate = type; candidate != null; candidate = candidate.getSuperclass()) {
            if (candidate.getName().equals(typeName)) {
                return true;
            }
        }
        return false;
    }
}
