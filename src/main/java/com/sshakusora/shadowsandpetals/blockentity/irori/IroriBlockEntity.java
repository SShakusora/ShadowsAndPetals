package com.sshakusora.shadowsandpetals.blockentity.irori;

import com.sshakusora.shadowsandpetals.api.irori.*;
import com.sshakusora.shadowsandpetals.block.decoration.IroriBlock;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriFuelState.FirewoodModel;
import com.sshakusora.shadowsandpetals.client.effect.IroriClientEffects;
import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import com.sshakusora.shadowsandpetals.menu.IroriMenu;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class IroriBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final String MASTER_POS_KEY = "MasterPos";
    private static final double ASH_DROP_Y = 10.0D / 16.0D + 0.1D;
    private static final FirewoodRenderOffset ZERO_RENDER_OFFSET = new FirewoodRenderOffset(0.0D, 0.0D);

    private @Nullable BlockPos masterPos;
    private final IroriFuelState fuelState = new IroriFuelState();
    private final IroriCookingState cookingState = new IroriCookingState();
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

    public void dropContentsAndReset() {
        if (level == null || level.isClientSide()) {
            return;
        }

        IroriBlockEntity master = resolveMaster();
        if (master.fuelState.isFuelEmpty()
                && master.fuelState.getFirewoodModel() == null
                && master.cookingState.isEmpty()) {
            return;
        }

        Containers.dropContents(level, master.getBlockPos(), master);
        master.dropCookingContents(false, master.getBlockPos());
        master.resetStoredState();
        syncFirewoodLightState(level, IroriComponentTopology.collectConnectedComponent(level, master.getBlockPos()), master.getBlockPos(), false);
        master.setChanged();
        master.syncToClient();
    }

    public void dropContentsOnRemoval(BlockPos dropPos) {
        if (level == null || level.isClientSide()) {
            return;
        }

        IroriBlockEntity master = resolveMaster();
        if (master.fuelState.isFuelEmpty()
                && master.fuelState.getFirewoodModel() == null
                && master.cookingState.isEmpty()) {
            return;
        }

        Containers.dropContents(level, dropPos, master);
        master.dropCookingContents(true, dropPos);
        if (master.fuelState.isBurning() || master.isAshModel()) {
            master.dropAshResults(dropPos);
        }
        master.resetStoredState();
        syncFirewoodLightState(level, IroriComponentTopology.collectConnectedComponent(level, master.getBlockPos()), master.getBlockPos(), false);
        master.setChanged();
        master.syncToClient();
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
        return master.level != null && !master.level.getBlockState(cookingPos.above()).isAir();
    }

    public boolean tryPlaceCookingItem(
            ServerLevel level,
            BlockPos cookingPos,
            Player player,
            ItemStack heldStack
    ) {
        IroriBlockEntity master = resolveMaster();
        if (heldStack.isEmpty()
                || master.isSurfacePositionOccupied(cookingPos)
                || !master.isValidCookingPosition(cookingPos)) {
            return false;
        }

        IroriCookingContext context = new IroriCookingContext(
                level,
                master.getBlockPos(),
                cookingPos,
                heldStack
        );
        IroriCookingProcess process = IroriApi.findCookingProcess(context).orElse(null);
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
                        item.position().asLong()
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

    public @Nullable GrillRenderInfo getGrillRenderInfo() {
        GrillLayoutInfo layout = getGrillLayoutInfo();
        Level level = this.level;
        if (layout == null || level == null) {
            return null;
        }

        if (!IroriApi.requiresGrill(createApiView(level, layout))) {
            return null;
        }

        return new GrillRenderInfo(
                layout.model(),
                layout.offsetX(),
                layout.offsetZ(),
                layout.rotated()
        );
    }

    private IroriView createApiView(Level level, GrillLayoutInfo grillLayout) {
        IroriComponentTopology.Layout componentLayout = getComponentLayout();
        IroriLayout apiLayout = new IroriLayout(
                componentLayout.width(),
                componentLayout.depth(),
                componentLayout.offsetX(),
                componentLayout.offsetZ(),
                componentLayout.rotated(),
                componentLayout.centerWidth(),
                componentLayout.centerDepth()
        );

        List<IroriContent> contents = new ArrayList<>(grillLayout.centerWidth() * grillLayout.centerDepth());
        for (int x = 0; x < grillLayout.centerWidth(); x++) {
            for (int z = 0; z < grillLayout.centerDepth(); z++) {
                BlockPos contentPos = worldPosition.offset(x, 1, z);
                BlockState contentState = level.getBlockState(contentPos);
                if (!contentState.isAir()) {
                    contents.add(new IroriContent.BlockContent(contentPos, contentState));
                }
            }
        }
        for (IroriCookingState.PlacedItem item : cookingState.placedItems()) {
            contents.add(new IroriContent.ItemContent(item.position().above(), item.stack()));
        }

        return new IroriView(level, worldPosition, apiLayout, contents, fuelState.isBurning());
    }

    public boolean isComponentWideAndDeep() {
        getFirewoodRenderOffset();
        return cachedComponentWidth > 1 && cachedComponentDepth > 1;
    }

    public static void reelectMaster(Level level, Set<BlockPos> component) {
        if (component.isEmpty()) {
            return;
        }

        BlockPos masterPos = IroriComponentTopology.electMaster(component);
        IroriFuelState.Snapshot carriedState = resolveComponentFuelState(level, component, masterPos);
        IroriCookingState.Snapshot carriedCookingState = resolveComponentCookingState(level, component, masterPos);

        for (BlockPos pos : component) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IroriBlockEntity irori) {
                irori.invalidateRenderOffsetCache();
                irori.setMasterPos(pos.equals(masterPos) ? null : masterPos);
                if (!pos.equals(masterPos)) {
                    irori.fuelState.reset();
                    irori.cookingState.reset();
                }
            }
        }

        BlockEntity blockEntity = level.getBlockEntity(masterPos);
        if (blockEntity instanceof IroriBlockEntity master) {
            master.fuelState.restore(carriedState);
            master.cookingState.restore(carriedCookingState);
            List<IroriCookingState.PlacedItem> removedItems = master.cookingState.removeOutside(
                    IroriComponentTopology.centerPositions(component, masterPos)
            );
            if (!level.isClientSide()) {
                dropPlacedItems(level, removedItems, false, masterPos);
            }
            master.invalidateRenderOffsetCache();
            master.setChanged();
        }

        syncFirewoodLightState(level, component, masterPos, carriedState.burnTime() > 0);
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
        if (state.getValue(IroriBlock.WATERLOGGED)) {
            if (blockEntity.fuelState.isBurning()) {
                blockEntity.fuelState.extinguish(level.getRandom(), level);
                syncFirewoodLightState(level, IroriComponentTopology.collectConnectedComponent(level, pos), pos, false);
                blockEntity.setChanged();
                blockEntity.syncToClient();
            }
            return;
        }
        if (!blockEntity.fuelState.isBurning()) {
            return;
        }

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
        if (isValidMaster()) {
            fuelState.save(output);
            cookingState.save(output, worldPosition);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        masterPos = input.getLong(MASTER_POS_KEY).map(BlockPos::of).orElse(null);
        fuelState.load(input);
        cookingState.load(input, worldPosition);
        invalidateRenderOffsetCache();
    }

    public void syncToClient() {
        if (level != null && !level.isClientSide()) {
            syncComponentToClient(level, IroriComponentTopology.collectConnectedComponent(level, getBlockPos()));
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
        boolean grillWasVisible = getGrillRenderInfo() != null;
        loadCustomOnly(input);
        syncFirewoodLightStateFromBlockEntityData();
        refreshGrillSectionIfVisibilityChanged(grillWasVisible);
    }

    @Override
    public void onDataPacket(Connection net, ValueInput input) {
        boolean grillWasVisible = getGrillRenderInfo() != null;
        loadWithComponents(input);
        syncFirewoodLightStateFromBlockEntityData();
        refreshGrillSectionIfVisibilityChanged(grillWasVisible);
    }

    private void refreshGrillSectionIfVisibilityChanged(boolean grillWasVisible) {
        if (level == null || !level.isClientSide()) {
            return;
        }

        boolean grillIsVisible = getGrillRenderInfo() != null;
        if (grillWasVisible == grillIsVisible) {
            return;
        }

        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
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
        return fuelState.canAcceptFuel(stack, level);
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
        FirewoodRenderOffset renderOffset = getFirewoodRenderOffset();
        spawnAshDrops(
                worldPosition,
                worldPosition.getX() + 0.5D + renderOffset.x(),
                worldPosition.getY() + ASH_DROP_Y,
                worldPosition.getZ() + 0.5D + renderOffset.z()
        );
    }

    private void dropAshResults(BlockPos dropPos) {
        spawnAshDrops(dropPos, dropPos.getX() + 0.5D, dropPos.getY() + ASH_DROP_Y, dropPos.getZ() + 0.5D);
    }

    private void spawnAshDrops(BlockPos dropPos, double x, double y, double z) {
        if (level == null) {
            return;
        }

        RandomSource random = level.getRandom();
        IroriAshDropContext context = new IroriAshDropContext(level, dropPos, random);
        for (ItemStack drop : IroriApi.getAshDrops(context)) {
            ItemEntity itemEntity = new ItemEntity(level, x, y, z, drop);
            itemEntity.setDeltaMovement(
                    random.triangle(0.0D, 0.035D),
                    0.04D,
                    random.triangle(0.0D, 0.035D)
            );
            level.addFreshEntity(itemEntity);
        }
    }

    private boolean isValidCookingPosition(BlockPos cookingPos) {
        if (level == null) {
            return false;
        }
        Set<BlockPos> component = IroriComponentTopology.collectConnectedComponent(level, worldPosition);
        return IroriComponentTopology.centerPositions(component, worldPosition).contains(cookingPos);
    }

    private void finishCooking(ServerLevel level, List<BlockPos> completedPositions) {
        for (BlockPos completedPos : completedPositions) {
            double x = completedPos.getX() + 0.5D;
            double y = completedPos.getY() + 0.82D;
            double z = completedPos.getZ() + 0.5D;
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, 4, 0.15D, 0.04D, 0.15D, 0.01D);
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

    private void dropCookingContents(boolean atFallbackPosition, BlockPos fallbackPos) {
        if (level == null) {
            return;
        }
        dropPlacedItems(level, cookingState.takeAll(), atFallbackPosition, fallbackPos);
    }

    private static void dropPlacedItems(
            Level level,
            List<IroriCookingState.PlacedItem> items,
            boolean atFallbackPosition,
            BlockPos fallbackPos
    ) {
        for (IroriCookingState.PlacedItem item : items) {
            BlockPos dropPos = atFallbackPosition ? fallbackPos : item.position();
            Containers.dropItemStack(
                    level,
                    dropPos.getX() + 0.5D,
                    dropPos.getY() + 0.8D,
                    dropPos.getZ() + 0.5D,
                    item.stack()
            );
        }
    }

    private void resetStoredState() {
        fuelState.reset();
        cookingState.reset();
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

    private static IroriFuelState.Snapshot resolveComponentFuelState(Level level, Set<BlockPos> component, BlockPos electedMasterPos) {
        BlockEntity elected = level.getBlockEntity(electedMasterPos);
        if (elected instanceof IroriBlockEntity irori) {
            IroriFuelState.Snapshot electedState = irori.fuelState.snapshot();
            if (!electedState.isEmpty()) {
                return electedState;
            }
        }

        for (BlockPos pos : component) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IroriBlockEntity irori && irori.isValidMaster()) {
                IroriFuelState.Snapshot state = irori.fuelState.snapshot();
                if (!state.isEmpty()) {
                    return state;
                }
            }
        }

        for (BlockPos pos : component) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IroriBlockEntity irori) {
                IroriFuelState.Snapshot state = irori.fuelState.snapshot();
                if (!state.isEmpty()) {
                    return state;
                }
            }
        }

        return IroriFuelState.Snapshot.EMPTY;
    }

    private static IroriCookingState.Snapshot resolveComponentCookingState(
            Level level,
            Set<BlockPos> component,
            BlockPos electedMasterPos
    ) {
        Map<BlockPos, IroriCookingState.SlotSnapshot> merged = new LinkedHashMap<>();
        addCookingSnapshot(level.getBlockEntity(electedMasterPos), merged);
        for (BlockPos pos : component) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IroriBlockEntity irori && irori.isValidMaster()) {
                addCookingSnapshot(irori, merged);
            }
        }
        for (BlockPos pos : component) {
            addCookingSnapshot(level.getBlockEntity(pos), merged);
        }
        return merged.isEmpty()
                ? IroriCookingState.Snapshot.EMPTY
                : new IroriCookingState.Snapshot(List.copyOf(merged.values()));
    }

    private static void addCookingSnapshot(
            @Nullable BlockEntity blockEntity,
            Map<BlockPos, IroriCookingState.SlotSnapshot> merged
    ) {
        if (!(blockEntity instanceof IroriBlockEntity irori)) {
            return;
        }
        for (IroriCookingState.SlotSnapshot slot : irori.cookingState.snapshot().slots()) {
            merged.putIfAbsent(slot.position(), slot);
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

    public record FirewoodRenderOffset(double x, double z) {
    }

    public record GrillRenderInfo(GrillModel model, double offsetX, double offsetZ, boolean rotated) {
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

    public record CookingRenderItem(ItemStack stack, double offsetX, double offsetZ, long seed) {
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
