package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The large curtain's clock block entity: identical to the small
 * {@link CurtainBlockEntity} except that it binds the
 * {@code shadowsandpetals:large_curtain} block-entity type, whose
 * valid-blocks check would otherwise reject the placed block state.
 */
public class LargeCurtainBlockEntity extends CurtainBlockEntity {
    public LargeCurtainBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.LARGE_CURTAIN.get(), pos, blockState);
    }
}