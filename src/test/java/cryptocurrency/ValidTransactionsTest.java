package cryptocurrency;

import cryptocurrency.wallet.Transaction;
import cryptocurrency.wallet.TransactionPool;
import cryptocurrency.wallet.Wallet;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@QuarkusTest
public class ValidTransactionsTest {

    private static final String anyRecipient = "any-recipient-public-key";
    BigDecimal tooHighAmount = new BigDecimal(999999);

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
                anyRecipient,
                new BigDecimal(15)
        );
    }

    @BeforeEach
    void setup() {
        validTransactions = new ArrayList<Transaction>();

        // mock valid and invalid transactions
        for (int i=0; i<10; i++) {
            transaction = new Transaction(
                    senderWallet,
                    anyRecipient,
                    new BigDecimal(5)
            );

            //mess up the transactions
            if (i%3==0) { //change amount
                transaction.input.amount = tooHighAmount;
            } else if(i%3==1) { //change signature
                transaction.input.signature = new Wallet().sign("i" + i);
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
        List<Transaction> transactionList = transactionPool.validTransactions();
        assertTrue(transactionList.containsAll(validTransactions), "returns valid transaction");
        assertTrue(transactionList.size()==validTransactions.size(),
                "comparison arrays must have the same size");
        assertTrue(validTransactions.containsAll(transactionList), "returns valid transaction");
    }

}
