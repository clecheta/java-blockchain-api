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
public class CreateTransactionTest {

    BigDecimal tooHighAmount = new BigDecimal(999999);
    String recipient = "foo-recipient";

    Wallet wallet;
    Transaction transaction;
    BigDecimal amount;

    @BeforeEach
    void init() {
        wallet = new Wallet();
        amount = BigDecimal.TEN;
        transaction = wallet.createTransaction(recipient, amount);
    }

    @Test
    void exceedsBalance() {
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> wallet.createTransaction(recipient, tooHighAmount),
                "the amount exceeds the balance"
        );
        assertTrue(thrown.getMessage().contains("Amount exceeds balance"),
                "throws an error");
    }

    // Below tests for when the amount is valid

    @Test
    void createTransaction() {
        assertTrue(transaction instanceof Transaction, "creates an instance of Transaction class");
    }

    @Test
    void matchesTransactionInput() {
        String expectedValue = wallet.publicKey;
        String actualValue = transaction.input.address;
        assertEquals(expectedValue, actualValue, "matches the transaction input with the wallet");
    }

    @Test
    void outputAmount() {
        BigDecimal actualAmount = transaction.outputMap.get(recipient);
        assertEquals(amount, actualAmount, "outputs the amount the recipient");

    }

}
