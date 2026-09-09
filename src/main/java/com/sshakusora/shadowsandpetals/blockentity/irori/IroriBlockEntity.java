package com.sshakusora.shadowsandpetals.blockentity.irori;

import com.sshakusora.shadowsandpetals.block.decoration.irori.*;
import com.sshakusora.shadowsandpetals.blockentity.CopperTeapotBlockEntity;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriFuelState.FirewoodModel;
import com.sshakusora.shadowsandpetals.client.effect.IroriClientEffects;
import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import com.sshakusora.shadowsandpetals.event.IroriPhantomRepellent;
import com.sshakusora.shadowsandpetals.menu.IroriMenu;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class IroriBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final String MASTER_POS_KEY = "MasterPos";
    private static final String GRILL_INSTALLED_KEY = "GrillInstalled";
    private static final String TOPOLOGY_PLACEMENT_PENDING_KEY = "TopologyPlacementPending";
    private static final int MIN_ASH_BONE_MEAL_DROPS = 1;
    private static final int MAX_ASH_BONE_MEAL_DROPS = 3;
    private static final FirewoodRenderOffset ZERO_RENDER_OFFSET = new FirewoodRenderOffset(0.0D, 0.0D);
    private static final Map<Level, Set<BlockPos>> PENDING_TOPOLOGY_PLACEMENTS = new WeakHashMap<>();

    private @Nullable BlockPos masterPos;
    private final IroriFuelState fuelState = new IroriFuelState();
    private final IroriCookingState cookingState = new IroriCookingState();
    private boolean grillInstalled;
    private boolean mutatingGrillStructure;
    private boolean topologyEjectionInProgress;
    private boolean topologyPlacementPending;
    private double cachedRenderOffsetX;
    private double cachedRenderOffsetZ;
    private int cachedComponentWidth = 1;
    private int cachedComponentDepth = 1;
    private boolean renderOffsetCached;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            IroriBlockEntity master = getMaster();
            return switch (index) {
                case 0 -> master.fuelState.getBurnTime();
                case 1 -> master.fuelState.getBurnTimeTotal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            IroriBlockEntity master = getMaster();
            switch (index) {
                case 0 -> master.fuelState.setBurnTime(value);
                case 1 -> master.fuelState.setBurnTimeTotal(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(BuiltinLanguageKeys.IRORI_CONTAINER_NAME.key());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        IroriBlockEntity master = resolveMaster();
        return new IroriMenu(id, playerInv, master, master.getDataAccess());
    }

    public IroriBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.IRORI.get(), pos, blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            if (consumePendingTopologyPlacement(level, worldPosition)) {
                topologyPlacementPending = true;
            }
            serverLevel.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState newState) {
        if (level instanceof ServerLevel && !(newState.getBlock() instanceof IroriBlock)) {
            ejectContentsForTopologyChange(pos);
        }
        super.preRemoveSideEffects(pos, newState);
    }

    public IroriBlockEntity getMaster() {
        if (masterPos == null || level == null) {
            return this;
        }

        BlockEntity blockEntity = level.getBlockEntity(masterPos);
        if (blockEntity instanceof IroriBlockEntity master && master.isValidMaster()) {
            return master;
        }
        return this;
    }

    public IroriBlockEntity resolveMaster() {
        IroriBlockEntity master = getMaster();
        if (master != this || masterPos == null) {
            return master;
        }

        masterPos = null;
        setChanged();
        invalidateRenderOffsetCache();
        return this;
    }

    public boolean isValidMaster() {
        return masterPos == null;
    }

    public void setMasterPos(@Nullable BlockPos pos) {
        if (Objects.equals(masterPos, pos)) {
            return;
        }

        masterPos = pos;
        invalidateRenderOffsetCache();
        setChanged();
    }

    public @Nullable BlockPos getMasterPos() {
        return masterPos;
    }

    /**
     * Marks a newly placed cell so its first server reconciliation can distinguish an actual
     * component expansion or merge from a normal load or neighbor-state refresh.
     */
    public static void markTopologyPlacementPending(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        synchronized (PENDING_TOPOLOGY_PLACEMENTS) {
            PENDING_TOPOLOGY_PLACEMENTS
                    .computeIfAbsent(level, ignored -> new HashSet<>())
                    .add(pos.immutable());
        }
    }

    private static boolean consumePendingTopologyPlacement(Level level, BlockPos pos) {
        synchronized (PENDING_TOPOLOGY_PLACEMENTS) {
            Set<BlockPos> positions = PENDING_TOPOLOGY_PLACEMENTS.get(level);
            if (positions == null || !positions.remove(pos)) {
                return false;
            }
            if (positions.isEmpty()) {
                PENDING_TOPOLOGY_PLACEMENTS.remove(level);
            }
            return true;
        }
    }

    private boolean hasTopologyPlacementPending() {
        return topologyPlacementPending;
    }

    private void clearTopologyPlacementPending() {
        topologyPlacementPending = false;
    }

    public boolean canIgnite() {
        IroriBlockEntity master = getMaster();
        return master.fuelState.canIgnite(master.level);
    }

    public @Nullable FirewoodModel getFirewoodModel() {
        return getMaster().fuelState.getFirewoodModel();
    }

    public ItemStack getFuelStack() {
        return getMaster().fuelState.getFuelStack();
    }

    public int getBurnTime() {
        return getMaster().fuelState.getBurnTime();
    }

    public int getBurnTimeTotal() {
        return getMaster().fuelState.getBurnTimeTotal();
    }

    public int getBurnCycle() {
        return getMaster().fuelState.getBurnCycle();
    }

    public boolean tryIgnite(Level level, RandomSource random) {
        IroriBlockEntity master = resolveMaster();
        if (master.fuelState.isBurning()) {
            return false;
        }

        if (!master.startBurningFromFuel(level, random)) {
            return false;
        }

        master.setChanged();
        master.syncToClient();
        return true;
    }

    public void setFuelStack(ItemStack stack, RandomSource random) {
        IroriBlockEntity master = resolveMaster();
        if (!master.fuelState.replaceFuel(stack, random, master.level)) {
            return;
        }

        master.setChanged();
        master.syncToClient();
    }

    public boolean hasInstalledGrill() {
        return getMaster().grillInstalled;
    }

    /**
     * Installs the component-wide grill state and its upper block footprint.
     * The caller owns item consumption and synchronization.  All replaceability
     * checks happen before any world mutation so a blocked footprint never
     * consumes the ingot or leaves a half-installed grill behind.
     */
    public boolean installGrill() {
        if (level == null || level.isClientSide()) {
            return false;
        }

        IroriBlockEntity master = resolveMaster();
        if (master.grillInstalled) {
            return false;
        }

        Map<BlockPos, IroriGrillPart> parts = IroriGrillPart.forComponent(
                master.getComponentLayout(),
                master.getBlockPos()
        );
        if (!master.canInstallGrillParts(parts)) {
            return false;
        }

        master.grillInstalled = true;
        master.refreshGrillState();
        master.setChanged();
        return true;
    }

    /**
     * Removes one installed grill structure.  This is called by the upper
     * block's break/remove hooks, so it is deliberately idempotent: removing
     * several upper cells during one block update still drops exactly one
     * ingot.
     */
    public static void removeInstalledGrill(
            Level level,
            BlockPos lowerPos,
            @Nullable BlockPos brokenUpperPos,
            boolean dropGrill
    ) {
        if (level.isClientSide() || !(level.getBlockEntity(lowerPos) instanceof IroriBlockEntity irori)) {
            return;
        }

        IroriBlockEntity master = irori.resolveMaster();
        if (master.mutatingGrillStructure) {
            return;
        }
        if (!master.grillInstalled) {
            if (brokenUpperPos != null) {
                BlockState brokenState = level.getBlockState(brokenUpperPos);
                dropGrillPartContents(level, brokenUpperPos, brokenState, dropGrill);
            }
            return;
        }

        BlockPos dropPos = brokenUpperPos != null ? brokenUpperPos : master.getBlockPos().above();
        master.grillInstalled = false;
        master.mutatingGrillStructure = true;
        try {
            master.removeGrillParts(Set.of(), dropGrill);
        } finally {
            master.mutatingGrillStructure = false;
        }
        master.refreshGrillState();
        master.setChanged();
        if (dropGrill) {
            dropItemStack(level, dropPos, new ItemStack(Items.IRON_INGOT));
        }
        master.syncToClient();
    }

    /**
     * Ejects the shared state before a connected-component topology change.
     *
     * <p>The fuel item consumed for the current burn cycle is not reconstructable and is therefore
     * not refunded.  Fuel remaining in the shared slot, placed cooking contents, a logical grill,
     * and already formed ash are dropped.  The method is idempotent so both player-destruction and
     * the block removal callback may invoke it safely.</p>
     */
    public boolean ejectContentsForTopologyChange(BlockPos dropPos) {
        if (level == null || level.isClientSide()) {
            return false;
        }

        IroriBlockEntity master = resolveMaster();
        if (master.topologyEjectionInProgress) {
            return false;
        }
        if (master.fuelState.isFuelEmpty()
                && !master.fuelState.isBurning()
                && master.fuelState.getBurnTimeTotal() <= 0
                && master.fuelState.getFirewoodModel() == null
                && master.cookingState.isEmpty()
                && !master.grillInstalled) {
            return false;
        }

        master.topologyEjectionInProgress = true;
        try {
            master.dropFuel(dropPos);
            master.dropCookingContents();

            if (master.grillInstalled) {
                master.grillInstalled = false;
                master.mutatingGrillStructure = true;
                try {
                    master.removeGrillParts(Set.of(), true);
                } finally {
                    master.mutatingGrillStructure = false;
                }
                master.refreshGrillState();
                dropItemStack(level, dropPos, new ItemStack(Items.IRON_INGOT));
            }

            if (master.isAshModel()) {
                master.dropAshResults(dropPos);
            }

            Set<BlockPos> component = IroriComponentTopology.collectConnectedComponent(level, master.getBlockPos());
            master.resetStoredState();
            syncFirewoodLightState(level, component, master.getBlockPos(), false);
            master.setChanged();
            master.syncToClient();
            return true;
        } finally {
            master.topologyEjectionInProgress = false;
        }
    }

    public boolean clearAshAndDropResults() {
        if (level == null || level.isClientSide()) {
            return false;
        }

        IroriBlockEntity master = resolveMaster();
        if (!master.isAshModel()) {
            return false;
        }

        master.dropAshResults();
        master.fuelState.clearAsh();
        master.setChanged();
        master.syncToClient();
        return true;
    }

    public boolean shouldRenderFirewood() {
        return getMaster() == this && fuelState.getFirewoodModel() != null;
    }

    public boolean hasAsh() {
        return getMaster().isAshModel();
    }

    public boolean hasCookingItem(BlockPos cookingPos) {
        return getMaster().cookingState.contains(cookingPos);
    }

    /** Returns whether one physical center cell already contains an item or a block element. */
    public boolean isSurfacePositionOccupied(BlockPos cookingPos) {
        IroriBlockEntity master = getMaster();
        if (master.cookingState.contains(cookingPos)) {
            return true;
        }
        if (master.level == null) {
            return false;
        }
        BlockState aboveState = master.level.getBlockState(cookingPos.above());
        return !aboveState.isAir() && !(aboveState.getBlock() instanceof IroriGrillBlock);
    }

    public boolean tryPlaceCookingItem(
            ServerLevel level,
            BlockPos cookingPos,
            Player player,
            ItemStack heldStack
    ) {
        IroriBlockEntity master = resolveMaster();
        if (!master.hasInstalledGrill()
                || heldStack.isEmpty()
                || master.isSurfacePositionOccupied(cookingPos)
                || !master.isValidCookingPosition(cookingPos)) {
            return false;
        }

        CookingProcess process = findCookingProcess(level, heldStack).orElse(null);
        if (process == null) {
            return false;
        }

        ItemStack placedStack = heldStack.copyWithCount(1);
        if (!master.cookingState.place(cookingPos, placedStack, process)) {
            return false;
        }
        heldStack.consume(1, player);

        level.gameEvent(player, GameEvent.BLOCK_CHANGE, cookingPos);
        master.onSurfaceContentsChanged();
        return true;
    }

    public boolean takeCookingItem(BlockPos cookingPos, Player player) {
        IroriBlockEntity master = resolveMaster();
        ItemStack removed = master.cookingState.take(cookingPos);
        if (removed.isEmpty()) {
            return false;
        }

        player.getInventory().placeItemBackInInventory(removed);
        if (master.level != null) {
            master.level.gameEvent(player, GameEvent.BLOCK_CHANGE, cookingPos);
        }
        master.onSurfaceContentsChanged();
        return true;
    }

    public List<CookingRenderItem> getCookingRenderItems() {
        IroriBlockEntity master = getMaster();
        if (master != this) {
            return List.of();
        }

        return cookingState.placedItems().stream()
                .map(item -> new CookingRenderItem(
                        item.stack(),
                        item.position().getX() - worldPosition.getX(),
                        item.position().getZ() - worldPosition.getZ(),
                        item.position().asLong(),
                        !item.completed()
                ))
                .toList();
    }

    public FirewoodRenderOffset getFirewoodRenderOffset() {
        if (level == null) {
            return ZERO_RENDER_OFFSET;
        }
        if (renderOffsetCached) {
            return new FirewoodRenderOffset(cachedRenderOffsetX, cachedRenderOffsetZ);
        }

        IroriComponentTopology.Bounds component = IroriComponentTopology.bounds(level, getBlockPos());
        IroriComponentTopology.Layout layout = IroriComponentTopology.layout(component.width(), component.depth());
        cachedComponentWidth = layout.width();
        cachedComponentDepth = layout.depth();
        cachedRenderOffsetX = layout.offsetX();
        cachedRenderOffsetZ = layout.offsetZ();
        renderOffsetCached = true;
        return new FirewoodRenderOffset(cachedRenderOffsetX, cachedRenderOffsetZ);
    }

    public IroriComponentTopology.Layout getComponentLayout() {
        getFirewoodRenderOffset();
        return IroriComponentTopology.layout(cachedComponentWidth, cachedComponentDepth);
    }

    public int getComponentSize() {
        getFirewoodRenderOffset();
        return cachedComponentWidth * cachedComponentDepth;
    }

    public @Nullable GrillLayoutInfo getGrillLayoutInfo() {
        if (level == null || getMaster() != this) {
            return null;
        }

        IroriComponentTopology.Layout layout = getComponentLayout();

        GrillModel model;
        if (layout.centerWidth() == 2 && layout.centerDepth() == 2) {
            model = GrillModel.TWO_BY_TWO;
        } else if (layout.centerWidth() == 2 || layout.centerDepth() == 2) {
            model = GrillModel.ONE_BY_TWO;
        } else {
            model = GrillModel.ONE_BY_ONE;
        }

        return new GrillLayoutInfo(
                model,
                layout.offsetX(),
                layout.offsetZ(),
                layout.rotated(),
                layout.centerWidth(),
                layout.centerDepth()
        );
    }

    public boolean isComponentWideAndDeep() {
        getFirewoodRenderOffset();
        return cachedComponentWidth > 1 && cachedComponentDepth > 1;
    }

    private static void ejectComponentSourcesForPlacement(
            ServerLevel level,
            Set<BlockPos> component,
            BlockPos placedPos
    ) {
        Set<BlockPos> sourceMasterPositions = new HashSet<>();
        for (BlockPos componentPos : component) {
            if (componentPos.equals(placedPos)) {
                continue;
            }
            if (level.getBlockEntity(componentPos) instanceof IroriBlockEntity irori) {
                IroriBlockEntity sourceMaster = irori.resolveMaster();
                if (!sourceMaster.getBlockPos().equals(placedPos)) {
                    sourceMasterPositions.add(sourceMaster.getBlockPos());
                }
            }
        }

        for (BlockPos sourceMasterPos : sourceMasterPositions) {
            if (level.getBlockEntity(sourceMasterPos) instanceof IroriBlockEntity sourceMaster) {
                sourceMaster.ejectContentsForTopologyChange(placedPos);
            }
        }

        for (BlockPos componentPos : component) {
            if (level.getBlockEntity(componentPos) instanceof IroriBlockEntity irori) {
                irori.clearTopologyPlacementPending();
            }
        }
    }

    public static void reconcileComponent(ServerLevel level, BlockPos origin) {
        if (!(level.getBlockState(origin).getBlock() instanceof IroriBlock)) {
            return;
        }

        Set<BlockPos> component = IroriComponentTopology.collectConnectedComponent(level, origin);
        for (BlockPos componentPos : component) {
            if (!(level.getBlockEntity(componentPos) instanceof IroriBlockEntity)) {
                level.scheduleTick(origin, level.getBlockState(origin).getBlock(), 1);
                return;
            }
        }

        boolean placementPending = level.getBlockEntity(origin) instanceof IroriBlockEntity placed
                && placed.hasTopologyPlacementPending();
        if (placementPending && component.size() > 1) {
            ejectComponentSourcesForPlacement(level, component, origin);
            component = IroriComponentTopology.collectConnectedComponent(level, origin);
        }

        BlockPos masterPos = IroriComponentTopology.electMaster(component);
        if (!(level.getBlockEntity(masterPos) instanceof IroriBlockEntity master)) {
            level.scheduleTick(masterPos, level.getBlockState(masterPos).getBlock(), 1);
            return;
        }

        FuelMergeResult fuelMergeResult = mergeComponentFuelStates(level, component, masterPos);
        CookingMergeResult cookingMergeResult = mergeComponentCookingStates(level, component, masterPos);
        GrillMergeResult grillMergeResult = mergeComponentGrillState(level, component);

        for (BlockPos pos : component) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IroriBlockEntity irori) {
                irori.invalidateRenderOffsetCache();
                irori.setMasterPos(pos.equals(masterPos) ? null : masterPos);
                if (!pos.equals(masterPos)) {
                    irori.fuelState.reset();
                    irori.cookingState.reset();
                    irori.grillInstalled = false;
                }
                irori.setChanged();
            }
        }

        master.fuelState.restore(fuelMergeResult.snapshot());
        master.fuelState.onFuelChanged(level.getRandom(), level);
        master.cookingState.restore(cookingMergeResult.snapshot());
        master.grillInstalled = grillMergeResult.installed();
        List<IroriCookingState.PlacedItem> removedItems = master.cookingState.removeOutside(
                IroriComponentTopology.centerPositions(component, masterPos)
        );
        dropPlacedItems(level, removedItems);
        dropPlacedItems(level, cookingMergeResult.conflicts());
        for (ItemStack overflow : fuelMergeResult.overflowFuel()) {
            dropItemStack(level, masterPos, overflow);
        }
        for (BlockPos ashDropPos : fuelMergeResult.ashDropPositions()) {
            master.dropAshResults(ashDropPos);
        }
        for (int i = 0; i < grillMergeResult.redundantCount(); i++) {
            dropItemStack(level, masterPos, new ItemStack(Items.IRON_INGOT));
        }
        master.invalidateRenderOffsetCache();
        master.setChanged();
        master.refreshGrillState();

        syncFirewoodLightState(level, component, masterPos, fuelMergeResult.snapshot().burnTime() > 0);
        syncComponentToClient(level, component);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IroriBlockEntity blockEntity) {
        if (level.isClientSide()) {
            IroriClientEffects.tick(blockEntity, level, state);
            return;
        }
        if (!blockEntity.isValidMaster() || !blockEntity.getBlockPos().equals(pos)) {
            return;
        }
        Set<BlockPos> component = IroriComponentTopology.collectConnectedComponent(level, pos);
        List<Boolean> componentWaterlogged = component.stream()
                .map(level::getBlockState)
                .map(componentState -> componentState.hasProperty(IroriBlock.WATERLOGGED)
                        && componentState.getValue(IroriBlock.WATERLOGGED))
                .toList();
        if (IroriComponentTopology.hasWaterloggedState(componentWaterlogged)) {
            if (blockEntity.fuelState.isBurning()) {
                blockEntity.fuelState.extinguish(level.getRandom(), level);
                syncFirewoodLightState(level, component, pos, false);
                blockEntity.setChanged();
                blockEntity.syncToClient();
            }
            return;
        }
        if (!blockEntity.fuelState.isBurning()) {
            return;
        }

        IroriPhantomRepellent.tick((ServerLevel) level, blockEntity);

        IroriCookingState.TickResult cookingTick = blockEntity.cookingState.tick();
        if (cookingTick.changed()) {
            blockEntity.setChanged();
        }
        if (!cookingTick.completedPositions().isEmpty()) {
            blockEntity.finishCooking((ServerLevel) level, cookingTick.completedPositions());
            blockEntity.syncToClient();
        }

        if (!blockEntity.fuelState.tickBurnTime()) {
            return;
        }

        if (!blockEntity.startBurningFromFuel(level, level.getRandom())) {
            blockEntity.fuelState.burnOut(level.getRandom(), level);
            syncFirewoodLightState(level, IroriComponentTopology.collectConnectedComponent(level, pos), pos, false);
        }
        blockEntity.setChanged();
        blockEntity.syncToClient();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (masterPos != null) {
            output.putLong(MASTER_POS_KEY, masterPos.asLong());
        }
        if (topologyPlacementPending) {
            output.putBoolean(TOPOLOGY_PLACEMENT_PENDING_KEY, true);
        }
        if (isValidMaster()) {
            if (grillInstalled) {
                output.putBoolean(GRILL_INSTALLED_KEY, true);
            }
            fuelState.save(output);
            cookingState.save(output, worldPosition);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        masterPos = input.getLong(MASTER_POS_KEY).map(BlockPos::of).orElse(null);
        topologyPlacementPending = input.getBooleanOr(TOPOLOGY_PLACEMENT_PENDING_KEY, false);
        grillInstalled = input.getBooleanOr(GRILL_INSTALLED_KEY, false);
        fuelState.load(input);
        cookingState.load(input, worldPosition);
        invalidateRenderOffsetCache();
    }

    public void syncToClient() {
        if (level != null && !level.isClientSide()) {
            IroriBlockEntity master = resolveMaster();
            master.refreshGrillState();
            syncComponentToClient(level, IroriComponentTopology.collectConnectedComponent(level, master.getBlockPos()));
        }
    }

    /** Recomputes the server-authoritative grill flags and upper block footprint. */
    public void refreshGrillState() {
        Level level = this.level;
        if (level == null || level.isClientSide()) {
            return;
        }

        IroriBlockEntity master = resolveMaster();
        Set<BlockPos> component = IroriComponentTopology.collectConnectedComponent(level, master.getBlockPos());
        Map<BlockPos, IroriGrillPart> parts = IroriGrillPart.forComponent(
                master.getComponentLayout(),
                master.getBlockPos()
        );
        Set<BlockPos> centerPositions = IroriComponentTopology.centerPositions(component, master.getBlockPos());

        // Publish the lower-half state before touching the upper blocks.  The
        // upper block validates its support in updateShape, so this ordering
        // prevents a freshly installed grill from disappearing between the
        // two block-state writes.
        for (BlockPos componentPos : component) {
            BlockState componentState = level.getBlockState(componentPos);
            if (!componentState.hasProperty(IroriBlock.HAS_GRILL)) {
                continue;
            }

            boolean shouldHaveGrill = master.grillInstalled && centerPositions.contains(componentPos);
            if (componentState.getValue(IroriBlock.HAS_GRILL) != shouldHaveGrill) {
                level.setBlock(
                        componentPos,
                        componentState.setValue(IroriBlock.HAS_GRILL, shouldHaveGrill),
                        Block.UPDATE_ALL
                );
            }
        }

        master.mutatingGrillStructure = true;
        try {
            if (master.grillInstalled) {
                master.removeUnexpectedGrillParts(parts);
                master.applyGrillParts(parts);
            } else {
                master.removeGrillParts();
            }
        } finally {
            master.mutatingGrillStructure = false;
        }
    }

    private boolean canInstallGrillParts(Map<BlockPos, IroriGrillPart> parts) {
        if (level == null) {
            return false;
        }
        for (Map.Entry<BlockPos, IroriGrillPart> entry : parts.entrySet()) {
            BlockPos upperPos = entry.getKey().above();
            BlockState state = level.getBlockState(upperPos);
            if (IroriGrillPartHolder.isGrillPart(state)
                    && IroriGrillPartHolder.masterPosition(upperPos, state).equals(worldPosition)) {
                continue;
            }
            if (!state.canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    private void applyGrillParts(Map<BlockPos, IroriGrillPart> parts) {
        if (level == null) {
            return;
        }
        for (Map.Entry<BlockPos, IroriGrillPart> entry : parts.entrySet()) {
            BlockPos upperPos = entry.getKey().above();
            BlockState current = level.getBlockState(upperPos);
            boolean waterlogged = current.hasProperty(IroriGrillBlock.WATERLOGGED)
                    ? current.getValue(IroriGrillBlock.WATERLOGGED)
                    : level.getFluidState(upperPos).getType() == Fluids.WATER;
            BlockState expected = BlockRegistry.IRORI_GRILL
                    .get()
                    .defaultBlockState()
                    .setValue(IroriGrillBlock.GRILL_PART, entry.getValue())
                    .setValue(IroriGrillBlock.WATERLOGGED, waterlogged);
            if (current.getBlock() instanceof IroriGrillCopperTeapotBlock) {
                BlockState updated = current
                        .setValue(IroriGrillBlock.GRILL_PART, entry.getValue())
                        .setValue(IroriGrillBlock.WATERLOGGED, waterlogged);
                if (!current.equals(updated)) {
                    level.setBlock(upperPos, updated, Block.UPDATE_ALL);
                }
            } else if (IroriGrillPartHolder.isGrillPart(current)) {
                BlockState updated = current
                        .setValue(IroriGrillBlock.GRILL_PART, entry.getValue())
                        .setValue(IroriGrillBlock.WATERLOGGED, waterlogged);
                if (!current.equals(updated)) {
                    level.setBlock(upperPos, updated, Block.UPDATE_ALL);
                }
            } else if (current.canBeReplaced()) {
                level.setBlock(upperPos, expected, Block.UPDATE_ALL);
            }
        }
    }

    private void removeUnexpectedGrillParts(Map<BlockPos, IroriGrillPart> expected) {
        if (level == null) {
            return;
        }
        removeGrillParts(
                expected.keySet(),
                IroriComponentTopology.collectConnectedComponent(level, worldPosition),
                true
        );
    }

    private void removeGrillParts() {
        removeGrillParts(Set.of(), null, true);
    }

    private void removeGrillParts(Set<BlockPos> expectedLowerPositions, boolean dropCompositeTeapots) {
        removeGrillParts(expectedLowerPositions, null, dropCompositeTeapots);
    }

    private void removeGrillParts(
            Set<BlockPos> expectedLowerPositions,
            @Nullable Set<BlockPos> component,
            boolean dropCompositeTeapots
    ) {
        if (level == null) {
            return;
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos upperPos = worldPosition.offset(dx, 1, dz);
                BlockState state = level.getBlockState(upperPos);
                if (!IroriGrillPartHolder.isGrillPart(state)) {
                    continue;
                }
                boolean belongsToMaster = IroriGrillPartHolder.masterPosition(upperPos, state)
                        .equals(worldPosition);
                if (component != null && component.contains(upperPos.below())) {
                    belongsToMaster = true;
                }
                if (!belongsToMaster) {
                    continue;
                }
                if (expectedLowerPositions.contains(upperPos.below())) {
                    continue;
                }
                dropGrillPartContents(level, upperPos, state, dropCompositeTeapots);
                BlockState replacement = state.getValue(IroriGrillBlock.WATERLOGGED)
                        ? Fluids.WATER.defaultFluidState().createLegacyBlock()
                        : Blocks.AIR.defaultBlockState();
                level.setBlock(upperPos, replacement, Block.UPDATE_ALL);
            }
        }
    }

    /**
     * Marks placed surface content as changed and refreshes the whole connected Irori component.
     * Surface placement mechanics should use this single update path.
     */
    public void onSurfaceContentsChanged() {
        IroriBlockEntity master = resolveMaster();
        master.setChanged();
        master.syncToClient();
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        loadCustomOnly(input);
        syncFirewoodLightStateFromBlockEntityData();
    }

    @Override
    public void onDataPacket(Connection net, ValueInput input) {
        loadWithComponents(input);
        syncFirewoodLightStateFromBlockEntityData();
    }

    @Override
    public int getContainerSize() {
        return IroriFuelState.CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return getMaster().fuelState.isFuelEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        return getMaster().fuelState.getFuelStack();
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }

        IroriBlockEntity master = resolveMaster();
        ItemStack removed = master.fuelState.removeFuel(amount);
        if (!removed.isEmpty()) {
            master.afterFuelChanged(master.getLevelRandom());
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }

        IroriBlockEntity master = resolveMaster();
        ItemStack removed = master.fuelState.takeFuel();
        if (!removed.isEmpty()) {
            master.afterFuelChanged(master.getLevelRandom());
        }
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) {
            return;
        }

        IroriBlockEntity master = resolveMaster();
        if (!master.fuelState.setFuelStack(stack, master.getMaxStackSize(stack))) {
            return;
        }

        master.afterFuelChanged(master.getLevelRandom());
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public void clearContent() {
        IroriBlockEntity master = resolveMaster();
        if (!master.fuelState.clearFuel()) {
            return;
        }

        master.afterFuelChanged(master.getLevelRandom());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != 0 || stack.isEmpty() || level == null) {
            return false;
        }
        return getMaster().fuelState.canAcceptFuel(stack, level);
    }

    private boolean startBurningFromFuel(Level level, RandomSource random) {
        if (!fuelState.startBurning(level, random)) {
            return false;
        }

        syncFirewoodLightState(level, IroriComponentTopology.collectConnectedComponent(level, getBlockPos()), getBlockPos(), true);
        return true;
    }

    private boolean isAshModel() {
        return fuelState.isAsh();
    }

    private void dropAshResults() {
        spawnAshDrops(worldPosition);
    }

    private void dropAshResults(BlockPos dropPos) {
        spawnAshDrops(dropPos);
    }

    private void spawnAshDrops(BlockPos dropPos) {
        if (level == null) {
            return;
        }

        RandomSource random = level.getRandom();
        int count = MIN_ASH_BONE_MEAL_DROPS + random.nextInt(
                MAX_ASH_BONE_MEAL_DROPS - MIN_ASH_BONE_MEAL_DROPS + 1
        );
        dropItemStack(level, dropPos, new ItemStack(Items.BONE_MEAL, count));
    }

    private boolean isValidCookingPosition(BlockPos cookingPos) {
        if (level == null) {
            return false;
        }
        Set<BlockPos> component = IroriComponentTopology.collectConnectedComponent(level, worldPosition);
        return IroriComponentTopology.centerPositions(component, worldPosition).contains(cookingPos);
    }

    private static Optional<CookingProcess> findCookingProcess(ServerLevel level, ItemStack input) {
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        return findRecipeProcess(level, recipeInput, RecipeType.CAMPFIRE_COOKING)
                .or(() -> findRecipeProcess(level, recipeInput, RecipeType.SMOKING));
    }

    private static <T extends AbstractCookingRecipe> Optional<CookingProcess> findRecipeProcess(
            ServerLevel level,
            SingleRecipeInput input,
            RecipeType<T> recipeType
    ) {
        return level.recipeAccess()
                .getRecipeFor(recipeType, input, level)
                .map(RecipeHolder::value)
                .flatMap(recipe -> {
                    ItemStack result = recipe.assemble(input);
                    if (result.isEmpty() || !result.isItemEnabled(level.enabledFeatures())) {
                        return Optional.empty();
                    }
                    return Optional.of(new CookingProcess(result, recipe.cookingTime()));
                });
    }

    private void finishCooking(ServerLevel level, List<BlockPos> completedPositions) {
        for (BlockPos completedPos : completedPositions) {
            double x = completedPos.getX() + 0.5D;
            double y = completedPos.getY() + 1.35D;
            double z = completedPos.getZ() + 0.5D;
            ItemStack completedStack = cookingState.itemAt(completedPos);
            level.sendParticles(ParticleTypes.POOF, x, y, z, 5, 0.14D, 0.035D, 0.14D, 0.025D);
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, 3, 0.13D, 0.03D, 0.13D, 0.012D);
            if (!completedStack.isEmpty()) {
                ItemParticleOption completedItemParticle = new ItemParticleOption(
                        ParticleTypes.ITEM,
                        ItemStackTemplate.fromNonEmptyStack(completedStack)
                );
                level.sendParticles(completedItemParticle, x, y, z, 9, 0.14D, 0.035D, 0.14D, 0.055D);
            }
            level.playSound(
                    null,
                    completedPos,
                    SoundEvents.GENERIC_EXTINGUISH_FIRE,
                    SoundSource.BLOCKS,
                    0.35F,
                    1.6F
            );
            level.gameEvent(GameEvent.BLOCK_CHANGE, completedPos, GameEvent.Context.of(getBlockState()));
        }
    }

    private void dropFuel(BlockPos dropOrigin) {
        if (level == null) {
            return;
        }
        dropItemStack(level, dropOrigin, fuelState.takeFuel());
    }

    private void dropCookingContents() {
        if (level == null) {
            return;
        }
        dropPlacedItems(level, cookingState.takeAll());
    }

    private static void dropPlacedItems(
            Level level,
        List<IroriCookingState.PlacedItem> items
    ) {
        for (IroriCookingState.PlacedItem item : items) {
            dropItemStack(level, item.position(), item.stack());
        }
    }

    private static void dropGrillPartContents(
            Level level,
            BlockPos upperPos,
            BlockState state,
            boolean dropCompositeTeapot
    ) {
        if (!(state.getBlock() instanceof IroriGrillCopperTeapotBlock) || !dropCompositeTeapot) {
            return;
        }
        if (level.getBlockEntity(upperPos) instanceof CopperTeapotBlockEntity teapot) {
            for (int slot = 0; slot < teapot.getContainerSize(); slot++) {
                dropItemStack(level, upperPos, teapot.getItem(slot));
            }
        }
        dropItemStack(level, upperPos, new ItemStack(BlockRegistry.COPPER_TEAPOT.get()));
    }

    private static void dropItemStack(Level level, BlockPos sourcePos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        BlockState sourceState = level.getBlockState(sourcePos);
        BlockPos dropPos = sourceState.getBlock() instanceof IroriBlock
                ? sourcePos.above(sourceState.getValue(IroriBlock.HAS_GRILL) ? 2 : 1)
                : sourcePos;
        Containers.dropItemStack(level, dropPos.getX(), dropPos.getY(), dropPos.getZ(), stack);
    }

    private void resetStoredState() {
        fuelState.reset();
        cookingState.reset();
        grillInstalled = false;
        invalidateRenderOffsetCache();
    }

    public void onFuelSlotChanged() {
        IroriBlockEntity master = resolveMaster();
        master.afterFuelChanged(master.getLevelRandom());
    }

    private void afterFuelChanged(RandomSource random) {
        fuelState.onFuelChanged(random, level);
        setChanged();
        syncToClient();
    }

    private void invalidateRenderOffsetCache() {
        renderOffsetCached = false;
        cachedRenderOffsetX = 0.0D;
        cachedRenderOffsetZ = 0.0D;
        cachedComponentWidth = 1;
        cachedComponentDepth = 1;
    }

    private RandomSource getLevelRandom() {
        return level != null ? level.getRandom() : RandomSource.create(worldPosition.asLong());
    }

    private static FuelMergeResult mergeComponentFuelStates(
            Level level,
            Set<BlockPos> component,
            BlockPos electedMasterPos
    ) {
        List<FuelStateSource> sources = component.stream()
                .sorted(Comparator.comparingLong(BlockPos::asLong))
                .map(pos -> {
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (!(blockEntity instanceof IroriBlockEntity irori)) {
                        return null;
                    }
                    IroriFuelState.Snapshot snapshot = irori.fuelState.snapshot();
                    return snapshot.isEmpty() ? null : new FuelStateSource(pos, snapshot);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparingInt((FuelStateSource source) -> fuelStatePriority(source.snapshot()))
                        .reversed()
                        .thenComparing(source -> !source.position().equals(electedMasterPos))
                        .thenComparingLong(source -> source.position().asLong()))
                .toList();
        if (sources.isEmpty()) {
            return FuelMergeResult.EMPTY;
        }

        FuelStateSource primary = sources.getFirst();
        ItemStack mergedFuel = primary.snapshot().fuelStack().copy();
        List<ItemStack> overflowFuel = new ArrayList<>();
        int mergedBurnTime = 0;
        int mergedBurnTimeTotal = 0;
        int mergedBurnCycle = 0;

        for (FuelStateSource source : sources) {
            IroriFuelState.Snapshot snapshot = source.snapshot();
            mergedBurnTime = saturatedAdd(mergedBurnTime, snapshot.burnTime());
            mergedBurnTimeTotal = saturatedAdd(mergedBurnTimeTotal, snapshot.burnTimeTotal());
            mergedBurnCycle = Math.max(mergedBurnCycle, snapshot.burnCycle());
            if (source != primary) {
                mergedFuel = mergeFuelStack(mergedFuel, snapshot.fuelStack(), overflowFuel);
            }
        }
        if (mergedBurnTime > 0) {
            mergedBurnTimeTotal = Math.max(mergedBurnTimeTotal, mergedBurnTime);
        }

        boolean preservePrimaryAsh = mergedBurnTime <= 0
                && mergedFuel.isEmpty()
                && primary.snapshot().ashState();
        List<BlockPos> ashDropPositions = sources.stream()
                .filter(source -> source.snapshot().ashState())
                .filter(source -> source != primary || !preservePrimaryAsh)
                .map(FuelStateSource::position)
                .toList();

        FirewoodModel firewoodModel = primary.snapshot().firewoodModel();
        if (firewoodModel == null) {
            firewoodModel = sources.stream()
                    .map(FuelStateSource::snapshot)
                    .map(IroriFuelState.Snapshot::firewoodModel)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }

        IroriFuelState.Snapshot mergedSnapshot = new IroriFuelState.Snapshot(
                mergedFuel,
                firewoodModel,
                mergedBurnTime,
                mergedBurnTimeTotal,
                mergedBurnCycle,
                preservePrimaryAsh
        );
        return new FuelMergeResult(
                mergedSnapshot,
                List.copyOf(overflowFuel),
                List.copyOf(ashDropPositions)
        );
    }

    private static int fuelStatePriority(IroriFuelState.Snapshot snapshot) {
        if (snapshot.burnTime() > 0) {
            return 4;
        }
        if (!snapshot.fuelStack().isEmpty()) {
            return 3;
        }
        if (snapshot.ashState()) {
            return 2;
        }
        return snapshot.firewoodModel() != null ? 1 : 0;
    }

    private static ItemStack mergeFuelStack(
            ItemStack current,
            ItemStack additional,
            List<ItemStack> overflow
    ) {
        if (additional.isEmpty()) {
            return current;
        }
        if (current.isEmpty()) {
            return additional.copy();
        }
        if (!ItemStack.isSameItemSameComponents(current, additional)) {
            overflow.add(additional.copy());
            return current;
        }

        int inserted = Math.min(additional.getCount(), current.getMaxStackSize() - current.getCount());
        if (inserted > 0) {
            current.grow(inserted);
        }
        if (inserted < additional.getCount()) {
            overflow.add(additional.copyWithCount(additional.getCount() - inserted));
        }
        return current;
    }

    private static int saturatedAdd(int first, int second) {
        long sum = (long) first + second;
        return sum >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    private static GrillMergeResult mergeComponentGrillState(Level level, Set<BlockPos> component) {
        int installedCount = 0;
        for (BlockPos pos : component) {
            if (level.getBlockEntity(pos) instanceof IroriBlockEntity irori && irori.grillInstalled) {
                installedCount++;
            }
        }
        if (installedCount == 0) {
            return GrillMergeResult.EMPTY;
        }
        return new GrillMergeResult(true, installedCount - 1);
    }

    private static CookingMergeResult mergeComponentCookingStates(
            Level level,
            Set<BlockPos> component,
            BlockPos electedMasterPos
    ) {
        Map<BlockPos, IroriCookingState.SlotSnapshot> merged = new LinkedHashMap<>();
        List<IroriCookingState.PlacedItem> conflicts = new ArrayList<>();
        Set<BlockPos> sourceOrder = new LinkedHashSet<>();
        sourceOrder.add(electedMasterPos);
        component.stream()
                .sorted(Comparator.comparingLong(BlockPos::asLong))
                .filter(pos -> {
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    return blockEntity instanceof IroriBlockEntity irori && irori.isValidMaster();
                })
                .forEach(sourceOrder::add);
        component.stream()
                .sorted(Comparator.comparingLong(BlockPos::asLong))
                .forEach(sourceOrder::add);

        for (BlockPos sourcePos : sourceOrder) {
            addCookingSnapshot(level.getBlockEntity(sourcePos), merged, conflicts);
        }
        IroriCookingState.Snapshot snapshot = merged.isEmpty()
                ? IroriCookingState.Snapshot.EMPTY
                : new IroriCookingState.Snapshot(List.copyOf(merged.values()));
        return new CookingMergeResult(snapshot, List.copyOf(conflicts));
    }

    private static void addCookingSnapshot(
            @Nullable BlockEntity blockEntity,
            Map<BlockPos, IroriCookingState.SlotSnapshot> merged,
            List<IroriCookingState.PlacedItem> conflicts
    ) {
        if (!(blockEntity instanceof IroriBlockEntity irori)) {
            return;
        }
        for (IroriCookingState.SlotSnapshot slot : irori.cookingState.snapshot().slots()) {
            if (merged.putIfAbsent(slot.position(), slot) != null) {
                conflicts.add(new IroriCookingState.PlacedItem(
                        slot.position(),
                        slot.item(),
                        slot.completed()
                ));
            }
        }
    }

    private static void syncComponentToClient(Level level, Set<BlockPos> component) {
        if (level.isClientSide()) {
            return;
        }

        for (BlockPos pos : component) {
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private static void syncFirewoodLightState(Level level, Set<BlockPos> component, BlockPos masterPos, boolean lit) {
        Set<BlockPos> firewoodLightPositions = lit
                ? IroriComponentTopology.centerPositions(component, masterPos)
                : Set.of();
        for (BlockPos pos : component) {
            BlockState state = level.getBlockState(pos);
            AuxiliaryLightManager lightManager = level.getAuxLightManager(pos);
            if (lightManager != null) {
                boolean shouldLight = firewoodLightPositions.contains(pos) && state.hasProperty(IroriBlock.WATERLOGGED) && !state.getValue(IroriBlock.WATERLOGGED);
                lightManager.setLightAt(pos, shouldLight ? 13 : 0);
            }
        }
    }

    private void syncFirewoodLightStateFromBlockEntityData() {
        if (level == null) {
            return;
        }

        IroriBlockEntity master = getMaster();
        syncFirewoodLightState(
                level,
                IroriComponentTopology.collectConnectedComponent(level, master.getBlockPos()),
                master.getBlockPos(),
                master.fuelState.isBurning()
        );
    }

    private record FuelStateSource(BlockPos position, IroriFuelState.Snapshot snapshot) {
    }

    private record FuelMergeResult(
            IroriFuelState.Snapshot snapshot,
            List<ItemStack> overflowFuel,
            List<BlockPos> ashDropPositions
    ) {
        private static final FuelMergeResult EMPTY = new FuelMergeResult(
                IroriFuelState.Snapshot.EMPTY,
                List.of(),
                List.of()
        );
    }

    private record GrillMergeResult(boolean installed, int redundantCount) {
        private static final GrillMergeResult EMPTY = new GrillMergeResult(false, 0);
    }

    private record CookingMergeResult(
            IroriCookingState.Snapshot snapshot,
            List<IroriCookingState.PlacedItem> conflicts
    ) {
    }

    public record FirewoodRenderOffset(double x, double z) {
    }

    public record GrillLayoutInfo(
            GrillModel model,
            double offsetX,
            double offsetZ,
            boolean rotated,
            int centerWidth,
            int centerDepth
    ) {
    }

    public record CookingRenderItem(
            ItemStack stack,
            double offsetX,
            double offsetZ,
            long seed,
            boolean cooking
    ) {
        public CookingRenderItem {
            stack = stack.copy();
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }

    public enum GrillModel {
        ONE_BY_ONE("1_1"),
        ONE_BY_TWO("1_2"),
        TWO_BY_TWO("2_2");

        private final String modelName;

        GrillModel(String modelName) {
            this.modelName = modelName;
        }

        public String modelName() {
            return modelName;
        }
    }

}
