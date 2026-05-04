package com.sshakusora.shadowsandpetals.block.decoration;

import com.mojang.serialization.MapCodec;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
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

public class CafeChairBlock extends AbstractSeatBlock {
    public static final MapCodec<CafeChairBlock> CODEC = simpleCodec(CafeChairBlock::new);
    public static final String DYE_HINT_PREFIX_KEY = "message.shadowsandpetals.cafe_chair.dye_hint_prefix";
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
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof DyeItem dyeItem) {
            BlockState dyedState = getDyedState(state, dyeItem.getDyeColor());
            if (dyedState.getBlock() != state.getBlock()) {
                if (!level.isClientSide) {
                    level.setBlock(pos, dyedState, Block.UPDATE_ALL);
                    level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 0.45F, 0.95F);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    public static boolean canApplyDye(BlockState state, DyeColor dyeColor) {
        return getDyedState(state, dyeColor).getBlock() != state.getBlock();
    }

    public static BlockState getDyedState(BlockState state, DyeColor dyeColor) {
        return BlockRegistry.CAFE_CHAIRS.get(dyeColor).get().defaultBlockState()
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
    }

    public static Component createDyeHintMessage(BlockState state, DyeColor dyeColor) {
        Component colorName = Component.translatable("color.minecraft." + dyeColor.getName())
                .withStyle(style -> style.withColor(dyeColor.getTextColor()));
        return Component.translatable(DYE_HINT_PREFIX_KEY, state.getBlock().getName()).append(colorName);
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
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updatedState = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        return direction == Direction.DOWN && !updatedState.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : updatedState;
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
