package com.sshakusora.shadowsandpetals.block.decoration.bonsai;

import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Plain-JUnit tests for {@link BonsaiAutoTreeResolver}. Registries are not
 * bootstrapped in this environment, so these tests exercise the null-guard
 * paths, the non-tree configuration rejection, and the cache lifecycle; the
 * full registry-driven chain is exercised in-game through the bonsai pot
 * interaction.
 */
class BonsaiAutoTreeResolverTest {
    @Test
    void resolveReturnsNullWithoutRegistries() {
        assertNull(BonsaiAutoTreeResolver.resolve(null, null));
    }
    @Test
    void nonTreeConfigurationsAreRejected() {
        // Any configuration that is not a TreeConfiguration must yield no
        // result so the explicit mapping fallback stays in charge. Vanilla
        // flower features and this mod's PrefabTreeConfiguration both land
        // on this branch.
        assertNull(BonsaiAutoTreeResolver.extractFromConfiguration(FeatureConfiguration.NONE));
    }

    @Test
    void nullConfigurationIsRejected() {
        assertNull(BonsaiAutoTreeResolver.extractFromConfiguration(null));
    }

    @Test
    void cacheIsClearableRepeatedly() {
        BonsaiAutoTreeResolver.invalidateCache();
        BonsaiAutoTreeResolver.invalidateCache();
    }

    @Test
    void resolveIsStableAcrossCacheInvalidations() {
        BonsaiAutoTreeResolver.invalidateCache();
        assertNull(BonsaiAutoTreeResolver.resolve(null, null));
    }

    @Test
    void repeatedResolvesDoNotThrow() {
        assertNull(BonsaiAutoTreeResolver.resolve(null, null));
        assertNull(BonsaiAutoTreeResolver.resolve(null, null));
    }
}