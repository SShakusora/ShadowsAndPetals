package com.sshakusora.shadowsandpetals.compat.transfer.transaction;

public interface TransactionContext {
    /**
     * Records an undo action for a mutation performed inside this transaction.
     *
     * <p>The default implementation deliberately does nothing so callers can
     * continue to pass {@code null} or a lightweight context when they do not
     * need rollback semantics.</p>
     */
    default void addRollback(Runnable action) {
    }
}