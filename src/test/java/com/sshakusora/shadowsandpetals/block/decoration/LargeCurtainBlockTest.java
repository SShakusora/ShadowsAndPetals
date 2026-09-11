package com.sshakusora.shadowsandpetals.block.decoration.curtain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LargeCurtainBlockTest {
    @Test
    void everyQuadrantResolvesToTheSameLowerOuterAnchor() {
        BlockPos anchor = new BlockPos(7, 11, -3);
        for (boolean rightSide : new boolean[]{false, true}) {
            Direction inner = LargeCurtainGeometry.innerStep(Direction.NORTH, rightSide);
            BlockPos[] parts = LargeCurtainGeometry.structurePositions(anchor, inner);

            assertEquals(4, Set.copyOf(Arrays.asList(parts)).size());
            assertEquals(anchor, parts[0]);
            assertEquals(anchor.relative(inner), parts[1]);
            assertEquals(anchor.above(), parts[2]);
            assertEquals(anchor.above().relative(inner), parts[3]);

            assertEquals(anchor, LargeCurtainGeometry.anchorOf(parts[0], inner, false, false));
            assertEquals(anchor, LargeCurtainGeometry.anchorOf(parts[1], inner, false, true));
            assertEquals(anchor, LargeCurtainGeometry.anchorOf(parts[2], inner, true, false));
            assertEquals(anchor, LargeCurtainGeometry.anchorOf(parts[3], inner, true, true));
            assertTrue(LargeCurtainGeometry.isAnchorPosition(parts[0], inner, false, false));
            assertFalse(LargeCurtainGeometry.isAnchorPosition(parts[1], inner, false, true));
            assertFalse(LargeCurtainGeometry.isAnchorPosition(parts[2], inner, true, false));
            assertFalse(LargeCurtainGeometry.isAnchorPosition(parts[3], inner, true, true));
        }
    }

    @Test
    void innerColumnDirectionMirrorsTheWindowSide() {
        for (Direction facing : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            assertEquals(facing.getClockWise().getOpposite(), LargeCurtainGeometry.innerStep(facing, true));
            assertEquals(facing.getClockWise(), LargeCurtainGeometry.innerStep(facing, false));
        }
    }

    @Test
    void placementSpaceCheckRequiresAllFourCells() {
        BlockPos anchor = new BlockPos(2, 4, 6);
        Direction inner = Direction.WEST;
        Set<BlockPos> allParts = Set.copyOf(Arrays.asList(
                LargeCurtainGeometry.structurePositions(anchor, inner)
        ));
        assertTrue(LargeCurtainGeometry.allReplaceable(anchor, inner, allParts::contains));

        BlockPos occupied = anchor.above().relative(inner);
        assertFalse(LargeCurtainGeometry.allReplaceable(
                anchor,
                inner,
                position -> !position.equals(occupied)
        ));
    }

    @Test
    void placementPrefersTheUpperClickedRowWhenTheLowerFootprintFits() {
        BlockPos clicked = new BlockPos(2, 8, 6);
        Direction inner = Direction.WEST;
        Set<BlockPos> footprint = Set.copyOf(Arrays.asList(
                LargeCurtainGeometry.structurePositions(clicked.below(), inner)
        ));

        LargeCurtainGeometry.Placement placement = LargeCurtainGeometry.choosePlacement(
                clicked, inner, footprint::contains
        );

        assertNotNull(placement);
        assertEquals(clicked.below(), placement.lowerOuter());
        assertTrue(placement.clickedIsUpper());
    }

    @Test
    void placementFallsBackToTheClickedLowerRowWhenTheLowerFootprintDoesNotFit() {
        BlockPos clicked = new BlockPos(2, 8, 6);
        Direction inner = Direction.WEST;
        Set<BlockPos> footprint = Set.copyOf(Arrays.asList(
                LargeCurtainGeometry.structurePositions(clicked, inner)
        ));

        LargeCurtainGeometry.Placement placement = LargeCurtainGeometry.choosePlacement(
                clicked, inner, footprint::contains
        );

        assertNotNull(placement);
        assertEquals(clicked, placement.lowerOuter());
        assertFalse(placement.clickedIsUpper());
    }

    @Test
    void placementIsRejectedWhenNeitherVerticalCandidateFits() {
        BlockPos clicked = new BlockPos(2, 8, 6);
        assertNull(LargeCurtainGeometry.choosePlacement(
                clicked, Direction.WEST, position -> false
        ));
    }

    @Test
    void neighbouringSidesMirrorUnlessThePlayerSneaks() {
        for (SideCase testCase : new SideCase[]{
                new SideCase(false, true, false, true),
                new SideCase(true, false, false, false),
                new SideCase(true, true, true, true),
                new SideCase(false, false, true, false)
        }) {
            assertEquals(
                    testCase.expected(),
                    LargeCurtainGeometry.sideFromNeighbour(
                            testCase.neighbourIsRight(),
                            testCase.neighbourIsOnObserverLeft(),
                            testCase.sneaking()
                    )
            );
        }
    }

    @Test
    void partnerAndRedstoneDecisionsUseTheWindowPairGeometry() {
        BlockPos anchor = new BlockPos(10, 3, 10);
        assertEquals(anchor.east(), LargeCurtainGeometry.partnerAnchor(anchor, Direction.WEST));
        assertEquals(anchor.west(), LargeCurtainGeometry.partnerAnchor(anchor, Direction.EAST));

        for (boolean requestedOpen : new boolean[]{false, true}) {
            for (boolean localPowered : new boolean[]{false, true}) {
                for (boolean partnerPowered : new boolean[]{false, true}) {
                    assertEquals(
                            requestedOpen || localPowered || partnerPowered,
                            LargeCurtainGeometry.targetOpen(requestedOpen, localPowered, partnerPowered)
                    );
                }
            }
        }
    }

    @Test
    void openQuadrantModelsContainTheExpectedRailAndPileGeometry() throws IOException {
        assertEquals(1, elementCount("static/right/open/white/upper_outer.json"));
        assertEquals(0, elementCount("static/right/open/white/lower_outer.json"));
        assertEquals(18, elementCount("static/right/open/white/upper_inner.json"));
        assertEquals(8, elementCount("static/right/open/white/lower_inner.json"));
        assertEquals(1, elementCount("static/left/open/white/upper_outer.json"));
        assertEquals(0, elementCount("static/left/open/white/lower_outer.json"));
        assertEquals(18, elementCount("static/left/open/white/upper_inner.json"));
        assertEquals(8, elementCount("static/left/open/white/lower_inner.json"));
    }

    @Test
    void openCollisionBoxesMatchTheFourQuadrantRoles() {
        assertEquals(
                new LargeCurtainGeometry.CollisionBox(0, 14, 14, 16, 15, 15),
                LargeCurtainGeometry.openCollisionBox(true, true, false)
        );
        assertNull(LargeCurtainGeometry.openCollisionBox(true, false, false));
        assertEquals(
                new LargeCurtainGeometry.CollisionBox(1, 3, 12, 9, 16, 17),
                LargeCurtainGeometry.openCollisionBox(false, false, false)
        );
        assertEquals(
                new LargeCurtainGeometry.CollisionBox(7, 3, 12, 15, 16, 17),
                LargeCurtainGeometry.openCollisionBox(false, false, true)
        );
        assertEquals(
                new LargeCurtainGeometry.CollisionBox(1, 0, 12, 9, 16, 17),
                LargeCurtainGeometry.openCollisionBox(false, true, false)
        );
    }

    @Test
    void closedCollisionBoxesFollowTheModelDepthAndVerticalHalf() {
        assertEquals(
                new LargeCurtainGeometry.CollisionBox(0, 3, 13, 16, 16, 16),
                LargeCurtainGeometry.closedCollisionBox(false)
        );
        assertEquals(
                new LargeCurtainGeometry.CollisionBox(0, 0, 13, 16, 16, 16),
                LargeCurtainGeometry.closedCollisionBox(true)
        );
    }

    @Test
    void everyDyeColorHasACompleteStateMatrixAndItemModel() throws IOException {
        for (DyeColor color : DyeColor.values()) {
            String id = color.getName() + "_large_curtain";
            JsonObject blockState = loadJson("assets/shadowsandpetals/blockstates/" + id + ".json");
            JsonObject variants = blockState.getAsJsonObject("variants");
            assertEquals(128, variants.entrySet().size(), id);
            variants.entrySet().forEach(entry -> {
                assertTrue(entry.getValue().getAsJsonObject().has("model"), id + ":" + entry.getKey());
                assertTrue(entry.getValue().getAsJsonObject().get("model").getAsString()
                                .startsWith("shadowsandpetals:block/large_curtain/"),
                        id + ":" + entry.getKey());
            });

            JsonObject item = loadJson("assets/shadowsandpetals/items/" + id + ".json");
            JsonObject itemModel = item.getAsJsonObject("model");
            assertEquals("shadowsandpetals:block/large_curtain/item/" + color.getName(),
                    itemModel.get("model").getAsString(), id);

            assertResourceExists("data/shadowsandpetals/loot_table/blocks/" + id + ".json");
        }
    }

    @Test
    void generatedLootOnlyTargetsTheExplicitAnchorState() throws IOException {
        for (DyeColor color : DyeColor.values()) {
            String id = color.getName() + "_large_curtain";
            JsonObject loot = loadJson("data/shadowsandpetals/loot_table/blocks/" + id + ".json");
            JsonObject properties = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                    .getAsJsonArray("conditions").get(0).getAsJsonObject()
                    .getAsJsonObject("properties");
            assertEquals("outer", properties.get("column").getAsString(), id);
            assertEquals("lower", properties.get("half").getAsString(), id);
            assertEquals("true", properties.get("anchor").getAsString(), id);
        }
    }

    @Test
    void generatedStateMappingUsesSideAndQuadrantSpecificModels() throws IOException {
        JsonObject variants = loadJson("assets/shadowsandpetals/blockstates/white_large_curtain.json")
                .getAsJsonObject("variants");
        assertEquals(
                "shadowsandpetals:block/large_curtain/static/left/closed/white/lower_outer",
                variants.getAsJsonObject("animating=false,column=outer,facing=north,half=lower,open=false,side=left")
                        .get("model").getAsString()
        );
        assertEquals(
                "shadowsandpetals:block/large_curtain/static/right/open/white/upper_inner",
                variants.getAsJsonObject("animating=false,column=inner,facing=north,half=upper,open=true,side=right")
                        .get("model").getAsString()
        );
    }

    private static int elementCount(String modelName) throws IOException {
        JsonObject model = loadJson("assets/shadowsandpetals/models/block/large_curtain/" + modelName);
        return model.has("elements") ? model.getAsJsonArray("elements").size() : 0;
    }

    private static JsonObject loadJson(String resourceName) throws IOException {
        try (InputStream stream = getResource(resourceName)) {
            assertNotNull(stream, resourceName);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    private static void assertResourceExists(String resourceName) throws IOException {
        try (InputStream stream = getResource(resourceName)) {
            assertNotNull(stream, resourceName);
        }
    }

    private static InputStream getResource(String resourceName) {
        return LargeCurtainBlockTest.class.getClassLoader().getResourceAsStream(resourceName);
    }

    private record SideCase(
            boolean neighbourIsRight,
            boolean neighbourIsOnObserverLeft,
            boolean sneaking,
            boolean expected
    ) {
    }
}
