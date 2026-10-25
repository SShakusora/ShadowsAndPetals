package com.sshakusora.shadowsandpetals.gametest;

import com.sshakusora.shadowsandpetals.block.decoration.BoxBlock;
import com.sshakusora.shadowsandpetals.block.decoration.BoxItem;
import com.sshakusora.shadowsandpetals.blockentity.BoxBlockEntity;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.registries.BlockRegistry;
import com.sshakusora.shadowsandpetals.registries.CreativeTabRegistry;
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
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    private static final BlockPos WALL_POS = new BlockPos(2, 1, 1);
    private static final String BOX_ID = "shadowsandpetals:box";
    private static final String BEEF_ID = "minecraft:beef";
    private static final String COD_ID = "minecraft:cod";
    private static final String SALMON_ID = "minecraft:salmon";

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

    @GameTest(template = "box_merge", timeoutTicks = 200)
    public static void boxCloneStackMatchesContents(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(FLOOR_POS, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        BlockPos absBox = helper.absolutePos(BOX_POS);

        // 1. A single bottom half keeps its content and must not become a double item.
        InteractionResult beefPlacement = placeOnFloor(helper, level, player, boxWithContent(BEEF_ID));
        helper.assertTrue(beefPlacement.consumesAction(), "beef box placement should consume the action, got " + beefPlacement);
        BlockState bottomState = level.getBlockState(absBox);
        helper.assertTrue(bottomState.getValue(BoxBlock.TYPE) == SlabType.BOTTOM,
                "expected BOTTOM before cloning, got " + bottomState.getValue(BoxBlock.TYPE));

        ItemStack bottomClone = cloneStack(level, absBox, player);
        helper.assertTrue(BOX_ID.equals(itemId(bottomClone)), "expected clone item " + BOX_ID + ", got " + itemId(bottomClone));
        helper.assertTrue(!BoxContents.isDoubleItem(bottomClone), "single bottom clone must not be a double item");
        assertContent(helper, "cloned bottom half", BoxContents.contentStack(bottomClone), BEEF_ID);

        // 2. A merged double box picks the single topmost box, never the stacked item.
        InteractionResult codPlacement = mergeOntoExistingBox(helper, level, player, boxWithContent(COD_ID));
        helper.assertTrue(codPlacement.consumesAction(), "cod box merge should consume the action, got " + codPlacement);
        BlockState doubleState = level.getBlockState(absBox);
        helper.assertTrue(doubleState.getValue(BoxBlock.TYPE) == SlabType.DOUBLE,
                "expected DOUBLE before cloning, got " + doubleState.getValue(BoxBlock.TYPE));

        ItemStack doubleClone = cloneStack(level, absBox, player);
        helper.assertTrue(!BoxContents.isDoubleItem(doubleClone), "picked clone must be a single box, not the stacked item");
        helper.assertTrue(ItemStack.isSameItemSameComponents(doubleClone, boxWithContent(COD_ID)),
                "picked clone must stack with a regular " + COD_ID + " box");
        assertContent(helper, "picked top half of double", BoxContents.contentStack(doubleClone), COD_ID);

        // 3. A single top half placed against the western face of a stone wall picks as a regular box.
        helper.destroyBlock(BOX_POS);
        helper.setBlock(WALL_POS, Blocks.STONE);
        InteractionResult salmonPlacement = placeOnWallWestFace(helper, level, player, boxWithContent(SALMON_ID));
        helper.assertTrue(salmonPlacement.consumesAction(), "salmon top box placement should consume the action, got " + salmonPlacement);

        BlockState topState = level.getBlockState(absBox);
        helper.assertTrue(topState.is(BlockRegistry.BOX.get()), "expected box after wall placement, got " + topState);
        helper.assertTrue(topState.getValue(BoxBlock.TYPE) == SlabType.TOP,
                "expected TOP before cloning, got " + topState.getValue(BoxBlock.TYPE));

        ItemStack topClone = cloneStack(level, absBox, player);
        helper.assertTrue(!BoxContents.isDoubleItem(topClone), "single top clone must not be a double item");
        helper.assertTrue(ItemStack.isSameItemSameComponents(topClone, boxWithContent(SALMON_ID)),
                "single top clone must be a regular box carrying the top content");
        assertContent(helper, "cloned top half", BoxContents.contentStack(topClone), SALMON_ID);

        helper.succeed();
    }

    @GameTest(template = "box_merge", timeoutTicks = 200)
    public static void emptyBoxIsBareBoxAndMerges(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Item boxItem = BlockRegistry.BOX.get().asItem();
        ItemStack emptyBox = new ItemStack(boxItem);
        helper.assertTrue(!BoxContents.isDoubleItem(emptyBox), "the empty box must not be a double item");
        helper.assertTrue(BoxContents.contentStack(emptyBox).isEmpty(), "a bare box must be empty");
        helper.assertTrue(BuiltInRegistries.ITEM.getOptional(
                ResourceLocation.fromNamespaceAndPath("shadowsandpetals", "empty_box")).isEmpty(),
                "the empty_box item registration should be gone");

        // The bare box is the first boxes tab entry and the tab icon.
        CreativeModeTab tab = CreativeTabRegistry.BOXES.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess()));
        List<ItemStack> contents = List.copyOf(tab.getDisplayItems());
        helper.assertTrue(!contents.isEmpty(), "boxes tab should not be empty");
        helper.assertTrue(contents.get(0).is(boxItem), "empty box should be the first boxes tab entry, got " + contents.get(0));
        helper.assertTrue(BoxContents.contentStack(contents.get(0)).isEmpty(), "the first tab entry should be empty");
        helper.assertTrue(tab.getIconItem().is(boxItem), "boxes tab icon should be the box item, got " + tab.getIconItem());
        for (int i = 1; i < contents.size(); i++) {
            helper.assertTrue(contents.get(i).getItem() instanceof BoxItem,
                    "boxes tab entry " + i + " should be a box, got " + contents.get(i));
        }

        // Place a bare box: it is a bottom half with no content and half-block collision.
        helper.setBlock(FLOOR_POS, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        InteractionResult placement = placeOnFloor(helper, level, player, new ItemStack(boxItem));
        helper.assertTrue(placement.consumesAction(), "empty box placement should consume the action, got " + placement);

        BlockPos absBox = helper.absolutePos(BOX_POS);
        BlockState bottom = level.getBlockState(absBox);
        helper.assertTrue(bottom.is(BlockRegistry.BOX.get()), "expected box after placement, got " + bottom);
        helper.assertTrue(bottom.getValue(BoxBlock.TYPE) == SlabType.BOTTOM,
                "expected BOTTOM after empty box placement, got " + bottom.getValue(BoxBlock.TYPE));
        assertEmpty(helper, "placed empty box", requireBoxEntity(helper, level, absBox).contentStack());
        double halfHeight = bottom.getCollisionShape(level, absBox).max(Direction.Axis.Y);
        helper.assertTrue(Math.abs(halfHeight - 0.5D) < 1.0E-6D,
                "empty box collision should be half a block tall, got " + halfHeight);

        // Merging an empty box onto the empty box yields a full box with two empty halves.
        InteractionResult emptyMerge = mergeOntoExistingBox(helper, level, player, new ItemStack(boxItem));
        helper.assertTrue(emptyMerge.consumesAction(), "empty box merge should consume the action, got " + emptyMerge);
        BlockState doubleEmpty = level.getBlockState(absBox);
        helper.assertTrue(doubleEmpty.getValue(BoxBlock.TYPE) == SlabType.DOUBLE,
                "expected DOUBLE after empty merge, got " + doubleEmpty.getValue(BoxBlock.TYPE));
        BoxBlockEntity doubleEmptyEntity = requireBoxEntity(helper, level, absBox);
        assertEmpty(helper, "merged empty bottom half", doubleEmptyEntity.contentStack());
        assertEmpty(helper, "merged empty top half", doubleEmptyEntity.topContentStack());
        double fullHeight = doubleEmpty.getCollisionShape(level, absBox).max(Direction.Axis.Y);
        helper.assertTrue(Math.abs(fullHeight - 1.0D) < 1.0E-6D,
                "a merged empty box should be a full block tall, got " + fullHeight);

        ItemStack emptyTopClone = cloneStack(level, absBox, player);
        helper.assertTrue(!BoxContents.isDoubleItem(emptyTopClone), "picked empty clone must be a single box");
        helper.assertTrue(ItemStack.isSameItemSameComponents(emptyTopClone, new ItemStack(boxItem)),
                "picked empty clone must stack with a regular empty box");
        assertEmpty(helper, "picked empty top half", BoxContents.contentStack(emptyTopClone));

        // The full empty box round-trips through its drop.
        List<ItemStack> drops = Block.getDrops(doubleEmpty, level, absBox, doubleEmptyEntity, player, ItemStack.EMPTY);
        helper.assertTrue(drops.size() == 1, "expected exactly one box drop, got " + drops);
        ItemStack doubleDrop = drops.get(0);
        helper.assertTrue(BoxContents.isDoubleItem(doubleDrop), "dropped empty full box must stay double");
        assertEmpty(helper, "dropped empty bottom half", BoxContents.contentStack(doubleDrop));
        assertEmpty(helper, "dropped empty top half", BoxContents.topContentStack(doubleDrop));

        helper.destroyBlock(BOX_POS);
        InteractionResult doubleRePlacement = placeOnFloor(helper, level, player, doubleDrop);
        helper.assertTrue(doubleRePlacement.consumesAction(), "full empty box re-placement should consume the action, got " + doubleRePlacement);
        helper.assertTrue(level.getBlockState(absBox).getValue(BoxBlock.TYPE) == SlabType.DOUBLE,
                "re-placed full empty box should stay DOUBLE");

        // An empty half merges with a filled half in both orders.
        helper.destroyBlock(BOX_POS);
        placeOnFloor(helper, level, player, new ItemStack(boxItem));
        InteractionResult beefOntoEmpty = mergeOntoExistingBox(helper, level, player, boxWithContent(BEEF_ID));
        helper.assertTrue(beefOntoEmpty.consumesAction(), "beef merge onto empty box should consume the action, got " + beefOntoEmpty);
        BoxBlockEntity emptyThenBeef = requireBoxEntity(helper, level, absBox);
        assertEmpty(helper, "empty bottom half after beef merge", emptyThenBeef.contentStack());
        assertContent(helper, "beef top half after merge", emptyThenBeef.topContentStack(), BEEF_ID);

        helper.destroyBlock(BOX_POS);
        placeOnFloor(helper, level, player, boxWithContent(COD_ID));
        InteractionResult emptyOntoBeef = mergeOntoExistingBox(helper, level, player, new ItemStack(boxItem));
        helper.assertTrue(emptyOntoBeef.consumesAction(), "empty merge onto filled box should consume the action, got " + emptyOntoBeef);
        BoxBlockEntity filledThenEmpty = requireBoxEntity(helper, level, absBox);
        assertContent(helper, "cod bottom half after merge", filledThenEmpty.contentStack(), COD_ID);
        assertEmpty(helper, "empty top half after merge", filledThenEmpty.topContentStack());

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

    private static InteractionResult placeOnWallWestFace(GameTestHelper helper, ServerLevel level, Player player, ItemStack stack) {
        BlockPos absWall = helper.absolutePos(WALL_POS);
        BlockHitResult hit = new BlockHitResult(
                new Vec3(absWall.getX(), absWall.getY() + 0.75D, absWall.getZ() + 0.5D),
                Direction.WEST,
                absWall,
                false
        );
        return stack.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, hit));
    }

    private static ItemStack cloneStack(ServerLevel level, BlockPos absPos, Player player) {
        BlockState state = level.getBlockState(absPos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
        return state.getCloneItemStack(hit, level, absPos, player);
    }

    private static BoxBlockEntity requireBoxEntity(GameTestHelper helper, ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof BoxBlockEntity,
                "expected BoxBlockEntity at " + pos + ", got " + blockEntity);
        return (BoxBlockEntity) blockEntity;
    }

    private static void assertEmpty(GameTestHelper helper, String label, ItemStack stack) {
        helper.assertTrue(stack.isEmpty(), label + " should be empty, got " + stack);
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
