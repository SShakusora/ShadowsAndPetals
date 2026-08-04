package com.sshakusora.shadowsandpetals.client.outline;

import com.sshakusora.shadowsandpetals.api.outline.BlockOutlineContext;
import com.sshakusora.shadowsandpetals.api.outline.BlockOutlineProvider;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

public final class BlockOutlineRegistry {
    private static final Map<Block, BlockOutlineProvider> PROVIDERS = new IdentityHashMap<>();

    private BlockOutlineRegistry() {
    }

    public static void register(Block block, BlockOutlineProvider provider) {
        PROVIDERS.put(
                Objects.requireNonNull(block, "block"),
                Objects.requireNonNull(provider, "provider")
        );
    }

    public static void unregister(Block block) {
        PROVIDERS.remove(block);
    }

    @Nullable
    public static OutlineGeometry createGeometry(BlockState state, BlockOutlineContext context) {
        Block block = state.getBlock();
        BlockOutlineProvider provider = PROVIDERS.get(block);
        if (provider == null && block instanceof BlockOutlineProvider blockProvider) {
            provider = blockProvider;
        }
        return provider == null ? null : provider.getOutline(state, context);
    }
}
