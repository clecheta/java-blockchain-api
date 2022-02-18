package api.cryptocurrency;

import api.BaseTest;
import com.R07r35cInc.cryptocurrency.Transaction;
import com.R07r35cInc.cryptocurrency.TransactionPool;
import com.R07r35cInc.cryptocurrency.Tx;
import com.R07r35cInc.cryptocurrency.Wallet;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@QuarkusTest
public class ValidTransactionsTest extends BaseTest {

    static TransactionPool transactionPool;
    static Transaction transaction;
    static Wallet senderWallet;
    List<Transaction> validTransactions;

    @BeforeAll
    static void init() {
        transactionPool = new TransactionPool();
        senderWallet = new Wallet();
        transaction = new Transaction(
                senderWallet,
                publicKey,
                new BigDecimal(15)
        );
    }

    @BeforeEach
    void setup() {
        validTransactions = new ArrayList<Transaction>();

        // mock valid and invalid transactions
        for (int i = 0; i < 10; i++) {
            transaction = new Transaction(
                    senderWallet,
                    publicKey,
                    new BigDecimal(5)
            );

            //mess up the transactions
            if (i % 3 == 0) { //change amount
                transaction.getInput().amount = tooHighAmount;
            } else if (i % 3 == 1) { //change signature
                transaction.getInput().signature = new Wallet().sign("i" + i);
            } else {
                //if (validTransactions.size()<=1) { // break the test to check validTransactions() implementation
                validTransactions.add(transaction);
                //}
            }

            transactionPool.setTransaction(transaction);
        }
    }

    @Test
    void validTransactions() {
        List<Tx> transactionList = transactionPool.getValidTransactions();
        assertTrue(transactionList.containsAll(validTransactions), "returns valid transaction");
        assertTrue(transactionList.size() == validTransactions.size(),
                "comparison arrays must have the same size");
        assertTrue(validTransactions.containsAll(transactionList), "returns valid transaction");
    }

}
