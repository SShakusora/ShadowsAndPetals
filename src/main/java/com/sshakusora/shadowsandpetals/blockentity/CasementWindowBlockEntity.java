package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.block.decoration.window.CasementWindowBlock;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class CasementWindowBlockEntity extends BlockEntity {
    private static final String OPEN_KEY = "Open";
    private static final String TRANSITION_TICK_KEY = "TransitionTick";

    private boolean targetOpen;
    private long transitionStartTick = Long.MIN_VALUE;

    public CasementWindowBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.CASEMENT_WINDOW.get(), pos, blockState);
        targetOpen = blockState.hasProperty(CasementWindowBlock.OPEN)
                && blockState.getValue(CasementWindowBlock.OPEN);
    }

    public void recordTransition(long gameTime, boolean open) {
        targetOpen = open;
        transitionStartTick = gameTime;
        setChanged();
    }

    public boolean targetOpen() {
        return targetOpen;
    }

    public float transitionProgress(long gameTime, float partialTick) {
        if (transitionStartTick == Long.MIN_VALUE) {
            return -1.0F;
        }
        return Math.max(0.0F, gameTime - transitionStartTick + partialTick)
                / CasementWindowBlock.ANIMATION_TICKS;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean(OPEN_KEY, targetOpen);
        output.putLong(TRANSITION_TICK_KEY, transitionStartTick);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        targetOpen = input.getBooleanOr(OPEN_KEY, false);
        transitionStartTick = input.getLong(TRANSITION_TICK_KEY).orElse(Long.MIN_VALUE);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
