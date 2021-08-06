package cryptocurrency;

import java.util.*;
import java.util.stream.Collectors;

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

    public Transaction existingTransaction(String inputAddress) {
        List<Transaction> transactions = transactionMap.values().stream()
                .filter(transaction -> transactionMap.values().stream()
                        .anyMatch(t -> t.getInput().address.equals(inputAddress)))
                .collect(Collectors.toList());

        if (transactions.size()>0) {
            return transactions.get(0);
        }
        return null;
    }

    public Map<UUID, Transaction> getTransactionMap() {
        return transactionMap;
    }
}
