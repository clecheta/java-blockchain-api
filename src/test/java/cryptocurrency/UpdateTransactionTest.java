package cryptocurrency;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@QuarkusTest
public class UpdateTransactionTest {

    private static final String publicKey = "recipient-foo-public-key";
    BigDecimal tooHighAmount = new BigDecimal(999999);

    static Transaction transaction;
    static Wallet senderWallet;
    static String recipient;
    static BigDecimal amount;

    String originalSignature;
    BigDecimal originalSenderOutput;
    String nextRecipient;
    BigDecimal nextAmount;

    @BeforeAll
    static void init() {
        senderWallet = new Wallet();
        recipient = publicKey;
        amount = BigDecimal.TEN;
        transaction = new Transaction(senderWallet, recipient, amount);
    }

    @BeforeEach
    void setup() {
        originalSignature = transaction.getInput().signature;
        originalSenderOutput = transaction.getOutputMap().get(senderWallet.publicKey);
        nextRecipient = "next-recipient";
        nextAmount = new BigDecimal(5);

        transaction.update(senderWallet, nextRecipient, nextAmount);
    }

    @Test
    void isInvalidAmount() {
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> transaction.update(senderWallet, recipient, tooHighAmount),
                "the amount is invalid"
        );
        assertTrue(thrown.getMessage().contains("Amount exceeds balance"),
                "throws an error");

    }

    // Below tests for when the transaction is valid

    @Test
    void isValidOutputAmountToTheNextRecipient() {
        BigDecimal actualAmount = transaction.getOutputMap().get(nextRecipient);
        assertEquals(nextAmount, actualAmount, "outputs the amount to the next recipient");
    }

    @Test
    void isValidSubtractsAmountFromOriginalSender() {
        BigDecimal actualAmount = transaction.getOutputMap().get(senderWallet.publicKey);
        BigDecimal expectedAmount = originalSenderOutput.subtract(nextAmount);
        assertEquals(expectedAmount, actualAmount,
                "subtracts the amount from the original sender output amount");
    }

    @Test
    void isValidMaintainsTotalOutputThatMatchesInputAmount() {
        BigDecimal outputTotal = BigDecimal.ZERO;
        Iterator<Map.Entry<String, BigDecimal>> iterator = transaction.getOutputMap().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, BigDecimal> entry = iterator.next();
            outputTotal = outputTotal.add(entry.getValue());
        }
        assertEquals(transaction.getInput().amount, outputTotal,
                "maintains a total output that matches the input amount");
    }

    @Test
    void isValidReSignTransaction() {
        String actualSignature = transaction.getInput().signature;
        assertNotEquals(originalSignature, actualSignature, "re-signs the transaction");
    }
}