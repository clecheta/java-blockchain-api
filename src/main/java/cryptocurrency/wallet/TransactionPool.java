package cryptocurrency.wallet;

import com.R07r35cInc.Block;

import java.math.BigDecimal;
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

    /** Filter transactions in the poll in order to get only valid transactions
     * based on the static method Transaction.isValidTransaction()  */
    public List<Transaction> validTransactions() {
        List<Transaction> transactions = transactionMap.values().stream()
                .filter(transaction -> Transaction.isValidTransaction(transaction))
                .collect(Collectors.toList());
       return transactions;
    }

    public void clearBlockchainTransactions(List<Block> chainBlockList) {
        for (int i=0; i<chainBlockList.size(); i++) {
            Block block = chainBlockList.get(i);
            try {
                List<Transaction> transactionList = (List<Transaction>) block.getData();
                for (Transaction transaction: transactionList) {
                    if (this.transactionMap.containsKey(transaction.id.toString())) {
                        this.transactionMap.remove(transaction.id.toString());
                    }
                }
            } catch (ClassCastException cce) {
                if (block.getData()!=Block.genesis().getData()) {
                    throw new RuntimeException("error cleaning blockchain transactions from the pool");
                }
            }
        }
    }
}
