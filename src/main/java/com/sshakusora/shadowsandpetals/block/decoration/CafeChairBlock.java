package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CafeChairBlock extends AbstractSeatBlock {
    public static final MapCodec<CafeChairBlock> CODEC = simpleCodec(CafeChairBlock::new);
    private static final double SEAT_HEIGHT = 0.4375D;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(7.0D, 0.0D, 7.0D, 9.0D, 7.0D, 9.0D),
            Block.box(7.5D, 0.0D, 3.0D, 8.5D, 1.0D, 7.0D),
            Block.box(7.5D, 0.0D, 9.0D, 8.5D, 1.0D, 13.0D),
            Block.box(3.0D, 0.0D, 7.5D, 7.0D, 1.0D, 8.5D),
            Block.box(9.0D, 0.0D, 7.5D, 13.0D, 1.0D, 8.5D),
            Block.box(3.5D, 7.0D, 3.5D, 12.5D, 8.0D, 12.5D),
            Block.box(3.0D, 7.25D, 3.0D, 13.0D, 9.25D, 13.0D),
            Block.box(3.5D, 9.25D, 3.5D, 12.5D, 10.25D, 12.5D)
    );

    public CafeChairBlock(BlockBehaviour.Properties properties) {
        super(properties, SEAT_HEIGHT);
    }

    @Override
    protected MapCodec<CafeChairBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance * 0.5F);
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(level, entity);
            return;
        }
        bounceUp(entity);
    }

    private void bounceUp(Entity entity) {
        Vec3 deltaMovement = entity.getDeltaMovement();
        if (deltaMovement.y < 0.0D) {
            double bounceScale = entity instanceof LivingEntity ? 1.0D : 0.8D;
            entity.setDeltaMovement(deltaMovement.x, -deltaMovement.y * 0.66D * bounceScale, deltaMovement.z);
        }
    }
}
