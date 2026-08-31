package com.sshakusora.shadowsandpetals.client.renderer;

import com.sshakusora.shadowsandpetals.blockentity.BonsaiBlockEntity;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BonsaiPartCacheKeyTest {
    private static final Identifier OAK = Identifier.withDefaultNamespace("oak_log");
    private static final Identifier MAPLE = Identifier.withDefaultNamespace("maple_log");

    @Test
    void sameStateProducesEqualKeys() {
        BonsaiPartCacheKey a = new BonsaiPartCacheKey(
                BonsaiBlockEntity.Shape.SEMI_CASCADE, false, OAK, Identifier.withDefaultNamespace("oak_leaves"));
        BonsaiPartCacheKey b = new BonsaiPartCacheKey(
                BonsaiBlockEntity.Shape.SEMI_CASCADE, false, OAK, Identifier.withDefaultNamespace("oak_leaves"));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentShapeProducesDifferentKeys() {
        BonsaiPartCacheKey a = new BonsaiPartCacheKey(
                BonsaiBlockEntity.Shape.SEMI_CASCADE, false, OAK, Identifier.withDefaultNamespace("oak_leaves"));
        BonsaiPartCacheKey b = new BonsaiPartCacheKey(
                BonsaiBlockEntity.Shape.SLANTING, false, OAK, Identifier.withDefaultNamespace("oak_leaves"));
        assertNotEquals(a, b);
    }

    @Test
    void deadFlagSeparatesKeys() {
        BonsaiPartCacheKey a = new BonsaiPartCacheKey(
                BonsaiBlockEntity.Shape.SEMI_CASCADE, false, OAK, Identifier.withDefaultNamespace("oak_leaves"));
        BonsaiPartCacheKey b = new BonsaiPartCacheKey(
                BonsaiBlockEntity.Shape.SEMI_CASCADE, true, OAK, Identifier.withDefaultNamespace("oak_leaves"));
        assertNotEquals(a, b);
    }

    @Test
    void differentTrunkProducesDifferentKeys() {
        BonsaiPartCacheKey a = new BonsaiPartCacheKey(
                BonsaiBlockEntity.Shape.SEMI_CASCADE, false, OAK, Identifier.withDefaultNamespace("oak_leaves"));
        BonsaiPartCacheKey b = new BonsaiPartCacheKey(
                BonsaiBlockEntity.Shape.SEMI_CASCADE, false, MAPLE, Identifier.withDefaultNamespace("oak_leaves"));
        assertNotEquals(a, b);
    }

    @Test
    void differentLeavesProduceDifferentKeys() {
        BonsaiPartCacheKey a = new BonsaiPartCacheKey(
                BonsaiBlockEntity.Shape.SEMI_CASCADE, false, OAK, Identifier.withDefaultNamespace("oak_leaves"));
        BonsaiPartCacheKey b = new BonsaiPartCacheKey(
                BonsaiBlockEntity.Shape.SEMI_CASCADE, false, OAK, Identifier.withDefaultNamespace("maple_leaves"));
        assertNotEquals(a, b);
    }

    @Test
    void nullLeavesHandled() {
        BonsaiPartCacheKey a = new BonsaiPartCacheKey(BonsaiBlockEntity.Shape.SEMI_CASCADE, true, OAK, null);
        BonsaiPartCacheKey b = new BonsaiPartCacheKey(BonsaiBlockEntity.Shape.SEMI_CASCADE, true, OAK, null);
        assertEquals(a, b);
    }

    @Test
    void deadTreesIgnoreLeavesInCacheKey() {
        BonsaiPartCacheKey oak = new BonsaiPartCacheKey(
                BonsaiBlockEntity.Shape.TWIN, true, OAK, Identifier.withDefaultNamespace("oak_leaves"));
        BonsaiPartCacheKey maple = new BonsaiPartCacheKey(
                BonsaiBlockEntity.Shape.TWIN, true, OAK, Identifier.withDefaultNamespace("maple_leaves"));
        assertEquals(oak, maple);
    }

    @Test
    void emptyStateDropsTreeIdentifiers() {
        BonsaiPartCacheKey key = BonsaiPartCacheKey.forState(
                BonsaiBlockEntity.Shape.SEMI_CASCADE,
                false,
                false,
                OAK,
                Identifier.withDefaultNamespace("oak_leaves"));
        assertNull(key.trunkBlockId());
        assertNull(key.leavesBlockId());
    }
}
