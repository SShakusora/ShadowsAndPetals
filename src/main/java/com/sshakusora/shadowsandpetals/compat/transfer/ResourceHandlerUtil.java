package com.sshakusora.shadowsandpetals.compat.transfer;

import com.sshakusora.shadowsandpetals.compat.transfer.transaction.TransactionContext;

public final class ResourceHandlerUtil {
    private ResourceHandlerUtil(){}
    public static <T> int moveFirst(ResourceHandler<T> from,ResourceHandler<T> to,TransactionContext tx){
        for(int i=0;i<from.size();i++){T r=from.getResource(i);int amount=(int)Math.min(Integer.MAX_VALUE,from.getAmountAsLong(i));if(r==null||amount<=0)continue;int moved=to.insert(0,r,amount,tx);if(moved>0){from.extract(i,r,moved,tx);return moved;}}return 0;
    }
    public static <T> int moveFirst(ResourceHandler<T> from, ResourceHandler<T> to, java.util.function.Predicate<T> predicate, int maxAmount, TransactionContext tx) {
        for (int i = 0; i < from.size(); i++) {
            T resource = from.getResource(i);
            if (resource == null || !predicate.test(resource)) continue;
            int available = (int)Math.min(maxAmount, from.getAmountAsLong(i));
            if (available <= 0) continue;
            int moved = to.insert(0, resource, available, tx);
            if (moved > 0) { from.extract(i, resource, moved, tx); return moved; }
        }
        return 0;
    }
}
