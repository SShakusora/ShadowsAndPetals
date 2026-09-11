package com.sshakusora.shadowsandpetals.compat.transfer;

public final class TransferPreconditions {
    private TransferPreconditions() {}
    public static <T> void checkNonEmptyNonNegative(T resource, int amount) {
        if (resource == null || amount < 0) throw new IllegalArgumentException("Invalid transfer");
        try {
            if (resource.getClass().getMethod("isEmpty").invoke(resource).equals(Boolean.TRUE) && amount > 0)
                throw new IllegalArgumentException("Empty resource");
        } catch (ReflectiveOperationException ignored) {}
    }
}