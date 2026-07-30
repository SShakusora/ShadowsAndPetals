package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.block.nature.ClamDiggingSandBlock;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.registries.ItemRegistry;
import com.sshakusora.shadowsandpetals.world.clam.ClamHarvestData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class ClamDiggingSandBlockEntity extends BlockEntity {
    public static final int BRUSH_COOLDOWN_TICKS = 10;
    public static final int BRUSH_RESET_TICKS = 40;
    public static final int REQUIRED_BRUSHES_TO_COMPLETE = 10;

    private static final int RESET_STEP_TICKS = 4;
    private static final int RESET_STEP_AMOUNT = 2;
    private static final long SUCCESS_COOLDOWN_TICKS = 40_960L;
    private static final long EMPTY_COOLDOWN_TICKS = 2_400L;
    private static final String BRUSH_COUNT_KEY = "brush_count";
    private static final String RESET_AT_KEY = "reset_at";
    private static final String COOLDOWN_END_KEY = "cooldown_end";
    private static final String HIT_DIRECTION_KEY = "hit_direction";
    private static final String ITEM_KEY = "item";
    private static final String RESULT_RESOLVED_KEY = "result_resolved";

    private int brushCount;
    private long brushCountResetsAtTick;
    private long coolDownEndsAtTick;
    private ItemStack item = ItemStack.EMPTY;
    private boolean resultResolved;
    private @Nullable Direction hitDirection;

    public ClamDiggingSandBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.CLAM_DIGGING_SAND.get(), pos, state);
    }

    public void begin(long gameTime) {
        brushCountResetsAtTick = gameTime + BRUSH_RESET_TICKS;
        setChanged();
    }

    public boolean brush(long gameTime, ServerLevel level, Direction direction) {
        if (hitDirection == null) {
            hitDirection = direction;
        }

        brushCountResetsAtTick = gameTime + BRUSH_RESET_TICKS;
        if (gameTime < coolDownEndsAtTick) {
            return false;
        }

        coolDownEndsAtTick = gameTime + BRUSH_COOLDOWN_TICKS;
        resolveResult(level);
        int previousDusted = getDustedState();
        brushCount++;
        setChanged();

        if (brushCount >= REQUIRED_BRUSHES_TO_COMPLETE) {
            complete(level);
            return true;
        }

        int dusted = getDustedState();
        if (previousDusted != dusted) {
            level.setBlock(worldPosition, getBlockState().setValue(ClamDiggingSandBlock.DUSTED, dusted), 3);
        }
        level.scheduleTick(worldPosition, getBlockState().getBlock(), 2);
        return false;
    }

    public void checkReset(ServerLevel level) {
        if (brushCount != 0 && level.getGameTime() >= brushCountResetsAtTick) {
            int previousDusted = getDustedState();
            brushCount = Math.max(0, brushCount - RESET_STEP_AMOUNT);
            int dusted = getDustedState();
            setChanged();

            if (brushCount == 0) {
                level.setBlock(worldPosition, Blocks.SAND.defaultBlockState(), 3);
                return;
            }

            brushCountResetsAtTick = level.getGameTime() + RESET_STEP_TICKS;
            if (previousDusted != dusted) {
                level.setBlock(worldPosition, getBlockState().setValue(ClamDiggingSandBlock.DUSTED, dusted), 3);
            }
        }

        if (brushCount == 0) {
            if (brushCountResetsAtTick == 0L || level.getGameTime() >= brushCountResetsAtTick) {
                level.setBlock(worldPosition, Blocks.SAND.defaultBlockState(), 3);
            } else {
                level.scheduleTick(worldPosition, getBlockState().getBlock(), 2);
            }
        } else {
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 2);
        }
    }

    private void complete(ServerLevel level) {
        boolean foundClam = !item.isEmpty();
        ClamHarvestData.startCooldown(
                level,
                worldPosition,
                foundClam ? SUCCESS_COOLDOWN_TICKS : EMPTY_COOLDOWN_TICKS
        );

        BlockState state = getBlockState();
        level.levelEvent(3008, worldPosition, Block.getId(state));
        level.playSound(null, worldPosition, SoundEvents.BRUSH_SAND_COMPLETED, SoundSource.BLOCKS);
        if (foundClam) {
            dropContent(level);
        }
        level.setBlock(worldPosition, Blocks.SAND.defaultBlockState(), 3);
    }

    private void resolveResult(ServerLevel level) {
        if (resultResolved) {
            return;
        }

        resultResolved = true;
        item = level.getRandom().nextInt(4) == 0
                ? new ItemStack(ItemRegistry.CLAM.get())
                : ItemStack.EMPTY;
        setChanged();
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 3);
    }

    private void dropContent(ServerLevel level) {
        Direction dropDirection = hitDirection == null ? Direction.UP : hitDirection;
        BlockPos dropPos = worldPosition.relative(dropDirection);
        double itemWidth = EntityType.ITEM.getWidth();
        double inset = itemWidth / 2.0;
        double range = 1.0 - itemWidth;
        ItemEntity entity = new ItemEntity(
                level,
                dropPos.getX() + 0.5 * range + inset,
                dropPos.getY() + 0.5 + EntityType.ITEM.getHeight() / 2.0,
                dropPos.getZ() + 0.5 * range + inset,
                item.copy()
        );
        entity.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(entity);
        this.item = ItemStack.EMPTY;
    }

    public @Nullable Direction getHitDirection() {
        return hitDirection;
    }

    public ItemStack getItem() {
        return item;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.storeNullable(HIT_DIRECTION_KEY, Direction.CODEC, hitDirection);
        tag.putBoolean(RESULT_RESOLVED_KEY, resultResolved);
        if (!item.isEmpty()) {
            RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
            tag.store(ITEM_KEY, ItemStack.CODEC, ops, item);
        }
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        brushCount = Math.clamp(input.getIntOr(BRUSH_COUNT_KEY, 0), 0, REQUIRED_BRUSHES_TO_COMPLETE - 1);
        brushCountResetsAtTick = input.getLongOr(RESET_AT_KEY, 0L);
        coolDownEndsAtTick = input.getLongOr(COOLDOWN_END_KEY, 0L);
        item = input.read(ITEM_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        resultResolved = input.getBooleanOr(RESULT_RESOLVED_KEY, false);
        hitDirection = input.read(HIT_DIRECTION_KEY, Direction.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (brushCount != 0) {
            output.putInt(BRUSH_COUNT_KEY, brushCount);
            output.putLong(RESET_AT_KEY, brushCountResetsAtTick);
            output.putLong(COOLDOWN_END_KEY, coolDownEndsAtTick);
        }
        if (!item.isEmpty()) {
            output.store(ITEM_KEY, ItemStack.CODEC, item);
        }
        output.putBoolean(RESULT_RESOLVED_KEY, resultResolved);
        output.storeNullable(HIT_DIRECTION_KEY, Direction.CODEC, hitDirection);
    }

    private int getDustedState() {
        if (brushCount == 0) {
            return 0;
        }
        if (brushCount < 3) {
            return 1;
        }
        return brushCount < 6 ? 2 : 3;
    }
}
