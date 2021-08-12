package cryptocurrency.wallet;

import cryptocurrency.wallet.Transaction;
import cryptocurrency.wallet.TransactionPool;
import cryptocurrency.wallet.Wallet;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@QuarkusTest
public class TransactionPoolTest {

    private static final String recipient = "recipient-foo-public-key";

    TransactionPool transactionPool;
    Transaction transaction;
    Wallet senderWallet;

    @BeforeEach
    void init() {
        transactionPool = new TransactionPool();
        senderWallet = new Wallet();
        transaction = new Transaction(
                senderWallet,
                recipient,
                new BigDecimal(15)
        );
    }

    @Test
    void addTransaction() {
        transactionPool.setTransaction(transaction);
        Transaction expected = transactionPool.getTransactionMap().get(transaction.id.toString());
        assertEquals(expected, transaction, "added a transaction");
    }

    @Test
    void existingTransaction() {
        transactionPool.setTransaction(transaction);
        Transaction expected = transactionPool.existingTransaction(senderWallet.publicKey);
        assertEquals(expected, transaction, "returns an existing transaction given an input address");
    }

}
