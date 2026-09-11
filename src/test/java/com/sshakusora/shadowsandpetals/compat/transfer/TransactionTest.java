package com.sshakusora.shadowsandpetals.compat.transfer;

import com.sshakusora.shadowsandpetals.compat.transfer.transaction.Transaction;
import com.sshakusora.shadowsandpetals.compat.transfer.transaction.TransactionContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionTest {
    @Test
    void mutationsRollBackWhenTransactionIsNotCommitted() {
        TestHandler handler = new TestHandler();

        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(2, handler.extract(0, "water", 2, transaction));
            assertEquals(3, handler.amount);
        }

        assertEquals(5, handler.amount);
    }

    @Test
    void committedMutationsAreKept() {
        TestHandler handler = new TestHandler();

        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(3, handler.insert(0, "water", 3, transaction));
            transaction.commit();
        }

        assertEquals(8, handler.amount);
    }

    private static final class TestHandler implements ResourceHandler<String> {
        private int amount = 5;

        @Override
        public int size() {
            return 1;
        }

        @Override
        public String getResource(int index) {
            return "water";
        }

        @Override
        public long getAmountAsLong(int index) {
            return amount;
        }

        @Override
        public long getCapacityAsLong(int index, String resource) {
            return 10;
        }

        @Override
        public boolean isValid(int index, String resource) {
            return index == 0 && "water".equals(resource);
        }

        @Override
        public int insert(int index, String resource, int requested, TransactionContext transaction) {
            if (!isValid(index, resource) || requested <= 0) {
                return 0;
            }
            int previous = amount;
            int inserted = Math.min(requested, 10 - amount);
            amount += inserted;
            if (transaction != null) {
                transaction.addRollback(() -> amount = previous);
            }
            return inserted;
        }

        @Override
        public int extract(int index, String resource, int requested, TransactionContext transaction) {
            if (!isValid(index, resource) || requested <= 0) {
                return 0;
            }
            int previous = amount;
            int extracted = Math.min(requested, amount);
            amount -= extracted;
            if (transaction != null) {
                transaction.addRollback(() -> amount = previous);
            }
            return extracted;
        }
    }
}
