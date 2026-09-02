package com.sshakusora.shadowsandpetals.client.animation;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.client.animation.builder.RegBlockAnimationBuilder;
import com.sshakusora.shadowsandpetals.client.animation.builder.RegUseAnimationBuilder;

/**
 * Client-only entry points for fluent animation registration.
 */
public final class SAPAnimationRegistries {
    private SAPAnimationRegistries() {
    }

    /**
     * Creates a builder for a reusable item-use animation profile.
     *
     * @param name profile path in the Shadows & Petals namespace
     */
    public static RegUseAnimationBuilder useAnimation(String name) {
        return new RegUseAnimationBuilder(ShadowsAndPetals.asResource(name));
    }

    /**
     * Creates a builder for a reusable block-entity animation controller.
     */
    public static RegBlockAnimationBuilder blockAnimation(String name) {
        return new RegBlockAnimationBuilder(ShadowsAndPetals.asResource(name));
    }
}
