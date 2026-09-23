package com.sshakusora.shadowsandpetals.block.decoration.sofa;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SofaBlockTest {
    private static final BlockPos ORIGIN = BlockPos.ZERO;

    @Test
    void modelPortsContainTheBackDirection() {
        assertEquals(
                Set.of(new SofaPort(Direction.EAST, Direction.SOUTH)),
                SofaConnectionGeometry.worldPorts(
                        Direction.NORTH, SofaBlock.SofaShape.LEFT_EDGE));
        assertEquals(
                Set.of(new SofaPort(Direction.NORTH, Direction.WEST),
                        new SofaPort(Direction.EAST, Direction.SOUTH)),
                SofaConnectionGeometry.worldPorts(
                        Direction.NORTH, SofaBlock.SofaShape.INNER_RIGHT));
        assertEquals(
                Set.of(new SofaPort(Direction.WEST, Direction.SOUTH),
                        new SofaPort(Direction.SOUTH, Direction.WEST)),
                SofaConnectionGeometry.worldPorts(
                        Direction.NORTH, SofaBlock.SofaShape.OUTER_RIGHT));
    }

    @Test
    void facingRotatesBothPortAttributes() {
        assertEquals(
                Set.of(new SofaPort(Direction.SOUTH, Direction.WEST)),
                SofaConnectionGeometry.worldPorts(
                        Direction.EAST, SofaBlock.SofaShape.LEFT_EDGE));
        assertEquals(
                Set.of(new SofaPort(Direction.WEST, Direction.NORTH)),
                SofaConnectionGeometry.worldPorts(
                        Direction.SOUTH, SofaBlock.SofaShape.LEFT_EDGE));
        assertEquals(
                Set.of(new SofaPort(Direction.NORTH, Direction.EAST)),
                SofaConnectionGeometry.worldPorts(
                        Direction.WEST, SofaBlock.SofaShape.LEFT_EDGE));
    }

    @Test
    void everyShapeRoundTripsThroughEveryFacing() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (SofaBlock.SofaShape shape : SofaBlock.SofaShape.values()) {
                assertEquals(
                        shape,
                        SofaConnectionGeometry.shapeForPorts(
                                facing,
                                SofaConnectionGeometry.worldPorts(facing, shape)),
                        facing + " / " + shape);
            }
        }
    }

    @Test
    void portsMatchOnlyWhenFaceIsOppositeAndBackIsEqual() {
        assertTrue(SofaConnectionGeometry.matches(
                ORIGIN,
                new SofaPort(Direction.EAST, Direction.SOUTH),
                ORIGIN.east(),
                new SofaPort(Direction.WEST, Direction.SOUTH)));
        assertFalse(SofaConnectionGeometry.matches(
                ORIGIN,
                new SofaPort(Direction.EAST, Direction.SOUTH),
                ORIGIN.east(),
                new SofaPort(Direction.WEST, Direction.NORTH)));
        assertFalse(SofaConnectionGeometry.matches(
                ORIGIN,
                new SofaPort(Direction.EAST, Direction.SOUTH),
                ORIGIN.north(),
                new SofaPort(Direction.SOUTH, Direction.SOUTH)));
    }

    @Test
    void placementConnectsACompatibleStraightNeighbour() {
        Map<BlockPos, SofaConnectionPlanner.SofaState> layout = new HashMap<>();
        put(layout, ORIGIN, Direction.NORTH, SofaBlock.SofaShape.SINGLE);
        put(layout, ORIGIN.east(), Direction.NORTH, SofaBlock.SofaShape.SINGLE);

        Map<BlockPos, SofaBlock.SofaShape> plan = SofaConnectionPlanner.planPlacement(
                layout::get, ORIGIN);

        assertEquals(SofaBlock.SofaShape.LEFT_EDGE, plan.get(ORIGIN));
        assertEquals(SofaBlock.SofaShape.RIGHT_EDGE, plan.get(ORIGIN.east()));
        assertClosed(layout, plan);
    }

    @Test
    void placementUsesBothCompatibleSidesForCenter() {
        Map<BlockPos, SofaConnectionPlanner.SofaState> layout = new HashMap<>();
        put(layout, ORIGIN, Direction.NORTH, SofaBlock.SofaShape.SINGLE);
        put(layout, ORIGIN.east(), Direction.NORTH, SofaBlock.SofaShape.SINGLE);
        put(layout, ORIGIN.west(), Direction.NORTH, SofaBlock.SofaShape.SINGLE);

        Map<BlockPos, SofaBlock.SofaShape> plan = SofaConnectionPlanner.planPlacement(
                layout::get, ORIGIN);

        assertEquals(SofaBlock.SofaShape.CENTER, plan.get(ORIGIN));
        assertEquals(SofaBlock.SofaShape.RIGHT_EDGE, plan.get(ORIGIN.east()));
        assertEquals(SofaBlock.SofaShape.LEFT_EDGE, plan.get(ORIGIN.west()));
        assertClosed(layout, plan);
    }

    @Test
    void placementCanBuildAClosedInnerCornerWithAnOuterCornerPartner() {
        Map<BlockPos, SofaConnectionPlanner.SofaState> layout = new HashMap<>();
        BlockPos north = ORIGIN.north();
        BlockPos northWest = north.west();
        put(layout, ORIGIN, Direction.NORTH, SofaBlock.SofaShape.SINGLE);
        put(layout, ORIGIN.east(), Direction.NORTH, SofaBlock.SofaShape.SINGLE);
        put(layout, north, Direction.NORTH, SofaBlock.SofaShape.OUTER_RIGHT);
        put(layout, northWest, Direction.NORTH, SofaBlock.SofaShape.LEFT_EDGE);

        Map<BlockPos, SofaBlock.SofaShape> plan = SofaConnectionPlanner.planPlacement(
                layout::get, ORIGIN);

        assertEquals(SofaBlock.SofaShape.INNER_RIGHT, plan.get(ORIGIN), plan.toString());
        assertEquals(SofaBlock.SofaShape.OUTER_RIGHT, plan.get(north));
        assertEquals(SofaBlock.SofaShape.LEFT_EDGE, plan.get(northWest));
        assertEquals(SofaBlock.SofaShape.RIGHT_EDGE, plan.get(ORIGIN.east()));
        assertClosed(layout, plan);
    }

    @Test
    void placementPreservesAnExistingConnectionWhileExtendingIt() {
        Map<BlockPos, SofaConnectionPlanner.SofaState> layout = new HashMap<>();
        put(layout, ORIGIN.west(), Direction.NORTH, SofaBlock.SofaShape.LEFT_EDGE);
        put(layout, ORIGIN, Direction.NORTH, SofaBlock.SofaShape.RIGHT_EDGE);
        put(layout, ORIGIN.east(), Direction.NORTH, SofaBlock.SofaShape.SINGLE);

        Map<BlockPos, SofaBlock.SofaShape> plan = SofaConnectionPlanner.planPlacement(
                layout::get, ORIGIN.east());

        assertEquals(SofaBlock.SofaShape.LEFT_EDGE, plan.get(ORIGIN.west()));
        assertEquals(SofaBlock.SofaShape.CENTER, plan.get(ORIGIN));
        assertEquals(SofaBlock.SofaShape.RIGHT_EDGE, plan.get(ORIGIN.east()));
        assertClosed(layout, plan);
    }

    @Test
    void isolatedPlacementFallsBackToSingleWithoutReturningFailure() {
        Map<BlockPos, SofaConnectionPlanner.SofaState> layout = new HashMap<>();
        put(layout, ORIGIN, Direction.NORTH, SofaBlock.SofaShape.SINGLE);

        Map<BlockPos, SofaBlock.SofaShape> plan = SofaConnectionPlanner.planPlacement(
                layout::get, ORIGIN);

        assertNotNull(plan);
        assertEquals(SofaBlock.SofaShape.SINGLE, plan.get(ORIGIN));
    }

    @Test
    void removalKeepsAConnectionThatDoesNotTouchTheRemovedSection() {
        Map<BlockPos, SofaConnectionPlanner.SofaState> layout = new HashMap<>();
        put(layout, ORIGIN.west(), Direction.NORTH, SofaBlock.SofaShape.LEFT_EDGE);
        put(layout, ORIGIN, Direction.NORTH, SofaBlock.SofaShape.RIGHT_EDGE);

        Map<BlockPos, SofaBlock.SofaShape> plan = SofaConnectionPlanner.planRemoval(
                layout::get, ORIGIN.east());

        assertEquals(SofaBlock.SofaShape.LEFT_EDGE, plan.get(ORIGIN.west()));
        assertEquals(SofaBlock.SofaShape.RIGHT_EDGE, plan.get(ORIGIN));
        assertClosed(layout, plan);
    }

    @Test
    void removalClosesTheRemainingEndpointToSingle() {
        Map<BlockPos, SofaConnectionPlanner.SofaState> layout = new HashMap<>();
        put(layout, ORIGIN, Direction.NORTH, SofaBlock.SofaShape.LEFT_EDGE);

        Map<BlockPos, SofaBlock.SofaShape> plan = SofaConnectionPlanner.planRemoval(
                layout::get, ORIGIN.east());

        assertEquals(SofaBlock.SofaShape.SINGLE, plan.get(ORIGIN));
        assertClosed(layout, plan);
    }

    @Test
    void middleRemovalClosesBothDisconnectedRuns() {
        Map<BlockPos, SofaConnectionPlanner.SofaState> layout = new HashMap<>();
        put(layout, ORIGIN.west(), Direction.NORTH, SofaBlock.SofaShape.LEFT_EDGE);
        put(layout, ORIGIN.east(), Direction.NORTH, SofaBlock.SofaShape.RIGHT_EDGE);

        Map<BlockPos, SofaBlock.SofaShape> plan = SofaConnectionPlanner.planRemoval(
                layout::get, ORIGIN);

        assertEquals(SofaBlock.SofaShape.SINGLE, plan.get(ORIGIN.west()));
        assertEquals(SofaBlock.SofaShape.SINGLE, plan.get(ORIGIN.east()));
        assertClosed(layout, plan);
    }

    @Test
    void plannerNeverChangesFacing() {
        Map<BlockPos, SofaConnectionPlanner.SofaState> layout = new HashMap<>();
        put(layout, ORIGIN, Direction.NORTH, SofaBlock.SofaShape.SINGLE);
        put(layout, ORIGIN.east(), Direction.EAST, SofaBlock.SofaShape.SINGLE);

        Map<BlockPos, SofaBlock.SofaShape> plan = SofaConnectionPlanner.planPlacement(
                layout::get, ORIGIN);

        assertEquals(Direction.NORTH, layout.get(ORIGIN).facing());
        assertEquals(Direction.EAST, layout.get(ORIGIN.east()).facing());
        assertClosed(layout, plan);
    }

    @Test
    void placementIsDeterministicRegardlessOfNeighbourInsertionOrder() {
        Map<BlockPos, SofaConnectionPlanner.SofaState> first = new LinkedHashMap<>();
        put(first, ORIGIN, Direction.NORTH, SofaBlock.SofaShape.SINGLE);
        put(first, ORIGIN.east(), Direction.NORTH, SofaBlock.SofaShape.SINGLE);
        put(first, ORIGIN.west(), Direction.NORTH, SofaBlock.SofaShape.SINGLE);

        Map<BlockPos, SofaConnectionPlanner.SofaState> second = new LinkedHashMap<>();
        put(second, ORIGIN.west(), Direction.NORTH, SofaBlock.SofaShape.SINGLE);
        put(second, ORIGIN.east(), Direction.NORTH, SofaBlock.SofaShape.SINGLE);
        put(second, ORIGIN, Direction.NORTH, SofaBlock.SofaShape.SINGLE);

        assertEquals(
                SofaConnectionPlanner.planPlacement(first::get, ORIGIN),
                SofaConnectionPlanner.planPlacement(second::get, ORIGIN));
    }

    @Test
    void longStraightComponentRepairsInLinearTime() {
        Map<BlockPos, SofaConnectionPlanner.SofaState> layout = new HashMap<>();
        int length = 1_000;
        BlockPos removed = ORIGIN.east(500);
        for (int index = 0; index < length; index++) {
            BlockPos pos = ORIGIN.east(index);
            if (pos.equals(removed)) {
                continue;
            }
            SofaBlock.SofaShape shape = index == 0
                    ? SofaBlock.SofaShape.LEFT_EDGE
                    : index == length - 1
                            ? SofaBlock.SofaShape.RIGHT_EDGE
                            : SofaBlock.SofaShape.CENTER;
            put(layout, pos, Direction.NORTH, shape);
        }

        Map<BlockPos, SofaBlock.SofaShape> plan = SofaConnectionPlanner.planRemoval(
                layout::get, removed);

        assertEquals(SofaBlock.SofaShape.RIGHT_EDGE, plan.get(ORIGIN.east(499)));
        assertEquals(SofaBlock.SofaShape.LEFT_EDGE, plan.get(ORIGIN.east(501)));
        assertClosed(layout, plan);
    }

    @Test
    void generatedBlockstateStillContainsAllFacingAndShapeVariants() throws IOException {
        JsonObject variants = loadBlockstate().getAsJsonObject("variants");

        assertEquals(32, variants.entrySet().size());
        assertVariant(variants, "facing=north,shape=inner_left",
                "shadowsandpetals:block/sofa/brown_inner_corner", 270);
        assertVariant(variants, "facing=north,shape=inner_right",
                "shadowsandpetals:block/sofa/brown_inner_corner", 0);
        assertVariant(variants, "facing=north,shape=outer_left",
                "shadowsandpetals:block/sofa/brown_outer_corner", 0);
        assertVariant(variants, "facing=north,shape=outer_right",
                "shadowsandpetals:block/sofa/brown_outer_corner", 90);
    }

    private static void put(
            Map<BlockPos, SofaConnectionPlanner.SofaState> layout,
            BlockPos pos,
            Direction facing,
            SofaBlock.SofaShape shape
    ) {
        layout.put(pos, new SofaConnectionPlanner.SofaState(facing, shape));
    }

    private static void assertClosed(
            Map<BlockPos, SofaConnectionPlanner.SofaState> layout,
            Map<BlockPos, SofaBlock.SofaShape> shapes
    ) {
        for (Map.Entry<BlockPos, SofaBlock.SofaShape> entry : shapes.entrySet()) {
            BlockPos pos = entry.getKey();
            SofaConnectionPlanner.SofaState state = layout.get(pos);
            if (state == null) {
                continue;
            }
            for (SofaPort port : SofaConnectionGeometry.worldPorts(
                    state.facing(), entry.getValue())) {
                BlockPos neighbourPos = pos.relative(port.face());
                SofaConnectionPlanner.SofaState neighbour = layout.get(neighbourPos);
                assertTrue(neighbour != null, pos + " exposes " + port);
                SofaBlock.SofaShape neighbourShape = shapes.get(neighbourPos);
                assertNotNull(neighbourShape, pos + " has no repaired neighbour");
                assertTrue(
                        SofaConnectionGeometry.worldPorts(
                                        neighbour.facing(), neighbourShape)
                                .stream()
                                .anyMatch(neighbourPort -> SofaConnectionGeometry.matches(
                                        pos, port, neighbourPos, neighbourPort)),
                        pos + " is not matched at " + port);
            }
        }
    }

    private static JsonObject loadBlockstate() throws IOException {
        try (InputStream stream = SofaBlockTest.class.getClassLoader().getResourceAsStream(
                "assets/shadowsandpetals/blockstates/brown_sofa.json")) {
            if (stream == null) {
                throw new IOException("Missing generated brown sofa blockstate");
            }
            return JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static void assertVariant(
            JsonObject variants,
            String key,
            String expectedModel,
            int expectedY
    ) {
        JsonObject variant = variants.getAsJsonObject(key);
        assertEquals(expectedModel, variant.get("model").getAsString(), key);
        assertEquals(expectedY, variant.has("y") ? variant.get("y").getAsInt() : 0, key);
    }
}
