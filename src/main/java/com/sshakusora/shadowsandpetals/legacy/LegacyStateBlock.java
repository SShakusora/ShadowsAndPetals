package com.sshakusora.shadowsandpetals.legacy;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Temporary registry block used while a legacy state is being converted.  It deliberately reports
 * itself as a block-entity host: old block-entity NBT is deserialized before ChunkEvent.Load, and
 * Minecraft otherwise discards it because a plain Block does not allow a block entity.  The real
 * entity is installed by {@link BlockEntityAliasRegistry} after state migration; this temporary
 * block never creates a new entity when placed.
 */
public class LegacyStateBlock extends BaseEntityBlock {
    private static final ThreadLocal<Definition> CURRENT_DEFINITION = new ThreadLocal<>();
    // The temporary block is never data-driven; this codec only satisfies BaseEntityBlock's block
    // codec contract.  Registry construction still uses the definition-aware factory below.
    public static final MapCodec<LegacyStateBlock> CODEC = simpleCodec(properties ->
            new LegacyStateBlock(properties, new Definition(List.of())));

    private LegacyStateBlock(BlockBehaviour.Properties properties, Definition definition) {
        super(properties);
        registerDefaultState(definition.applyDefaults(stateDefinition.any()));
    }

    @Override
    protected MapCodec<? extends LegacyStateBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // State aliases are rewritten while a chunk is being deserialized.  Calling the normal
        // BlockEntity cleanup here asks Level for the same chunk and can deadlock the chunk-load
        // worker.  BlockEntityAliasRegistry owns the old entity's removal after state conversion.
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Function<BlockBehaviour.Properties, LegacyStateBlock> factory(Definition definition) {
        return properties -> {
            CURRENT_DEFINITION.set(definition);
            try {
                return new LegacyStateBlock(properties, definition);
            } finally {
                CURRENT_DEFINITION.remove();
            }
        };
    }

    private static Definition currentDefinition() {
        Definition definition = CURRENT_DEFINITION.get();
        // The registry factory installs the real definition in the thread-local before
        // constructing a compatibility block.  Codec construction can happen later, outside
        // that factory, so use an empty definition rather than failing during registry reload.
        return definition == null ? new Definition(List.of()) : definition;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        currentDefinition().addProperties(builder);
    }

    public static final class Builder {
        private final List<Entry<?>> entries = new ArrayList<>();

        public <T extends Comparable<T>> Builder property(Property<T> property, T defaultValue) {
            entries.add(new Entry<>(property, defaultValue));
            return this;
        }

        public Definition build() {
            return new Definition(List.copyOf(entries));
        }
    }

    public static final class Definition {
        private final List<Entry<?>> entries;

        private Definition(List<Entry<?>> entries) {
            this.entries = entries;
        }

        private void addProperties(StateDefinition.Builder<Block, BlockState> builder) {
            for (Entry<?> entry : entries) {
                entry.addTo(builder);
            }
        }

        private BlockState applyDefaults(BlockState state) {
            BlockState result = state;
            for (Entry<?> entry : entries) {
                result = entry.applyTo(result);
            }
            return result;
        }
    }

    private record Entry<T extends Comparable<T>>(Property<T> property, T defaultValue) {
        private void addTo(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(property);
        }

        private BlockState applyTo(BlockState state) {
            return state.setValue(property, defaultValue);
        }
    }
}
