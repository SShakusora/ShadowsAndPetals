package com.sshakusora.shadowsandpetals.api.irori;

/**
 * Result returned by an {@link IroriGrillRule}.
 *
 * <p>Rules compose monotonically: any {@link #REQUIRE} result makes the grill visible, while
 * {@link #PASS} leaves the decision to the remaining rules. A rule cannot hide a grill requested
 * by another rule.
 */
public enum GrillRequirement {
    PASS,
    REQUIRE
}
