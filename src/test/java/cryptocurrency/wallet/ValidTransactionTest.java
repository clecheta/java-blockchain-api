package cryptocurrency.wallet;

import cryptocurrency.wallet.Transaction;
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
        BigDecimal rightBalanceAmount = transaction.outputMap.get(senderWallet.publicKey);
        assertEquals(new BigDecimal(90), rightBalanceAmount);

        transaction.outputMap.put("senderWallet.publicKey", new BigDecimal(100).negate());
        assertFalse(Transaction.isValidTransaction(transaction), "transaction outputMap value is invalid");
    }

    @Test
    void invalidSignature() {
        transaction.input.signature = new Wallet().sign("fakeData");
        assertFalse(Transaction.isValidTransaction(transaction), "transaction input signature is invalid");
    }
}