package com.sshakusora.shadowsandpetals.menu;

import com.sshakusora.shadowsandpetals.blockentity.IroriBlockEntity;
import com.sshakusora.shadowsandpetals.registries.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
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

public class IroriMenu extends AbstractContainerMenu {
    public static final int FUEL_SLOT_X = 80;
    public static final int FUEL_SLOT_Y = 53;

    private static final int FUEL_SLOT = 0;
    private static final int CONTAINER_SLOT_COUNT = 1;
    private static final int INV_FIRST = 1;
    private static final int INV_LAST_EXCLUSIVE = 37; // 1..36
    private static final int MAIN_INV_END_EXCLUSIVE = 28; // 1..27 main, 28..36 hotbar

    private final Container container;
    private final ContainerData data;

    /** Client-side factory entry point. */
    public IroriMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(id, playerInv, clientHandles(playerInv, buf.readBlockPos()));
    }

    private IroriMenu(int id, Inventory playerInv, Handles handles) {
        this(id, playerInv, handles.container(), handles.data());
    }

    /** Server-side entry point. */
    public IroriMenu(int id, Inventory playerInv, Container container, ContainerData data) {
        super(MenuRegistry.IRORI.get(), id);
        checkContainerSize(container, CONTAINER_SLOT_COUNT);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.data = data;

        this.addSlot(new Slot(container, FUEL_SLOT, FUEL_SLOT_X, FUEL_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(FUEL_SLOT, stack);
            }
        });
        this.addStandardInventorySlots(playerInv, 8, 84);
        this.addDataSlots(data);
    }

    private static Handles clientHandles(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof IroriBlockEntity irori) {
            return new Handles(irori, irori.getDataAccess());
        }
        return new Handles(new SimpleContainer(CONTAINER_SLOT_COUNT), new SimpleContainerData(2));
    }

    public boolean isLit() {
        return this.data.get(0) > 0;
    }

    public float getLitProgress() {
        int total = this.data.get(1);
        if (total == 0) {
            total = 200;
        }
        return Mth.clamp((float) this.data.get(0) / total, 0.0F, 1.0F);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (slotIndex == FUEL_SLOT) {
                if (!this.moveItemStackTo(stack, INV_FIRST, INV_LAST_EXCLUSIVE, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.container.canPlaceItem(FUEL_SLOT, stack)) {
                if (!this.moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex >= INV_FIRST && slotIndex < MAIN_INV_END_EXCLUSIVE) {
                if (!this.moveItemStackTo(stack, MAIN_INV_END_EXCLUSIVE, INV_LAST_EXCLUSIVE, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex >= MAIN_INV_END_EXCLUSIVE && slotIndex < INV_LAST_EXCLUSIVE) {
                if (!this.moveItemStackTo(stack, INV_FIRST, MAIN_INV_END_EXCLUSIVE, false)) {
                    return ItemStack.EMPTY;
                }
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
        }
        return result;
    }

    private record Handles(Container container, ContainerData data) {
    }
}
