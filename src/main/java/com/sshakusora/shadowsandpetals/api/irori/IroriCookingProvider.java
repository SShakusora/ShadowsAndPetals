package com.sshakusora.shadowsandpetals.api.irori;

import java.util.Optional;

/**
 * Resolves a server-side cooking process for an item placed on an Irori grill.
 *
 * <p>Return an empty result when this provider does not handle the input. Providers are evaluated
 * by descending priority, then registration order. Implementations must not mutate the context.
 */
@FunctionalInterface
public interface IroriCookingProvider {
    Optional<IroriCookingProcess> getProcess(IroriCookingContext context);
}
