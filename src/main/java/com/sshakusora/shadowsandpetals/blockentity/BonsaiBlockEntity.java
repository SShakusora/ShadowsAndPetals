package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStackResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Block entity for bonsai pots. Stores the resolved trunk/leaf block IDs, the
 * single planted item (including its data components), the current shape
 * variant, and the dead-tree flag. No ticking is needed; the entity exists
 * solely to provide immutable client-side model data for chunk compilation.
 */
public final class BonsaiBlockEntity extends BlockEntity {
    private static final String TRUNK_KEY = "TrunkBlock";
    private static final String LEAVES_KEY = "LeavesBlock";
    private static final String SHAPE_KEY = "Shape";
    private static final String DEAD_KEY = "Dead";
    private static final String PLANT_SLOT_KEY = "PlantSlot";

    /**
     * Immutable snapshot consumed by the chunk compiler.  The snapshot keeps
     * asynchronous model collection off the live block entity, which is
     * important because chunk meshing runs on worker threads.
     */
    public static final ModelProperty<RenderData> RENDER_DATA = new ModelProperty<>();

    /**
     * Bonsai shape variants that the player can cycle through.
     * Order matters: semi_cascade is the initial planting shape.
     */
    public enum Shape implements StringRepresentable {
        SEMI_CASCADE("semi_cascade"),
        SLANTING("slanting"),
        TWIN("twin"),
        WINDSWEPT("windswept");

        private final String serializedName;

        public static final StringRepresentable.EnumCodec<Shape> CODEC =
                StringRepresentable.fromEnum(Shape::values);

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

    public record RenderData(
            boolean planted,
            boolean dead,
            Shape shape,
            @Nullable Identifier trunkBlockId,
            @Nullable Identifier leavesBlockId
    ) {
        public RenderData {
            if (!planted) {
                dead = false;
                trunkBlockId = null;
                leavesBlockId = null;
                shape = Shape.SEMI_CASCADE;
            } else if (dead) {
                leavesBlockId = null;
            }
        }
    }

    private boolean dead = false;
    private @Nullable Identifier trunkBlockId;
    private @Nullable Identifier leavesBlockId;
    private Shape shape = Shape.SEMI_CASCADE;
    private final BonsaiPlantStorage plantStorage = new BonsaiPlantStorage();

    public BonsaiBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.BONSAI.get(), pos, blockState);
    }

    public boolean isPlanted() {
        return !plantStorage.getStoredStack().isEmpty();
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

    /** Returns the original sapling/dead bush for an explicit clear action. */
    public ItemStack getPlantedItemStack() {
        return plantStorage.getStoredStack();
    }

    public ResourceHandler<ItemResource> getPlantStorage() {
        return plantStorage;
    }

    /**
     * Plants a sapling (or dead bush) into this bonsai pot.
     *
     * @param trunkBlock  the resolved trunk block
     * @param leavesBlock the resolved leaves block
     * @param plantedItem the item that was planted (for drop recovery)
     * @param dead        whether this is a dead-tree planting
     */
    public void plant(Block trunkBlock, Block leavesBlock, ItemStack plantedItem, boolean dead) {
        this.dead = dead;
        this.trunkBlockId = BuiltInRegistries.BLOCK.getKey(trunkBlock);
        this.leavesBlockId = BuiltInRegistries.BLOCK.getKey(leavesBlock);
        this.plantStorage.setStoredStack(plantedItem);
        this.shape = Shape.SEMI_CASCADE;
        setChangedAndSync();
    }

    /** Cycles to the next bonsai shape. Only valid when planted. */
    public void cycleShape() {
        if (!isPlanted()) {
            return;
        }
        this.shape = this.shape.next();
        setChangedAndSync();
    }

    /** Turns a living bonsai into a dead one (scissors on leaves). */
    public void makeDead() {
        if (!isPlanted() || dead) {
            return;
        }
        this.dead = true;
        setChangedAndSync();
    }

    /** Clears the bonsai back to an empty pot (scissors on dead tree). */
    public void clear() {
        this.dead = false;
        this.trunkBlockId = null;
        this.leavesBlockId = null;
        this.plantStorage.setStoredStack(ItemStack.EMPTY);
        this.shape = Shape.SEMI_CASCADE;
        setChangedAndSync();
    }

    private void setChangedAndSync() {
        setChanged();
        requestModelDataUpdate();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.dead = input.getBooleanOr(DEAD_KEY, false);
        this.shape = Shape.fromName(input.getStringOr(SHAPE_KEY, Shape.SEMI_CASCADE.getSerializedName()));
        this.trunkBlockId = input.getString(TRUNK_KEY)
                .map(Identifier::tryParse)
                .filter(BonsaiBlockEntity::isRegisteredBlock)
                .orElse(null);
        this.leavesBlockId = input.getString(LEAVES_KEY)
                .map(Identifier::tryParse)
                .filter(BonsaiBlockEntity::isRegisteredBlock)
                .orElse(null);
        this.plantStorage.setStoredStack(input.read(PLANT_SLOT_KEY, ItemStack.CODEC)
                .map(stack -> stack.copyWithCount(1))
                .orElse(ItemStack.EMPTY));

        if (isPlanted() && (trunkBlockId == null || leavesBlockId == null
                || !isSupportedPlant(plantStorage.getStoredStack()))) {
            dead = false;
            trunkBlockId = null;
            leavesBlockId = null;
            plantStorage.setStoredStack(ItemStack.EMPTY);
            shape = Shape.SEMI_CASCADE;
        } else if (!isPlanted()) {
            dead = false;
            trunkBlockId = null;
            leavesBlockId = null;
            shape = Shape.SEMI_CASCADE;
        }
        requestModelDataUpdate();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean(DEAD_KEY, dead);
        output.putString(SHAPE_KEY, shape.getSerializedName());
        if (trunkBlockId != null) {
            output.putString(TRUNK_KEY, trunkBlockId.toString());
        }
        if (leavesBlockId != null) {
            output.putString(LEAVES_KEY, leavesBlockId.toString());
        }
        ItemStack storedPlant = plantStorage.getStoredStack();
        if (!storedPlant.isEmpty()) {
            output.store(PLANT_SLOT_KEY, ItemStack.CODEC, storedPlant);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public void onDataPacket(Connection connection, ValueInput input) {
        loadWithComponents(input);

        if (level != null && level.isClientSide()) {
            requestModelDataUpdate();
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public ModelData getModelData() {
        return isPlanted()
                ? ModelData.of(RENDER_DATA, new RenderData(true, dead, shape, trunkBlockId, leavesBlockId))
                : ModelData.EMPTY;
    }

    private static boolean isSupportedPlant(ItemStack stack) {
        return stack.is(Items.DEAD_BUSH) || Block.byItem(stack.getItem()) instanceof SaplingBlock;
    }

    private static final class BonsaiPlantStorage extends ItemStackResourceHandler {
        private ItemStack storedStack = ItemStack.EMPTY;

        private ItemStack getStoredStack() {
            return storedStack.copy();
        }

        private void setStoredStack(ItemStack stack) {
            storedStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        }

        @Override
        protected ItemStack getStack() {
            return storedStack;
        }

        @Override
        protected void setStack(ItemStack stack) {
            setStoredStack(stack);
        }

        @Override
        protected boolean isValid(ItemResource resource) {
            return isSupportedPlant(resource.toStack(1));
        }

        @Override
        protected int getCapacity(ItemResource resource) {
            return 1;
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    }

    private static boolean isRegisteredBlock(Identifier id) {
        return BuiltInRegistries.BLOCK.getValue(id) != Blocks.AIR;
    }

}
