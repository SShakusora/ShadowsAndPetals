package com.sshakusora.shadowsandpetals.client.ct;

import com.sshakusora.shadowsandpetals.compat.CompatManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Registry for optional connected-texture context bridges. */
public final class CTContextBridges {
    private static final String CREATE_BRIDGE =
            "com.sshakusora.shadowsandpetals.compat.create.copycat.CreateCTContextBridge";

    private static final List<CTContextBridge> BRIDGES = new ArrayList<>();
    private static boolean initialized;

    private CTContextBridges() {
    }

    /**
     * Loads the Create bridge behind the optional dependency boundary. The
     * method is idempotent and is safe to call from model-data collection.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (!CompatManager.isCreateLoaded()) {
            return;
        }

        try {
            Class<?> bridgeClass = Class.forName(CREATE_BRIDGE);
            Object bridge = bridgeClass.getMethod("create").invoke(null);
            if (bridge instanceof CTContextBridge contextBridge) {
                BRIDGES.add(contextBridge);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Create is optional. The local CT implementation remains usable.
        }
    }

    /**
     * Registers an additional bridge. This is intentionally public so a
     * future optional integration can add its own render context without
     * changing the connected-texture model wrapper.
     */
    public static synchronized void register(CTContextBridge bridge) {
        if (!BRIDGES.contains(bridge)) {
            BRIDGES.add(bridge);
        }
    }

    public static @Nullable CTContextBridge find(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        initialize();
        for (CTContextBridge bridge : BRIDGES) {
            if (bridge.matches(level, pos, state)) {
                return bridge;
            }
        }
        return null;
    }
}
