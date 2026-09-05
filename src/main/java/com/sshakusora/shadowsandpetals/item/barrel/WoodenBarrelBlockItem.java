package com.sshakusora.shadowsandpetals.item.barrel;

import com.sshakusora.shadowsandpetals.blockentity.WoodenBarrelBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/**
 * A placeable wooden barrel item that can also pick up a full source fluid,
 * like the vanilla empty bucket.
 */
public class WoodenBarrelBlockItem extends BlockItem {
    public WoodenBarrelBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.isSecondaryUseActive()
                && WoodenBarrelItemFluid.read(context.getItemInHand()).isPresent()) {
            InteractionResult placement = tryPlaceStoredFluid(
                    context.getLevel(),
                    context.getPlayer(),
                    context.getHand()
            );
            return placement == InteractionResult.PASS ? InteractionResult.FAIL : placement;
        }

        InteractionResult pickup = tryPickupFluid(
                context.getLevel(),
                context.getPlayer(),
                context.getHand()
        );
        if (pickup.consumesAction()) {
            return pickup;
        }

        return super.useOn(context);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()
                && WoodenBarrelItemFluid.read(player.getItemInHand(hand)).isPresent()) {
            return tryPlaceStoredFluid(level, player, hand);
        }

        InteractionResult pickup = tryPickupFluid(level, player, hand);
        return pickup.consumesAction() ? pickup : super.use(level, player, hand);
    }

    private InteractionResult tryPlaceStoredFluid(Level level, Player player, InteractionHand hand) {
        if (player == null) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);
        FluidStack storedFluid = WoodenBarrelItemFluid.read(stack).orElse(FluidStack.EMPTY);
        if (storedFluid.isEmpty() || storedFluid.getAmount() < FluidType.BUCKET_VOLUME) {
            return InteractionResult.FAIL;
        }

        BlockHitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }

        BlockPos clickedPos = hit.getBlockPos();
        Direction side = hit.getDirection();
        BlockPos adjacentPos = clickedPos.relative(side);
        if (!level.mayInteract(player, clickedPos)
                || !player.mayUseItemAt(adjacentPos, side, stack)) {
            return InteractionResult.FAIL;
        }

        FluidResource resource = FluidResource.of(storedFluid);
        BlockState clickedState = level.getBlockState(clickedPos);
        BlockPos destination = clickedState.getBlock() instanceof LiquidBlockContainer container
                && container.canPlaceLiquid(player, level, clickedPos, clickedState, resource.getFluid())
                ? clickedPos
                : adjacentPos;

        ItemStack usedStack = stack.copy();
        boolean placed;
        if (player.hasInfiniteMaterials()) {
            placed = FluidUtil.tryPlaceFluid(resource, player, level, hand, destination);
        } else {
            ItemAccess itemAccess = ItemAccess.forPlayerInteraction(player, hand).oneByOne();
            ResourceHandler<FluidResource> handler = itemAccess.getCapability(Capabilities.Fluid.ITEM);
            placed = handler != null
                    && !FluidUtil.tryPlaceFluid(handler, player, level, hand, destination).isEmpty();
        }

        if (!placed) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            player.awardStat(Stats.ITEM_USED.get(this));
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, destination, usedStack);
            }
        }

        return InteractionResult.SUCCESS.heldItemTransformedTo(player.getItemInHand(hand));
    }

    private InteractionResult tryPickupFluid(Level level, Player player, InteractionHand hand) {
        if (player == null) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || WoodenBarrelItemFluid.read(stack).isPresent()) {
            return InteractionResult.PASS;
        }

        BlockHitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }

        BlockPos sourcePos = hit.getBlockPos();
        Direction side = hit.getDirection();
        if (!level.mayInteract(player, sourcePos)
                || !player.mayUseItemAt(sourcePos.relative(side), side, stack)) {
            return InteractionResult.FAIL;
        }

        ItemAccess itemAccess = ItemAccess.forPlayerInteraction(player, hand).oneByOne();
        ResourceHandler<FluidResource> handler = itemAccess.getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) {
            return InteractionResult.FAIL;
        }

        var pickedUp = FluidUtil.tryPickupFluid(handler, player, level, sourcePos, side);
        if (pickedUp.isEmpty()) {
            // The SOURCE_ONLY ray hit a fluid, but this particular source could
            // not be transferred into a barrel. Match the bucket's no-op result
            // instead of placing a barrel into the source by accident.
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            player.awardStat(Stats.ITEM_USED.get(this));
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.FILLED_BUCKET.trigger(serverPlayer, WoodenBarrelItemFluid.write(
                        new ItemStack(this), pickedUp
                ));
            }
        }

        return InteractionResult.SUCCESS.heldItemTransformedTo(player.getItemInHand(hand));
    }

    public static ItemStack filledWoodenBarrel(Fluid fluid) {
        return WoodenBarrelItemFluid.write(
                new ItemStack(BlockRegistry.WOODEN_BARREL.get()),
                FluidResource.of(fluid).toStack(WoodenBarrelBlockEntity.FLUID_CAPACITY)
        );
    }
}
