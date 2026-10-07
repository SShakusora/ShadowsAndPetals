package com.sshakusora.shadowsandpetals.block.decoration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecessedLampConnectionTest {
    @Test
    void everyNeighbourLayoutMapsToItsFourIntersectionOccupancyMasks() {
        for (int neighbours = 0; neighbours < 256; neighbours++) {
            boolean north = (neighbours & 1) != 0;
            boolean east = (neighbours & 2) != 0;
            boolean south = (neighbours & 4) != 0;
            boolean west = (neighbours & 8) != 0;
            boolean nw = (neighbours & 16) != 0;
            boolean ne = (neighbours & 32) != 0;
            boolean sw = (neighbours & 64) != 0;
            boolean se = (neighbours & 128) != 0;
            var corners = RecessedLampConnection.corners(north, east, south, west, nw, ne, sw, se);
            assertEquals(occupancy(west, true, nw, north), corners.northWest());
            assertEquals(occupancy(true, east, north, ne), corners.northEast());
            assertEquals(occupancy(sw, south, west, true), corners.southWest());
            assertEquals(occupancy(south, se, true, east), corners.southEast());
        }
    }

    @Test
    void isolatedLampUsesFourSingleQuadrantsAndAnInteriorLampUsesFourFullQuadrants() {
        assertEquals(new RecessedLampConnection.Corners(2, 1, 8, 4),
                RecessedLampConnection.corners(false, false, false, false, false, false, false, false));
        assertEquals(new RecessedLampConnection.Corners(15, 15, 15, 15),
                RecessedLampConnection.corners(true, true, true, true, true, true, true, true));
    }

    // Source model bits are ordered south-west, south-east, north-west, north-east.
    private static int occupancy(boolean southWest, boolean southEast, boolean northWest, boolean northEast) {
        boolean[] quadrants = {southWest, southEast, northWest, northEast};
        int mask = 0;
        for (int quadrant = 0; quadrant < quadrants.length; quadrant++) {
            if (quadrants[quadrant]) {
                mask += 1 << quadrant;
            }
        }
        return mask;
    }
}
