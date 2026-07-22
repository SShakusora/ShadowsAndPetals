package com.sshakusora.shadowsandpetals.menu;

import com.sshakusora.shadowsandpetals.blockentity.CopperTeapotBlockEntity;
import com.sshakusora.shadowsandpetals.registries.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class TeapotMenu extends AbstractContainerMenu {
    public static final int FLUID_CONTAINER_SLOT_X = 88;
    public static final int FLUID_CONTAINER_SLOT_Y = 28;
    public static final int TEA_SLOT_X = 107;
    public static final int TEA_SLOT_Y = 28;
    public static final int PLAYER_INVENTORY_X = 18;
    public static final int PLAYER_INVENTORY_Y = 96;

    private static final int CONTAINER_SLOT_COUNT = 2;
    private static final int PLAYER_INVENTORY_FIRST = CONTAINER_SLOT_COUNT;
    private static final int MAIN_INVENTORY_END_EXCLUSIVE = PLAYER_INVENTORY_FIRST + 27;
    private static final int PLAYER_INVENTORY_END_EXCLUSIVE = PLAYER_INVENTORY_FIRST + 36;

    private final Container container;
    private final ContainerData data;

    /** Client-side factory entry point. */
    public TeapotMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, clientHandles(playerInventory, buffer.readBlockPos()));
    }

    private TeapotMenu(int id, Inventory playerInventory, Handles handles) {
        this(id, playerInventory, handles.container(), handles.data());
    }

    /** Server-side entry point. */
    public TeapotMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(MenuRegistry.TEAPOT.get(), id);
        checkContainerSize(container, CopperTeapotBlockEntity.CONTAINER_SIZE);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.data = data;

        container.startOpen(playerInventory.player);
        this.addSlot(new Slot(
                container, CopperTeapotBlockEntity.TEA_SLOT, TEA_SLOT_X, TEA_SLOT_Y
        ) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(CopperTeapotBlockEntity.TEA_SLOT, stack);
            }
        });
        this.addSlot(new Slot(
                container,
                CopperTeapotBlockEntity.FLUID_CONTAINER_SLOT,
                FLUID_CONTAINER_SLOT_X,
                FLUID_CONTAINER_SLOT_Y
        ) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(CopperTeapotBlockEntity.FLUID_CONTAINER_SLOT, stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addStandardInventorySlots(playerInventory, PLAYER_INVENTORY_X, PLAYER_INVENTORY_Y);
        this.addDataSlots(data);
    }

    private static Handles clientHandles(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        Container container = blockEntity instanceof CopperTeapotBlockEntity teapot
                ? teapot
                : new SimpleContainer(CopperTeapotBlockEntity.CONTAINER_SIZE);
        return new Handles(container, new SimpleContainerData(2));
    }

    public Container getContainer() {
        return container;
    }

    public boolean hasFluid() {
        return data.get(1) > 0 && getFluid() != Fluids.EMPTY;
    }

    public boolean hasWater() {
        return getFluid().builtInRegistryHolder().is(FluidTags.WATER);
    }

    private Fluid getFluid() {
        return BuiltInRegistries.FLUID.byId(data.get(0));
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (slotIndex < CONTAINER_SLOT_COUNT) {
            if (!this.moveItemStackTo(
                    stack, PLAYER_INVENTORY_FIRST, PLAYER_INVENTORY_END_EXCLUSIVE, true)) {
                return ItemStack.EMPTY;
            }
        } else if (CopperTeapotBlockEntity.isFluidContainer(stack)) {
            if (!this.moveItemStackTo(
                    stack,
                    CopperTeapotBlockEntity.FLUID_CONTAINER_SLOT,
                    CopperTeapotBlockEntity.FLUID_CONTAINER_SLOT + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(CopperTeapotBlockEntity.TEA_SLOT, stack)) {
            if (!this.moveItemStackTo(
                    stack, CopperTeapotBlockEntity.TEA_SLOT, CopperTeapotBlockEntity.TEA_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex < MAIN_INVENTORY_END_EXCLUSIVE) {
            if (!this.moveItemStackTo(
                    stack, MAIN_INVENTORY_END_EXCLUSIVE, PLAYER_INVENTORY_END_EXCLUSIVE, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(
                stack, PLAYER_INVENTORY_FIRST, MAIN_INVENTORY_END_EXCLUSIVE, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    private record Handles(Container container, ContainerData data) {
    }
}
