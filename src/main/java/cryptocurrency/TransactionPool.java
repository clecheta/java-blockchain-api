package cryptocurrency;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
public class TransactionPool {

    private Map<UUID, Transaction> transactionMap;

    public TransactionPool() {
        this.transactionMap = new HashMap<UUID, Transaction>();
    }

    public void clear() {
        this.transactionMap = new HashMap<UUID, Transaction>();
    }

    public void setTransaction(Transaction transaction) {
        this.transactionMap.put(transaction.getId(), transaction);
    }

    public void setMap(Map<UUID, Transaction> transactionMap) {
        this.transactionMap = transactionMap;
    }

    public Map<UUID, Transaction> getTransactionMap() {
        return transactionMap;
    }
}
