package api.cryptocurrency;

import com.R07r35cInc.blockchain.NetChain;
import com.R07r35cInc.wallet.Transaction;
import com.R07r35cInc.wallet.TransactionPool;
import com.R07r35cInc.wallet.Wallet;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@QuarkusTest
public class ClearTransactionPoolTest {

    private static final String anyRecipient = "any-recipient-public-key";

    static TransactionPool transactionPool;
    static Transaction transaction;
    static Wallet senderWallet;

    @BeforeEach
    void init() {
        transactionPool = new TransactionPool();
        senderWallet = new Wallet();
        transaction = new Transaction(
                senderWallet,
                anyRecipient,
                new BigDecimal(15)
        );
    }

    @Test
    void clearAllTransactions() {
        transactionPool.clear();
        assertTrue(transactionPool.getTransactionMap().size() == 0, "clears the transactions");
    }

    @Test
    void clearBlockchainTransactionsOnly() {
        NetChain blockchain = new NetChain();
        Map<String, Transaction> expectedTransactionMap = new HashMap<String, Transaction>();
        for (int i = 0; i < 6; i++) {
            Transaction transaction = new Wallet().createTransaction("foo", new BigDecimal(20));
            transactionPool.setTransaction(transaction);
            if (i % 2 == 0) { //even
                List<Transaction> transactionList = new ArrayList<Transaction>();
                transactionList.add(transaction);
                blockchain.addBlock(transactionList);
            } else { //odd
                expectedTransactionMap.put(transaction.id.toString(), transaction);
            }
        }

        transactionPool.clearBlockchainTransactions(blockchain.getChain());
        assertEquals(expectedTransactionMap, transactionPool.getTransactionMap(),
                "clears the pool of any existing blockchain transactions");
        assertTrue(expectedTransactionMap.size() == transactionPool.getTransactionMap().size(),
                "transaction pool map size must match");
    }

}
