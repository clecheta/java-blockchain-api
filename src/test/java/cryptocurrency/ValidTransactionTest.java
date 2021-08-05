package cryptocurrency;

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
public class ValidTransactionTest {

    private static final String publicKey = "recipient-public-key";

    Transaction transaction;
    Wallet senderWallet;
    String recipient;
    BigDecimal amount;

    @BeforeEach
    void init() {
        senderWallet = new Wallet();
        recipient = publicKey;
        amount = BigDecimal.TEN;
        transaction = new Transaction(senderWallet, recipient, amount);
    }

    @Test
    void isValid() {
        assertTrue(Transaction.isValidTransaction(transaction), "when the transaction is valid");
    }

    // Below tests for when the transaction is invalid

    @Test
    void invalidOutputMap() {
        BigDecimal rightBalanceAmount = transaction.getOutputMap().get(senderWallet.publicKey);
        assertEquals(new BigDecimal(90), rightBalanceAmount);

        transaction.getOutputMap().put("senderWallet.publicKey", new BigDecimal(100).negate());
        assertFalse(Transaction.isValidTransaction(transaction), "transaction outputMap value is invalid");
    }

    @Test
    void invalidSignature() {
        transaction.getInput().signature = new Wallet().sign("fakeData");
        assertFalse(Transaction.isValidTransaction(transaction), "transaction input signature is invalid");
    }
}