package com.sshakusora.shadowsandpetals.blockentity.irori;

import com.sshakusora.shadowsandpetals.api.irori.IroriCookingProcess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.*;

/** Master-owned items and progress for the physical center cells of an Irori component. */
final class IroriCookingState {
    private static final String SLOTS_KEY = "CookingSlots";
    private static final String POSITION_KEY = "Offset";
    private static final String ITEM_KEY = "Item";
    private static final String RESULT_KEY = "Result";
    private static final String PROGRESS_KEY = "Progress";
    private static final String TOTAL_TIME_KEY = "TotalTime";
    private static final String COMPLETED_KEY = "Completed";
    private static final Comparator<BlockPos> POSITION_ORDER = Comparator
            .comparingInt((BlockPos pos) -> pos.getX())
            .thenComparingInt(BlockPos::getZ)
            .thenComparingInt(BlockPos::getY);

    private final Map<BlockPos, Slot> slots = new LinkedHashMap<>();

    boolean isEmpty() {
        return slots.isEmpty();
    }

    boolean contains(BlockPos cookingPos) {
        return slots.containsKey(cookingPos);
    }

    boolean place(BlockPos cookingPos, ItemStack input, IroriCookingProcess process) {
        if (input.isEmpty() || slots.containsKey(cookingPos)) {
            return false;
        }
        slots.put(
                cookingPos.immutable(),
                new Slot(input.copyWithCount(1), process.result(), 0, process.cookingTime(), false)
        );
        return true;
    }

    ItemStack take(BlockPos cookingPos) {
        Slot removed = slots.remove(cookingPos);
        return removed == null ? ItemStack.EMPTY : removed.item.copy();
    }

    TickResult tick() {
        boolean changed = false;
        List<BlockPos> completedPositions = new ArrayList<>();
        for (Map.Entry<BlockPos, Slot> entry : slots.entrySet()) {
            Slot slot = entry.getValue();
            if (slot.completed) {
                continue;
            }

            changed = true;
            slot.progress++;
            if (slot.progress < slot.totalTime) {
                continue;
            }

            slot.item = slot.result.copy();
            slot.result = ItemStack.EMPTY;
            slot.progress = slot.totalTime;
            slot.completed = true;
            completedPositions.add(entry.getKey());
        }
        return new TickResult(changed, List.copyOf(completedPositions));
    }

    List<PlacedItem> placedItems() {
        return slots.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(POSITION_ORDER))
                .map(entry -> new PlacedItem(entry.getKey(), entry.getValue().item))
                .toList();
    }

    List<PlacedItem> removeOutside(Set<BlockPos> validPositions) {
        List<PlacedItem> removed = new ArrayList<>();
        slots.entrySet().removeIf(entry -> {
            if (validPositions.contains(entry.getKey())) {
                return false;
            }
            removed.add(new PlacedItem(entry.getKey(), entry.getValue().item));
            return true;
        });
        return List.copyOf(removed);
    }

    List<PlacedItem> takeAll() {
        List<PlacedItem> removed = placedItems();
        slots.clear();
        return removed;
    }

    void reset() {
        slots.clear();
    }

    Snapshot snapshot() {
        List<SlotSnapshot> copiedSlots = slots.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(POSITION_ORDER))
                .map(entry -> entry.getValue().snapshot(entry.getKey()))
                .toList();
        return new Snapshot(copiedSlots);
    }

    void restore(Snapshot snapshot) {
        slots.clear();
        for (SlotSnapshot slot : snapshot.slots()) {
            slots.putIfAbsent(slot.position(), Slot.fromSnapshot(slot));
        }
    }

    void save(ValueOutput output, BlockPos masterPos) {
        if (slots.isEmpty()) {
            return;
        }

        ValueOutput.ValueOutputList slotList = output.childrenList(SLOTS_KEY);
        for (Map.Entry<BlockPos, Slot> entry : slots.entrySet()) {
            Slot slot = entry.getValue();
            ValueOutput slotOutput = slotList.addChild();
            BlockPos relativePos = entry.getKey().subtract(masterPos);
            slotOutput.putLong(POSITION_KEY, relativePos.asLong());
            slotOutput.store(ITEM_KEY, ItemStack.CODEC, slot.item);
            if (!slot.result.isEmpty()) {
                slotOutput.store(RESULT_KEY, ItemStack.CODEC, slot.result);
            }
            slotOutput.putInt(PROGRESS_KEY, slot.progress);
            slotOutput.putInt(TOTAL_TIME_KEY, slot.totalTime);
            if (slot.completed) {
                slotOutput.putBoolean(COMPLETED_KEY, true);
            }
        }
    }

    void load(ValueInput input, BlockPos masterPos) {
        slots.clear();
        for (ValueInput slotInput : input.childrenListOrEmpty(SLOTS_KEY)) {
            ItemStack item = slotInput.read(ITEM_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (item.isEmpty()) {
                continue;
            }

            var encodedPosition = slotInput.getLong(POSITION_KEY);
            if (encodedPosition.isEmpty()) {
                continue;
            }
            BlockPos relativePos = BlockPos.of(encodedPosition.get());
            BlockPos position = masterPos.offset(relativePos.getX(), relativePos.getY(), relativePos.getZ());
            ItemStack result = slotInput.read(RESULT_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            int totalTime = Math.max(1, slotInput.getIntOr(TOTAL_TIME_KEY, 1));
            int progress = Math.clamp(slotInput.getIntOr(PROGRESS_KEY, 0), 0, totalTime);
            boolean completed = slotInput.getBooleanOr(COMPLETED_KEY, false) || result.isEmpty();
            slots.putIfAbsent(
                    position.immutable(),
                    new Slot(item.copy(), result.copy(), progress, totalTime, completed)
            );
        }
    }

    record PlacedItem(BlockPos position, ItemStack stack) {
        PlacedItem {
            position = position.immutable();
            stack = stack.copy();
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }

    record TickResult(boolean changed, List<BlockPos> completedPositions) {
    }

    record Snapshot(List<SlotSnapshot> slots) {
        static final Snapshot EMPTY = new Snapshot(List.of());

        Snapshot {
            slots = List.copyOf(slots);
        }

        boolean isEmpty() {
            return slots.isEmpty();
        }
    }

    record SlotSnapshot(
            BlockPos position,
            ItemStack item,
            ItemStack result,
            int progress,
            int totalTime,
            boolean completed
    ) {
        SlotSnapshot {
            position = position.immutable();
            item = item.copy();
            result = result.copy();
        }

        @Override
        public ItemStack item() {
            return item.copy();
        }

        @Override
        public ItemStack result() {
            return result.copy();
        }
    }

    private static final class Slot {
        private ItemStack item;
        private ItemStack result;
        private int progress;
        private final int totalTime;
        private boolean completed;

        private Slot(ItemStack item, ItemStack result, int progress, int totalTime, boolean completed) {
            this.item = item;
            this.result = result;
            this.progress = progress;
            this.totalTime = totalTime;
            this.completed = completed;
        }

        private SlotSnapshot snapshot(BlockPos position) {
            return new SlotSnapshot(position, item, result, progress, totalTime, completed);
        }

        private static Slot fromSnapshot(SlotSnapshot snapshot) {
            return new Slot(
                    snapshot.item(),
                    snapshot.result(),
                    snapshot.progress(),
                    snapshot.totalTime(),
                    snapshot.completed()
            );
        }
    }
}
