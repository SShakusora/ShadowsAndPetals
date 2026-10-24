package com.sshakusora.shadowsandpetals.gametest;

import com.sshakusora.shadowsandpetals.block.decoration.BoxBlock;
import com.sshakusora.shadowsandpetals.blockentity.BoxBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import com.sshakusora.shadowsandpetals.util.BoxContents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Server-side game tests for the box merge/drop round trip.
 */
@GameTestHolder("shadowsandpetals")
@PrefixGameTestTemplate(false)
public final class BoxGameTests {
    private static final BlockPos FLOOR_POS = new BlockPos(1, 0, 1);
    private static final BlockPos BOX_POS = new BlockPos(1, 1, 1);
    private static final String BOX_ID = "shadowsandpetals:box";
    private static final String BEEF_ID = "minecraft:beef";
    private static final String COD_ID = "minecraft:cod";

    private BoxGameTests() {
    }

    @GameTest(template = "box_merge", timeoutTicks = 200)
    public static void boxMergePreservesContents(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(FLOOR_POS, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        BlockPos absBox = helper.absolutePos(BOX_POS);

        // 1. Place a bottom box carrying beef through the real BlockItem placement path.
        InteractionResult beefPlacement = placeOnFloor(helper, level, player, boxWithContent(BEEF_ID));
        helper.assertTrue(beefPlacement.consumesAction(), "beef box placement should consume the action, got " + beefPlacement);

        BlockState bottomState = level.getBlockState(absBox);
        helper.assertTrue(bottomState.is(BlockRegistry.BOX.get()), "expected box after first placement, got " + bottomState);
        helper.assertTrue(bottomState.getValue(BoxBlock.TYPE) == SlabType.BOTTOM,
                "expected BOTTOM after first placement, got " + bottomState.getValue(BoxBlock.TYPE));
        BoxBlockEntity bottomEntity = requireBoxEntity(helper, level, absBox);
        assertContent(helper, "bottom half before merge", bottomEntity.contentStack(), BEEF_ID);

        // 2. Merge a second box carrying cod onto the bottom half.
        InteractionResult codPlacement = mergeOntoExistingBox(helper, level, player, boxWithContent(COD_ID));
        helper.assertTrue(codPlacement.consumesAction(), "cod box merge should consume the action, got " + codPlacement);

        BlockState doubleState = level.getBlockState(absBox);
        helper.assertTrue(doubleState.is(BlockRegistry.BOX.get()), "expected box after merge, got " + doubleState);
        helper.assertTrue(doubleState.getValue(BoxBlock.TYPE) == SlabType.DOUBLE,
                "expected DOUBLE after merge, got " + doubleState.getValue(BoxBlock.TYPE));
        BoxBlockEntity doubleEntity = requireBoxEntity(helper, level, absBox);
        assertContent(helper, "merged bottom half", doubleEntity.contentStack(), BEEF_ID);
        assertContent(helper, "merged top half", doubleEntity.topContentStack(), COD_ID);

        // 3. Resolve the real loot table drops and verify the carried components.
        List<ItemStack> drops = Block.getDrops(doubleState, level, absBox, doubleEntity, player, ItemStack.EMPTY);
        helper.assertTrue(drops.size() == 1, "expected exactly one box drop, got " + drops.size() + ": " + drops);
        ItemStack drop = drops.get(0);
        helper.assertTrue(drop.getCount() == 1, "expected one item in the drop stack, got " + drop.getCount());
        helper.assertTrue(BOX_ID.equals(itemId(drop)), "expected drop item " + BOX_ID + ", got " + itemId(drop));
        helper.assertTrue(BoxContents.isDoubleItem(drop), "expected the dropped box to serialize as a double box");
        assertContent(helper, "dropped bottom half", BoxContents.contentStack(drop), BEEF_ID);
        assertContent(helper, "dropped top half", BoxContents.topContentStack(drop), COD_ID);

        // 4. Remove the block and place the dropped stack again; it must come back as DOUBLE.
        helper.destroyBlock(BOX_POS);
        helper.assertTrue(level.getBlockState(absBox).isAir(), "expected air after removing the merged box");
        InteractionResult rePlacement = placeOnFloor(helper, level, player, drop);
        helper.assertTrue(rePlacement.consumesAction(), "double box re-placement should consume the action, got " + rePlacement);

        BlockState replacedState = level.getBlockState(absBox);
        helper.assertTrue(replacedState.is(BlockRegistry.BOX.get()), "expected box after re-placement, got " + replacedState);
        helper.assertTrue(replacedState.getValue(BoxBlock.TYPE) == SlabType.DOUBLE,
                "expected DOUBLE after re-placement, got " + replacedState.getValue(BoxBlock.TYPE));
        BoxBlockEntity replacedEntity = requireBoxEntity(helper, level, absBox);
        assertContent(helper, "re-placed bottom half", replacedEntity.contentStack(), BEEF_ID);
        assertContent(helper, "re-placed top half", replacedEntity.topContentStack(), COD_ID);

        // 5. State definition and horizontal transforms.
        Block box = BlockRegistry.BOX.get();
        helper.assertTrue(box.getStateDefinition().getPossibleStates().size() == 12,
                "expected 12 box states, got " + box.getStateDefinition().getPossibleStates().size());
        BlockState sample = box.defaultBlockState()
                .setValue(BoxBlock.TYPE, SlabType.BOTTOM)
                .setValue(BoxBlock.FACING, Direction.NORTH);

        BlockState rotated = sample.rotate(Rotation.CLOCKWISE_90);
        helper.assertTrue(rotated.getValue(BoxBlock.FACING) == Direction.EAST,
                "CLOCKWISE_90 should turn NORTH into EAST, got " + rotated.getValue(BoxBlock.FACING));
        helper.assertTrue(rotated.getValue(BoxBlock.TYPE) == SlabType.BOTTOM,
                "rotation must not change TYPE, got " + rotated.getValue(BoxBlock.TYPE));

        BlockState leftRight = sample.mirror(Mirror.LEFT_RIGHT);
        helper.assertTrue(leftRight.getValue(BoxBlock.FACING) == Direction.SOUTH,
                "LEFT_RIGHT should turn NORTH into SOUTH, got " + leftRight.getValue(BoxBlock.FACING));
        BlockState frontBack = sample.mirror(Mirror.FRONT_BACK);
        helper.assertTrue(frontBack.getValue(BoxBlock.FACING) == Direction.NORTH,
                "FRONT_BACK should keep NORTH, got " + frontBack.getValue(BoxBlock.FACING));

        helper.succeed();
    }

    private static ItemStack boxWithContent(String contentId) {
        ItemStack stack = new ItemStack(BlockRegistry.BOX.get().asItem());
        CompoundTag tag = new CompoundTag();
        tag.putString(BoxContents.CONTENT_TAG, contentId);
        BlockItem.setBlockEntityData(stack, BlockEntityRegistry.BOX.get(), tag);
        return stack;
    }

    private static InteractionResult placeOnFloor(GameTestHelper helper, ServerLevel level, Player player, ItemStack stack) {
        BlockPos absFloor = helper.absolutePos(FLOOR_POS);
        BlockHitResult hit = new BlockHitResult(
                new Vec3(absFloor.getX() + 0.5D, absFloor.getY() + 1.0D, absFloor.getZ() + 0.5D),
                Direction.UP,
                absFloor,
                false
        );
        return stack.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, hit));
    }

    private static InteractionResult mergeOntoExistingBox(GameTestHelper helper, ServerLevel level, Player player, ItemStack stack) {
        BlockPos absBox = helper.absolutePos(BOX_POS);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absBox), Direction.UP, absBox, false);
        return stack.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, hit));
    }

    private static BoxBlockEntity requireBoxEntity(GameTestHelper helper, ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof BoxBlockEntity,
                "expected BoxBlockEntity at " + pos + ", got " + blockEntity);
        return (BoxBlockEntity) blockEntity;
    }

    private static void assertContent(GameTestHelper helper, String label, ItemStack stack, String expectedId) {
        helper.assertTrue(stack.getCount() == 1, label + " should hold exactly one item, got " + stack.getCount());
        helper.assertTrue(expectedId.equals(itemId(stack)),
                label + " expected " + expectedId + ", got " + itemId(stack));
    }

    private static String itemId(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.toString();
    }
}
