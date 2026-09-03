package com.sshakusora.shadowsandpetals.client.model.registry;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/** Client-only registry for decorators applied to baked block-state models. */
public final class BlockStateModelDecoratorRegistry {
    private static final List<Decorator> DECORATORS = new ArrayList<>();

    private BlockStateModelDecoratorRegistry() {
    }

    public static <B extends Block> Builder<B> forBlock(Class<B> blockType) {
        return new Builder<>(blockType);
    }

    public static void applyAll(ModelEvent.ModifyBakingResult event) {
        event.getBakingResult().blockStateModels().replaceAll((state, originalModel) -> {
            Block block = state.getBlock();
            BlockStateModel model = originalModel;
            for (Decorator decorator : DECORATORS) {
                if (decorator.matches(block)) {
                    model = decorator.apply(block, state, model);
                }
            }
            return model;
        });
    }

    public static final class Builder<B extends Block> {
        private final Class<B> blockType;
        private BiFunction<? super B, ? super BlockStateModel, ? extends BlockStateModel> wrapper;
        private StateAwareWrapper<B> stateAwareWrapper;

        private Builder(Class<B> blockType) {
            this.blockType = Objects.requireNonNull(blockType, "blockType");
        }

        public Builder<B> wrap(BiFunction<? super B, ? super BlockStateModel, ? extends BlockStateModel> wrapper) {
            this.wrapper = Objects.requireNonNull(wrapper, "wrapper");
            this.stateAwareWrapper = null;
            return this;
        }

        /**
         * Registers a decorator that also receives the concrete block state being baked.
         * This is needed by models whose breaking-overlay representation must retain
         * state that vanilla does not pass back during overlay rendering.
         */
        public Builder<B> wrapWithState(StateAwareWrapper<B> wrapper) {
            this.stateAwareWrapper = Objects.requireNonNull(wrapper, "wrapper");
            this.wrapper = null;
            return this;
        }

        public void register() {
            if (wrapper == null && stateAwareWrapper == null) {
                throw new IllegalStateException("Block-state model decorator is required for " + blockType.getName());
            }
            DECORATORS.add(new TypedDecorator<>(blockType, wrapper, stateAwareWrapper));
        }
    }

    @FunctionalInterface
    public interface StateAwareWrapper<B extends Block> {
        BlockStateModel apply(B block, BlockState state, BlockStateModel model);
    }

    private interface Decorator {
        boolean matches(Block block);

        BlockStateModel apply(Block block, BlockState state, BlockStateModel model);
    }

    private record TypedDecorator<B extends Block>(
            Class<B> blockType,
            BiFunction<? super B, ? super BlockStateModel, ? extends BlockStateModel> wrapper,
            StateAwareWrapper<B> stateAwareWrapper
    ) implements Decorator {
        @Override
        public boolean matches(Block block) {
            return blockType.isInstance(block);
        }

        @Override
        public BlockStateModel apply(Block block, BlockState state, BlockStateModel model) {
            B typedBlock = blockType.cast(block);
            return stateAwareWrapper != null
                    ? stateAwareWrapper.apply(typedBlock, state, model)
                    : wrapper.apply(typedBlock, model);
        }
    }
}
