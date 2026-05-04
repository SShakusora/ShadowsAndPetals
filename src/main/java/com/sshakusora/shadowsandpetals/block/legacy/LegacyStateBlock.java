package com.sshakusora.shadowsandpetals.block.legacy;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class LegacyStateBlock extends LegacyBlock {
    private static final ThreadLocal<Definition> CURRENT_DEFINITION = new ThreadLocal<>();

    private LegacyStateBlock(BlockBehaviour.Properties properties, Definition definition) {
        super(properties);
        registerDefaultState(definition.applyDefaults(stateDefinition.any()));
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

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        Definition definition = CURRENT_DEFINITION.get();
        if (definition == null) {
            throw new IllegalStateException("LegacyStateBlock definition is missing during construction");
        }
        definition.addProperties(builder);
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
