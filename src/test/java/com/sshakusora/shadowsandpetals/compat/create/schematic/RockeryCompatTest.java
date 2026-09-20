package com.sshakusora.shadowsandpetals.compat.create.schematic;

import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RockeryCompatTest {
    @Test
    void eachSavedPartContributesOneStoneToTheCompleteStructure() {
        List<RockeryDimensions> dimensions = List.of(
                new RockeryDimensions(1, 1, 1),
                new RockeryDimensions(1, 1, 2),
                new RockeryDimensions(1, 2, 1),
                new RockeryDimensions(1, 2, 2),
                new RockeryDimensions(1, 3, 1)
        );

        assertEquals(List.of(1, 2, 2, 4, 3),
                dimensions.stream().map(RockeryCompat::requiredStoneCount).toList());
    }
}
