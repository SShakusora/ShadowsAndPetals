package com.sshakusora.shadowsandpetals.api.irori;

/**
 * Determines whether one piece of content requires the Irori grill.
 *
 * <p>Implementations must be side-safe, read-only, and deterministic for the supplied view. The
 * rule can be evaluated during client section building as well as normal render-state extraction.
 */
@FunctionalInterface
public interface IroriGrillRule {
    GrillRequirement evaluate(IroriView irori, IroriContent content);
}
