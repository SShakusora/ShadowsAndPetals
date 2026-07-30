package com.sshakusora.shadowsandpetals.client.model;

import com.sshakusora.shadowsandpetals.blockentity.RecessedLampBlockEntity;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

public final class RecessedLampCompositeClientExtensions implements IClientBlockExtensions {
    @Override
    public void collectDynamicTintValues(
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos,
            IntList tintValues
    ) {
        BlockState storedSlab = level.getModelData(pos)
                .get(RecessedLampBlockEntity.STORED_SLAB_MODEL_PROPERTY);
        if (!RecessedLampBlockEntity.isValidStoredSlab(storedSlab)) {
            return;
        }

        for (var tintSource : Minecraft.getInstance().getBlockColors().getTintSources(storedSlab)) {
            tintValues.add(tintSource.colorInWorld(storedSlab, level, pos));
        }
    }
}
