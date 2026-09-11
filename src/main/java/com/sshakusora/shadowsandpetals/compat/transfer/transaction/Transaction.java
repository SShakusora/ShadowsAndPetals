package com.sshakusora.shadowsandpetals.compat.transfer.transaction;

public final class Transaction implements TransactionContext, AutoCloseable {
    private final java.util.List<Runnable> rollbackActions = new java.util.ArrayList<>();
    private boolean committed;
    private boolean closed;
    private Transaction() {}
    public static Transaction openRoot() { return new Transaction(); }
    @Override
    public void addRollback(Runnable action) {
        if (!closed && !committed && action != null) {
            rollbackActions.add(action);
        }
    }
    public void commit() {
        if (!closed) {
            committed = true;
            rollbackActions.clear();
        }
    }
    public boolean isCommitted() { return committed; }
    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (!committed) {
            for (int index = rollbackActions.size() - 1; index >= 0; index--) {
                rollbackActions.get(index).run();
            }
            rollbackActions.clear();
        }
        closed = true;
    }
}