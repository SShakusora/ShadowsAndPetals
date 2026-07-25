package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;
import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import com.sshakusora.shadowsandpetals.menu.TeapotMenu;
import com.sshakusora.shadowsandpetals.recipe.TeapotRecipe;
import com.sshakusora.shadowsandpetals.recipe.TeapotRecipeInput;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.registries.RecipeSerializerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class CopperTeapotBlockEntity extends RandomizableContainerBlockEntity {
    public static final int TEA_SLOT = 0;
    public static final int FLUID_CONTAINER_SLOT = 1;
    public static final int CONTAINER_SIZE = 2;
    public static final int FLUID_CAPACITY = FluidType.BUCKET_VOLUME;
    public static final float MAX_LID_LIFT = 1.5F / 16.0F;

    private static final int OPEN_EVENT_ID = 1;
    private static final float LID_SPEED = 0.1F;
    private static final String BREW_PROGRESS_KEY = "BrewProgress";
    private static final String ACTIVE_RECIPE_KEY = "ActiveRecipe";
    private static final Component DEFAULT_NAME =
            Component.translatable(BuiltinLanguageKeys.COPPER_TEAPOT_CONTAINER_NAME.key());

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final TeapotFluidTank fluidTank = new TeapotFluidTank();
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> BuiltInRegistries.FLUID.getId(fluidTank.getResource(0).getFluid());
                case 1 -> (int) fluidTank.getAmountAsLong(0);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 2;
        }
    };
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
            if (player.containerMenu instanceof TeapotMenu teapotMenu) {
                Container container = teapotMenu.getContainer();
                return container == CopperTeapotBlockEntity.this;
            }
            return false;
        }
    };

    private int openCount;
    private int brewProgress;
    private @Nullable Identifier activeRecipeId;
    private float lidProgress;
    private float lidProgressOld;

    public CopperTeapotBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.COPPER_TEAPOT.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CopperTeapotBlockEntity blockEntity) {
        if (!level.isClientSide()) {
            blockEntity.tryBrew((ServerLevel) level);
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

    public static boolean isFluidContainer(ItemStack stack) {
        return !stack.isEmpty()
                && ItemAccess.forStack(stack).oneByOne().getCapability(Capabilities.Fluid.ITEM) != null;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
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
        brewProgress = Math.max(0, input.getIntOr(BREW_PROGRESS_KEY, 0));
        activeRecipeId = input.getString(ACTIVE_RECIPE_KEY)
                .map(Identifier::tryParse)
                .orElse(null);
        if (activeRecipeId == null) {
            brewProgress = 0;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!trySaveLootTable(output)) {
            ContainerHelper.saveAllItems(output, items);
        }
        fluidTank.serialize(output);
        if (brewProgress > 0 && activeRecipeId != null) {
            output.putInt(BREW_PROGRESS_KEY, brewProgress);
            output.putString(ACTIVE_RECIPE_KEY, activeRecipeId.toString());
        }
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case TEA_SLOT -> !isFluidContainer(stack);
            case FLUID_CONTAINER_SLOT -> isFluidContainer(stack);
            default -> false;
        };
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (slot == FLUID_CONTAINER_SLOT) {
            tryTransferFluidContainer();
        }
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    private void tryTransferFluidContainer() {
        if (level == null || level.isClientSide() || getItem(FLUID_CONTAINER_SLOT).isEmpty()) {
            return;
        }

        ResourceHandler<ItemResource> itemHandler = VanillaContainerWrapper.of(this);
        ItemAccess containerAccess = ItemAccess
                .forHandlerIndexStrict(itemHandler, FLUID_CONTAINER_SLOT)
                .oneByOne();
        ResourceHandler<FluidResource> containerTank =
                containerAccess.getCapability(Capabilities.Fluid.ITEM);
        if (containerTank == null) {
            return;
        }

        if (hasFluid(containerTank)) {
            if (fluidTank.getAmountAsLong(0) == 0) {
                ResourceHandlerUtil.moveFirst(
                        containerTank, fluidTank, resource -> true, FLUID_CAPACITY, null);
            }
        } else if (fluidTank.getAmountAsLong(0) > 0) {
            ResourceHandlerUtil.moveFirst(
                    fluidTank, containerTank, resource -> true, FLUID_CAPACITY, null);
        }
    }

    private static boolean hasFluid(ResourceHandler<FluidResource> handler) {
        for (int index = 0; index < handler.size(); index++) {
            if (!handler.getResource(index).isEmpty() && handler.getAmountAsLong(index) > 0) {
                return true;
            }
        }
        return false;
    }

    private void tryBrew(ServerLevel level) {
        ItemStack ingredientStack = getItem(TEA_SLOT);
        FluidResource fluidResource = fluidTank.getResource(0);
        int fluidAmount = fluidTank.getAmountAsInt(0);
        if (ingredientStack.isEmpty() || fluidResource.isEmpty() || fluidAmount == 0) {
            resetBrewProgress();
            return;
        }

        TeapotRecipeInput input = new TeapotRecipeInput(
                fluidResource.toStack(fluidAmount), ingredientStack);
        var recipeHolder = level.recipeAccess()
                .getRecipeFor(RecipeSerializerRegistry.TEAPOT_BREWING_TYPE.get(), input, level);
        if (recipeHolder.isEmpty()) {
            resetBrewProgress();
            return;
        }

        var holder = recipeHolder.get();
        Identifier recipeId = holder.id().identifier();
        TeapotRecipe recipe = holder.value();
        if (!recipeId.equals(activeRecipeId)) {
            activeRecipeId = recipeId;
            brewProgress = 0;
            setChanged();
        }
        if (!hasHeatSource(level)) {
            return;
        }

        brewProgress = Math.min(brewProgress + 1, recipe.processingTime());
        setChanged();
        if (brewProgress >= recipe.processingTime() && applyRecipe(recipe)) {
            resetBrewProgress();
        }
    }

    private boolean hasHeatSource(ServerLevel level) {
        BlockPos belowPos = worldPosition.below();
        BlockState belowState = level.getBlockState(belowPos);
        if (belowState.hasProperty(BlockStateProperties.LIT) && belowState.getValue(BlockStateProperties.LIT)) {
            return true;
        }
        return level.getBlockEntity(belowPos) instanceof IroriBlockEntity irori
                && irori.getBurnTime() > 0;
    }

    private void resetBrewProgress() {
        if (brewProgress != 0 || activeRecipeId != null) {
            brewProgress = 0;
            activeRecipeId = null;
            setChanged();
        }
    }

    private boolean applyRecipe(TeapotRecipe recipe) {
        int inputAmount = recipe.fluid().amount();
        FluidResource inputFluid = fluidTank.getResource(0);
        FluidStack resultFluid = recipe.result().create();
        ItemStack ingredientStack = getItem(TEA_SLOT);
        ResourceHandler<ItemResource> itemHandler = VanillaContainerWrapper.of(this);

        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = fluidTank.extract(
                    0,
                    inputFluid,
                    inputAmount,
                    transaction
            );
            if (extracted != inputAmount) {
                return false;
            }

            int consumed = itemHandler.extract(
                    TEA_SLOT,
                    ItemResource.of(ingredientStack),
                    1,
                    transaction
            );
            if (consumed != 1) {
                return false;
            }

            int inserted = fluidTank.insert(
                    0,
                    FluidResource.of(resultFluid),
                    resultFluid.getAmount(),
                    transaction
            );
            if (inserted == resultFluid.getAmount()) {
                transaction.commit();
                return true;
            }
        }
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new TeapotMenu(containerId, inventory, this, dataAccess);
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
