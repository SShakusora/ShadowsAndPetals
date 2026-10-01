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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Shared client-side transition clock for every curtain block-entity type. */
public abstract class AbstractCurtainBlockEntity extends BlockEntity {
    private static final String OPEN_KEY = "Open";
    private static final String TRANSITION_TICK_KEY = "TransitionTick";

    private boolean open = true;
    private long transitionStartTick = Long.MIN_VALUE;

    protected AbstractCurtainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.open = blockState.hasProperty(AbstractCurtainBlock.OPEN)
                && blockState.getValue(AbstractCurtainBlock.OPEN);
    }

    public void recordTransition(long gameTime, boolean open) {
        this.open = open;
        this.transitionStartTick = gameTime;
        setChanged();
    }

    public boolean isOpen() {
        return open;
    }

    public float transitionTimeSeconds(long gameTime, float partialTick) {
        if (transitionStartTick == Long.MIN_VALUE) {
            return -1.0F;
        }
        return Math.max(0.0F, gameTime - transitionStartTick + partialTick) / 20.0F;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean(OPEN_KEY, open);
        output.putLong(TRANSITION_TICK_KEY, transitionStartTick);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        open = input.getBooleanOr(OPEN_KEY, false);
        transitionStartTick = input.getLong(TRANSITION_TICK_KEY).orElse(Long.MIN_VALUE);
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
