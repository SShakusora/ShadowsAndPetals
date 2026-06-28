package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

import java.util.List;

public class CopperTeapotBlockEntity extends RandomizableContainerBlockEntity {
    public static final int PLACEHOLDER_CONTAINER_SIZE = 9;
    public static final int FLUID_CAPACITY = FluidType.BUCKET_VOLUME;
    public static final float MAX_LID_LIFT = 1.5F / 16.0F;

    private static final int OPEN_EVENT_ID = 1;
    private static final float LID_SPEED = 0.1F;
    private static final Component DEFAULT_NAME =
            Component.translatable(BuiltinLanguageKeys.COPPER_TEAPOT_CONTAINER_NAME.key());

    private NonNullList<ItemStack> items = NonNullList.withSize(PLACEHOLDER_CONTAINER_SIZE, ItemStack.EMPTY);
    private final TeapotFluidTank fluidTank = new TeapotFluidTank();
    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState blockState) {
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState blockState) {
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState blockState, int previous, int current) {
            level.blockEvent(pos, blockState.getBlock(), OPEN_EVENT_ID, current);
        }

        @Override
        public boolean isOwnContainer(Player player) {
            if (player.containerMenu instanceof ChestMenu chestMenu) {
                Container container = chestMenu.getContainer();
                return container == CopperTeapotBlockEntity.this;
            }
            return false;
        }
    };

    private int openCount;
    private float lidProgress;
    private float lidProgressOld;

    public CopperTeapotBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.COPPER_TEAPOT.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CopperTeapotBlockEntity blockEntity) {
        if (!level.isClientSide()) {
            return;
        }

        blockEntity.lidProgressOld = blockEntity.lidProgress;
        if (blockEntity.openCount > 0) {
            blockEntity.lidProgress = Math.min(1.0F, blockEntity.lidProgress + LID_SPEED);
        } else {
            blockEntity.lidProgress = Math.max(0.0F, blockEntity.lidProgress - LID_SPEED);
        }
    }

    public float getLidProgress(float partialTick) {
        return lidProgressOld + (lidProgress - lidProgressOld) * partialTick;
    }

    public FluidStacksResourceHandler getFluidTank() {
        return fluidTank;
    }

    public void recheckOpen() {
        if (!remove) {
            openersCounter.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(input)) {
            ContainerHelper.loadAllItems(input, items);
        }
        fluidTank.deserialize(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!trySaveLootTable(output)) {
            ContainerHelper.saveAllItems(output, items);
        }
        fluidTank.serialize(output);
    }

    @Override
    public int getContainerSize() {
        return PLACEHOLDER_CONTAINER_SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x1, containerId, inventory, this, 1);
    }

    @Override
    public void startOpen(ContainerUser containerUser) {
        if (!remove && !containerUser.getLivingEntity().isSpectator()) {
            openersCounter.incrementOpeners(
                    containerUser.getLivingEntity(),
                    getLevel(),
                    getBlockPos(),
                    getBlockState(),
                    containerUser.getContainerInteractionRange()
            );
        }
    }

    @Override
    public void stopOpen(ContainerUser containerUser) {
        if (!remove && !containerUser.getLivingEntity().isSpectator()) {
            openersCounter.decrementOpeners(
                    containerUser.getLivingEntity(), getLevel(), getBlockPos(), getBlockState());
        }
    }

    @Override
    public List<ContainerUser> getEntitiesWithContainerOpen() {
        return openersCounter.getEntitiesWithContainerOpen(getLevel(), getBlockPos());
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == OPEN_EVENT_ID) {
            openCount = type;
            return true;
        }
        return super.triggerEvent(id, type);
    }

    private class TeapotFluidTank extends FluidStacksResourceHandler {
        private TeapotFluidTank() {
            super(1, FLUID_CAPACITY);
        }

        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            CopperTeapotBlockEntity.this.setChanged();
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return index == 0 && !resource.isEmpty();
        }
    }
}
