package com.sshakusora.shadowsandpetals.blockentity.irori;

import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public final class IroriFuelState {
    static final int CONTAINER_SIZE = 1;

    private static final String FIREWOOD_MODEL_KEY = "FirewoodModel";
    private static final String FUEL_STACK_KEY = "FuelStack";
    private static final String BURN_TIME_KEY = "BurnTime";
    private static final String BURN_TIME_TOTAL_KEY = "BurnTimeTotal";
    private static final String BURN_CYCLE_KEY = "BurnCycle";
    private static final String ASH_STATE_KEY = "AshState";
    private static final int SMALL_MODEL_THRESHOLD = 32;

    private NonNullList<ItemStack> items = emptyItems();
    private @Nullable FirewoodModel firewoodModel;
    private int burnTime;
    private int burnTimeTotal;
    private int burnCycle;
    private boolean ashState;

    public ItemStack getFuelStack() {
        return items.getFirst();
    }

    public @Nullable FirewoodModel getFirewoodModel() {
        return firewoodModel;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getBurnTimeTotal() {
        return burnTimeTotal;
    }

    public int getBurnCycle() {
        return burnCycle;
    }

    void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    void setBurnTimeTotal(int burnTimeTotal) {
        this.burnTimeTotal = burnTimeTotal;
    }

    boolean isBurning() {
        return burnTime > 0;
    }

    boolean isFuelEmpty() {
        return getFuelStack().isEmpty();
    }

    boolean isAsh() {
        return firewoodModel != null && firewoodModel.isAsh();
    }

    boolean canIgnite(@Nullable Level level) {
        ItemStack fuelStack = getFuelStack();
        return burnTime <= 0
                && !fuelStack.isEmpty()
                && (level == null || getFuelBurnTime(fuelStack, level) > 0);
    }

    boolean canAcceptFuel(ItemStack stack, Level level) {
        return !stack.isEmpty() && getFuelBurnTime(stack, level) > 0;
    }

    boolean setFuelStack(ItemStack stack) {
        return setFuelStack(stack, Integer.MAX_VALUE);
    }

    boolean replaceFuel(ItemStack stack, RandomSource random, @Nullable Level level) {
        if (!setFuelStack(stack)) {
            return false;
        }
        if (stack.isEmpty() && !isBurning()) {
            ashState = false;
        }
        onFuelChanged(random, level);
        return true;
    }

    boolean setFuelStack(ItemStack stack, int maxStackSize) {
        ItemStack normalized = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        if (!normalized.isEmpty() && normalized.getCount() > maxStackSize) {
            normalized.setCount(maxStackSize);
        }
        ItemStack current = getFuelStack();
        if (ItemStack.matches(current, normalized)) {
            return false;
        }

        items.set(0, normalized);
        return true;
    }

    ItemStack removeFuel(int amount) {
        return amount > 0 ? ContainerHelper.removeItem(items, 0, amount) : ItemStack.EMPTY;
    }

    ItemStack takeFuel() {
        return ContainerHelper.takeItem(items, 0);
    }

    boolean clearFuel() {
        if (isFuelEmpty()) {
            return false;
        }
        items.set(0, ItemStack.EMPTY);
        return true;
    }

    void onFuelChanged(RandomSource random, @Nullable Level level) {
        if (!isFuelEmpty()) {
            ashState = false;
        }
        updateFirewoodModel(random, level);
    }

    boolean startBurning(Level level, RandomSource random) {
        ItemStack fuelStack = getFuelStack();
        if (fuelStack.isEmpty()) {
            return false;
        }

        int nextBurnTime = getFuelBurnTime(fuelStack, level);
        if (nextBurnTime <= 0) {
            return false;
        }

        burnTime = nextBurnTime;
        burnTimeTotal = nextBurnTime;
        burnCycle = burnCycle == Integer.MAX_VALUE ? 1 : burnCycle + 1;
        ashState = false;
        consumeFuel();
        updateFirewoodModel(random, level);
        return true;
    }

    boolean tickBurnTime() {
        if (burnTime <= 0) {
            return false;
        }
        burnTime--;
        return burnTime == 0;
    }

    void extinguish(RandomSource random, Level level) {
        burnTime = 0;
        burnTimeTotal = 0;
        ashState = false;
        updateFirewoodModel(random, level);
    }

    void burnOut(RandomSource random, Level level) {
        ashState = true;
        burnTimeTotal = 0;
        updateFirewoodModel(random, level);
    }

    boolean clearAsh() {
        if (!isAsh()) {
            return false;
        }
        ashState = false;
        firewoodModel = null;
        return true;
    }

    void reset() {
        items = emptyItems();
        firewoodModel = null;
        burnTime = 0;
        burnTimeTotal = 0;
        burnCycle = 0;
        ashState = false;
    }

    Snapshot snapshot() {
        return new Snapshot(
                getFuelStack().copy(),
                firewoodModel,
                burnTime,
                burnTimeTotal,
                burnCycle,
                ashState
        );
    }

    void restore(Snapshot snapshot) {
        items = emptyItems();
        items.set(0, snapshot.fuelStack().copy());
        firewoodModel = snapshot.firewoodModel();
        burnTime = snapshot.burnTime();
        burnTimeTotal = snapshot.burnTimeTotal();
        burnCycle = snapshot.burnCycle();
        ashState = snapshot.ashState();
    }

    void save(ValueOutput output) {
        if (firewoodModel != null) {
            output.putString(FIREWOOD_MODEL_KEY, firewoodModel.name());
        }
        if (burnTime > 0) {
            output.putInt(BURN_TIME_KEY, burnTime);
        }
        if (burnTimeTotal > 0) {
            output.putInt(BURN_TIME_TOTAL_KEY, burnTimeTotal);
        }
        if (burnCycle > 0) {
            output.putInt(BURN_CYCLE_KEY, burnCycle);
        }
        if (ashState) {
            output.putBoolean(ASH_STATE_KEY, true);
        }
        ContainerHelper.saveAllItems(output, items);
    }

    void load(ValueInput input) {
        firewoodModel = input.getString(FIREWOOD_MODEL_KEY)
                .flatMap(IroriFuelState::parseFirewoodModel)
                .orElse(null);
        burnTime = input.getInt(BURN_TIME_KEY).orElse(0);
        burnTimeTotal = input.getInt(BURN_TIME_TOTAL_KEY).orElse(burnTime);
        burnCycle = input.getInt(BURN_CYCLE_KEY).orElse(0);
        ashState = input.getBooleanOr(ASH_STATE_KEY, false);
        items = emptyItems();
        ContainerHelper.loadAllItems(input, items);
        if (items.getFirst().isEmpty()) {
            input.read(FUEL_STACK_KEY, ItemStack.CODEC).ifPresent(stack -> items.set(0, stack));
        }
    }

    private void consumeFuel() {
        ItemStack fuelStack = getFuelStack();
        if (fuelStack.isEmpty()) {
            return;
        }

        Item fuelItem = fuelStack.getItem();
        fuelStack.shrink(1);
        if (fuelStack.isEmpty()) {
            ItemStackTemplate remainder = fuelItem.getCraftingRemainder();
            items.set(0, remainder != null ? remainder.create() : ItemStack.EMPTY);
        }
    }

    private void updateFirewoodModel(RandomSource random, @Nullable Level level) {
        FirewoodModel nextModel = selectModelForFuel(
                random,
                getRenderableFuelCount(level),
                burnTime > 0,
                ashState,
                firewoodModel
        );
        if (firewoodModel != nextModel) {
            firewoodModel = nextModel;
        }
    }

    private int getRenderableFuelCount(@Nullable Level level) {
        int fuelCount = burnTime > 0 ? 1 : 0;
        ItemStack fuelStack = getFuelStack();
        if (fuelStack.isEmpty()) {
            return fuelCount;
        }
        if (level == null || getFuelBurnTime(fuelStack, level) > 0) {
            fuelCount += fuelStack.getCount();
        }
        return fuelCount;
    }

    private static int getFuelBurnTime(ItemStack stack, Level level) {
        return stack.getBurnTime(RecipeType.SMELTING, level.fuelValues());
    }

    private static @Nullable FirewoodModel selectModelForFuel(
            RandomSource random,
            int fuelCount,
            boolean lit,
            boolean ashState,
            @Nullable FirewoodModel currentModel
    ) {
        FirewoodModelStage stage = selectModelStage(fuelCount, lit, ashState);
        return stage == null ? null : FirewoodModel.select(random, stage, currentModel);
    }

    private static @Nullable FirewoodModelStage selectModelStage(int fuelCount, boolean lit, boolean ashState) {
        if (ashState && fuelCount <= 0) {
            return FirewoodModelStage.ASH;
        }
        if (fuelCount <= 0) {
            return null;
        }
        boolean small = fuelCount <= SMALL_MODEL_THRESHOLD;
        if (lit) {
            return small ? FirewoodModelStage.SMALL_LIT : FirewoodModelStage.LIT;
        }
        return small ? FirewoodModelStage.SMALL_UNLIT : FirewoodModelStage.UNLIT;
    }

    private static Optional<FirewoodModel> parseFirewoodModel(String name) {
        try {
            return Optional.of(FirewoodModel.valueOf(name));
        } catch (IllegalArgumentException _) {
            return Optional.empty();
        }
    }

    private static NonNullList<ItemStack> emptyItems() {
        return NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    }

    record Snapshot(
            ItemStack fuelStack,
            @Nullable FirewoodModel firewoodModel,
            int burnTime,
            int burnTimeTotal,
            int burnCycle,
            boolean ashState
    ) {
        static final Snapshot EMPTY = new Snapshot(ItemStack.EMPTY, null, 0, 0, 0, false);

        boolean isEmpty() {
            return fuelStack.isEmpty() && firewoodModel == null && burnTime <= 0 && !ashState;
        }
    }

    public enum FirewoodModelStage {
        UNLIT,
        SMALL_UNLIT,
        LIT,
        SMALL_LIT,
        ASH;

        private static final int VARIANT_COUNT = 3;
    }

    public enum FirewoodModel {
        UNLIT_1(FirewoodModelStage.UNLIT, "unlit_1", 0),
        UNLIT_2(FirewoodModelStage.UNLIT, "unlit_2", 1),
        UNLIT_3(FirewoodModelStage.UNLIT, "unlit_3", 2),
        SMALL_UNLIT_1(FirewoodModelStage.SMALL_UNLIT, "small_unlit_1", 0),
        SMALL_UNLIT_2(FirewoodModelStage.SMALL_UNLIT, "small_unlit_2", 1),
        SMALL_UNLIT_3(FirewoodModelStage.SMALL_UNLIT, "small_unlit_3", 2),
        LIT_1(FirewoodModelStage.LIT, "lit_1", 0),
        LIT_2(FirewoodModelStage.LIT, "lit_2", 1),
        LIT_3(FirewoodModelStage.LIT, "lit_3", 2),
        SMALL_LIT_1(FirewoodModelStage.SMALL_LIT, "small_lit_1", 0),
        SMALL_LIT_2(FirewoodModelStage.SMALL_LIT, "small_lit_2", 1),
        SMALL_LIT_3(FirewoodModelStage.SMALL_LIT, "small_lit_3", 2),
        ASH_1(FirewoodModelStage.ASH, "ash_1", 0),
        ASH_2(FirewoodModelStage.ASH, "ash_2", 1),
        ASH_3(FirewoodModelStage.ASH, "ash_3", 2);

        private final FirewoodModelStage stage;
        private final String modelName;
        private final int variantIndex;

        FirewoodModel(FirewoodModelStage stage, String modelName, int variantIndex) {
            this.stage = stage;
            this.modelName = modelName;
            this.variantIndex = variantIndex;
        }

        public String modelName() {
            return modelName;
        }

        boolean isAsh() {
            return stage == FirewoodModelStage.ASH;
        }

        public FirewoodModelStage stage() {
            return stage;
        }

        private static FirewoodModel select(
                RandomSource random,
                FirewoodModelStage stage,
                @Nullable FirewoodModel currentModel
        ) {
            if (currentModel != null && currentModel.stage == stage) {
                return currentModel;
            }
            int variantIndex = currentModel != null
                    ? currentModel.variantIndex
                    : random.nextInt(FirewoodModelStage.VARIANT_COUNT);
            return byStageAndVariant(stage, variantIndex);
        }

        private static FirewoodModel byStageAndVariant(FirewoodModelStage stage, int variantIndex) {
            for (FirewoodModel model : values()) {
                if (model.stage == stage && model.variantIndex == variantIndex) {
                    return model;
                }
            }
            throw new IllegalStateException("Missing firewood model for stage " + stage + " variant " + variantIndex);
        }
    }
}
