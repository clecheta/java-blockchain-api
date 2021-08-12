package cryptocurrency.wallet;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
public class TransactionPool {

    private Map<String, Transaction> transactionMap;

    public TransactionPool() {
        this.transactionMap = new HashMap<String, Transaction>();
    }

    public void clear() {
        this.transactionMap = new HashMap<String, Transaction>();
    }

    public void setTransaction(Transaction transaction) {
        this.transactionMap.put(transaction.id.toString(), transaction);
    }

    public void setMap(Map<String, Transaction> transactionMap) {
        this.transactionMap = transactionMap;
    }

    public Transaction existingTransaction(String inputAddress) {
        List<Transaction> transactions = transactionMap.values().stream()
                .filter(transaction -> transactionMap.values().stream()
                        .anyMatch(t -> t.input.address.equals(inputAddress)))
                .collect(Collectors.toList());

        if (transactions.size()>0) {
            return transactions.get(0);
        }
        return null;
    }

    public Map<String, Transaction> getTransactionMap() {
        return transactionMap;
    }
}
