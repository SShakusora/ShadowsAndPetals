package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Block entity for bonsai pots. Stores the resolved trunk/leaf block IDs,
 * the current shape variant, and the dead-tree flag. No ticking is needed;
 * the entity exists solely to drive dynamic client-side rendering.
 */
public final class BonsaiBlockEntity extends BlockEntity {
    private static final String TRUNK_KEY = "TrunkBlock";
    private static final String LEAVES_KEY = "LeavesBlock";
    private static final String SHAPE_KEY = "Shape";
    private static final String DEAD_KEY = "Dead";
    private static final String PLANTED_KEY = "Planted";
    private static final String PLANTED_ITEM_KEY = "PlantedItem";

    /**
     * Bonsai shape variants that the player can cycle through.
     * Order matters: semi_cascade is the initial planting shape.
     */
    public enum Shape {
        SEMI_CASCADE("semi_cascade"),
        SLANTING("slanting"),
        TWIN("twin"),
        WINDSWEPT("windswept");

        private final String serializedName;

        Shape(String serializedName) {
            this.serializedName = serializedName;
        }

        public String getSerializedName() {
            return serializedName;
        }

        public Shape next() {
            Shape[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public static Shape fromName(String name) {
            for (Shape shape : values()) {
                if (shape.serializedName.equals(name)) {
                    return shape;
                }
            }
            return SEMI_CASCADE;
        }
    }

    private boolean planted = false;
    private boolean dead = false;
    private @Nullable Identifier trunkBlockId;
    private @Nullable Identifier leavesBlockId;
    private Shape shape = Shape.SEMI_CASCADE;
    /** The item that was planted — stored for drop recovery. */
    private @Nullable Identifier plantedItemId;

    public BonsaiBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.BONSAI.get(), pos, blockState);
    }

    public boolean isPlanted() {
        return planted;
    }

    public boolean isDead() {
        return dead;
    }

    public Shape getShape() {
        return shape;
    }

    public @Nullable Identifier getTrunkBlockId() {
        return trunkBlockId;
    }

    public @Nullable Identifier getLeavesBlockId() {
        return leavesBlockId;
    }

    public @Nullable Identifier getPlantedItemId() {
        return plantedItemId;
    }

    /**
     * Plants a sapling (or dead bush) into this bonsai pot.
     *
     * @param trunkBlock  the resolved trunk block
     * @param leavesBlock the resolved leaves block
     * @param plantedItem the item that was planted (for drop recovery)
     * @param dead        whether this is a dead-tree planting
     */
    public void plant(Block trunkBlock, Block leavesBlock, Identifier plantedItem, boolean dead) {
        this.planted = true;
        this.dead = dead;
        this.trunkBlockId = BuiltInRegistries.BLOCK.getKey(trunkBlock);
        this.leavesBlockId = BuiltInRegistries.BLOCK.getKey(leavesBlock);
        this.plantedItemId = plantedItem;
        this.shape = Shape.SEMI_CASCADE;
        setChangedAndSync();
    }

    /** Cycles to the next bonsai shape. Only valid when planted. */
    public void cycleShape() {
        if (!planted) {
            return;
        }
        this.shape = this.shape.next();
        setChangedAndSync();
    }

    /** Turns a living bonsai into a dead one (scissors on leaves). */
    public void makeDead() {
        if (!planted || dead) {
            return;
        }
        this.dead = true;
        setChangedAndSync();
    }

    /** Clears the bonsai back to an empty pot (scissors on dead tree). */
    public void clear() {
        this.planted = false;
        this.dead = false;
        this.trunkBlockId = null;
        this.leavesBlockId = null;
        this.plantedItemId = null;
        this.shape = Shape.SEMI_CASCADE;
        setChangedAndSync();
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, 3);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.planted = input.getBooleanOr(PLANTED_KEY, false);
        this.dead = input.getBooleanOr(DEAD_KEY, false);
        this.shape = Shape.fromName(input.getStringOr(SHAPE_KEY, Shape.SEMI_CASCADE.getSerializedName()));
        this.trunkBlockId = input.getString(TRUNK_KEY).map(Identifier::tryParse).orElse(null);
        this.leavesBlockId = input.getString(LEAVES_KEY).map(Identifier::tryParse).orElse(null);
        this.plantedItemId = input.getString(PLANTED_ITEM_KEY).map(Identifier::tryParse).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean(PLANTED_KEY, planted);
        output.putBoolean(DEAD_KEY, dead);
        output.putString(SHAPE_KEY, shape.getSerializedName());
        if (trunkBlockId != null) {
            output.putString(TRUNK_KEY, trunkBlockId.toString());
        }
        if (leavesBlockId != null) {
            output.putString(LEAVES_KEY, leavesBlockId.toString());
        }
        if (plantedItemId != null) {
            output.putString(PLANTED_ITEM_KEY, plantedItemId.toString());
        }
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