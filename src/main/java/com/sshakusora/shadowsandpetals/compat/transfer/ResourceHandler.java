package com.sshakusora.shadowsandpetals.compat.transfer;

import com.sshakusora.shadowsandpetals.compat.transfer.transaction.TransactionContext;

public interface ResourceHandler<T> {
    int size();
    T getResource(int index);
    long getAmountAsLong(int index);
    long getCapacityAsLong(int index, T resource);
    boolean isValid(int index, T resource);
    int insert(int index, T resource, int amount, TransactionContext transaction);
    int extract(int index, T resource, int amount, TransactionContext transaction);

    default int getAmountAsInt(int index) { return (int) Math.min(Integer.MAX_VALUE, getAmountAsLong(index)); }
    default int getCapacityAsInt(int index, T resource) { return (int) Math.min(Integer.MAX_VALUE, getCapacityAsLong(index, resource)); }
}