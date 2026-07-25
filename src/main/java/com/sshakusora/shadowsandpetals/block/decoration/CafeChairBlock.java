package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import com.sshakusora.shadowsandpetals.util.WoolUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

public class CafeChairBlock extends AbstractSeatBlock {
    public static final MapCodec<CafeChairBlock> CODEC = simpleCodec(CafeChairBlock::new);
    private static final double SEAT_HEIGHT = 0.625D;
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
    @Nullable
    private static Map<Block, DyeColor> colorByBlock;

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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof DyeItem) {
            DyeColor dyeColor = stack.get(DataComponents.DYE);
            if (dyeColor == null) {
                return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
            }

            BlockState dyedState = getDyedState(state, dyeColor);
            if (dyedState.getBlock() != state.getBlock()) {
                if (!level.isClientSide()) {
                    level.setBlock(pos, dyedState, Block.UPDATE_ALL);
                    level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 0.45F, 0.95F);
                    HumanoidArm brushArm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
                    spawnDyeParticles((ServerLevel) level, hitResult, player.getViewVector(0.0F), brushArm, getColor(state), dyeColor);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    public static BlockState getDyedState(BlockState state, DyeColor dyeColor) {
        return BlockRegistry.CAFE_CHAIRS.get(dyeColor).get().defaultBlockState()
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
    }

    private static Map<Block, DyeColor> getColorMap() {
        if (colorByBlock == null) {
            colorByBlock = new IdentityHashMap<>();
            for (DyeColor dyeColor : DyeColor.values()) {
                colorByBlock.put(BlockRegistry.CAFE_CHAIRS.get(dyeColor).get(), dyeColor);
            }
        }
        return colorByBlock;
    }

    public static DyeColor getColor(BlockState state) {
        return getColorMap().getOrDefault(state.getBlock(), DyeColor.WHITE);
    }

    private static void spawnDyeParticles(ServerLevel level, BlockHitResult hitResult, Vec3 viewVector, HumanoidArm brushArm, DyeColor sourceColor, DyeColor targetColor) {
        BlockParticleOption sourceParticle = new BlockParticleOption(ParticleTypes.BLOCK, WoolUtils.getWool(sourceColor).defaultBlockState());
        BlockParticleOption targetParticle = new BlockParticleOption(ParticleTypes.BLOCK, WoolUtils.getWool(targetColor).defaultBlockState());
        int armDirection = brushArm == HumanoidArm.RIGHT ? 1 : -1;
        DustParticlesDelta delta = DustParticlesDelta.fromDirection(viewVector, hitResult.getDirection());
        spawnDyeParticleBurst(level, hitResult, targetParticle, 7, delta, armDirection, 1.0D);
        spawnDyeParticleBurst(level, hitResult, sourceParticle, 1, delta, armDirection, -1.0D);
    }

    private static void spawnDyeParticleBurst(
            ServerLevel level,
            BlockHitResult hitResult,
            BlockParticleOption particle,
            int count,
            DustParticlesDelta delta,
            int armDirection,
            double sweepDirection
    ) {
        Vec3 location = hitResult.getLocation();
        Direction direction = hitResult.getDirection();
        for (int i = 0; i < count; i++) {
            RandomSource random = level.getRandom();
            double x = location.x + (random.nextDouble() - 0.5D) * 0.28D + surfaceOffset(direction.getStepX());
            double y = location.y + (random.nextDouble() - 0.5D) * 0.012D + surfaceOffset(direction.getStepY());
            double z = location.z + (random.nextDouble() - 0.5D) * 0.28D + surfaceOffset(direction.getStepZ());
            double sweepSpeed = (0.02D + random.nextDouble() * 0.025D) * armDirection * sweepDirection;
            double speedX = delta.xd() * sweepSpeed + (random.nextDouble() - 0.5D) * 0.006D;
            double speedY = delta.yd() * sweepSpeed + random.nextDouble() * 0.003D;
            double speedZ = delta.zd() * sweepSpeed + (random.nextDouble() - 0.5D) * 0.006D;
            level.sendParticles(particle, x, y, z, 1, speedX, speedY, speedZ, 0.0D);
        }
    }

    private static double surfaceOffset(int axisStep) {
        return axisStep == 0 ? 0.0D : axisStep * 1.0E-4D;
    }

    private record DustParticlesDelta(double xd, double yd, double zd) {
        public static DustParticlesDelta fromDirection(Vec3 viewVector, Direction direction) {
            return switch (direction) {
                case DOWN, UP -> new DustParticlesDelta(viewVector.z(), 0.0D, -viewVector.x());
                case NORTH -> new DustParticlesDelta(1.0D, 0.0D, -0.1D);
                case SOUTH -> new DustParticlesDelta(-1.0D, 0.0D, 0.1D);
                case WEST -> new DustParticlesDelta(-0.1D, 0.0D, -1.0D);
                case EAST -> new DustParticlesDelta(0.1D, 0.0D, 1.0D);
            };
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state != null && state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        return belowState.isFaceSturdy(level, belowPos, Direction.UP, SupportType.FULL);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random
    ) {
        BlockState updatedState = super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
        return direction == Direction.DOWN && !updatedState.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : updatedState;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance * 0.5F);
    }

    @Override
    public void updateEntityMovementAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityMovementAfterFallOn(level, entity);
            return;
        }
        Vec3 deltaMovement = entity.getDeltaMovement();
        if (deltaMovement.y < 0.0D) {
            double bounceScale = entity instanceof LivingEntity ? 1.0D : 0.8D;
            entity.setDeltaMovement(deltaMovement.x, -deltaMovement.y * 0.66D * bounceScale, deltaMovement.z);
        }
    }
}
