package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.block.decoration.curtain.AbstractCurtainBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side animation clock for the experimental curtain block.
 *
 * <p>The server owns the {@code OPEN} block-state property and writes the
 * world time of the latest toggle into this block entity. The block-entity
 * renderer derives the local animation time from that timestamp so open and
 * close stay in sync after world reloads.</p>
 */
public abstract class AbstractCurtainBlockEntity extends BlockEntity {
    private static final String OPEN_KEY = "Open";
    private static final String TRANSITION_TICK_KEY = "TransitionTick";

    private boolean open = true;
    private long transitionStartTick = Long.MIN_VALUE;

    protected AbstractCurtainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        // The OPEN block-state property is the single source of truth; the
        // constructor sees it before any data packet arrives.
        this.open = blockState.hasProperty(AbstractCurtainBlock.OPEN)
                && blockState.getValue(AbstractCurtainBlock.OPEN);
    }

    /** Records a toggle toward the given open state at the current game time. */
    public void recordTransition(long gameTime, boolean open) {
        this.open = open;
        this.transitionStartTick = gameTime;
        setChanged();
    }

    public boolean isOpen() {
        return open;
    }

    /**
     * Seconds since the current transition started, with partial-tick
     * interpolation. Returns a negative value before the first recorded
     * transition.
     */
    public float transitionTimeSeconds(long gameTime, float partialTick) {
        if (transitionStartTick == Long.MIN_VALUE) {
            return -1.0F;
        }
        return Math.max(0.0F, gameTime - transitionStartTick + partialTick) / 20.0F;
    }

    @Override
    protected void saveAdditional(CompoundTag output, HolderLookup.Provider registries) {
        super.saveAdditional(output, registries);
        output.putBoolean(OPEN_KEY, open);
        output.putLong(TRANSITION_TICK_KEY, transitionStartTick);
    }

    @Override
    protected void loadAdditional(CompoundTag input, HolderLookup.Provider registries) {
        super.loadAdditional(input, registries);
        open = input.getBoolean(OPEN_KEY);
        transitionStartTick = input.contains(TRANSITION_TICK_KEY)
                ? input.getLong(TRANSITION_TICK_KEY)
                : Long.MIN_VALUE;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
