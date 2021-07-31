package cryptocurrency;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@QuarkusTest
public class TransactionTest {

    private static final String publicKey = "recipient-public-key";
    private static final int UUID_LENGHT = 36;

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
    void hasId() {
        assertTrue(transaction.id != null, "has an id");
        assertTrue(transaction.id instanceof UUID, "id is UUID");
        assertTrue(transaction.id.toString().length() == UUID_LENGHT, "id is correctly formed");
    }

    @Test
    void hasOutputMap() {
        Field[] fields = Transaction.class.getDeclaredFields();
        assertEquals(true, Arrays.stream(fields).anyMatch(f -> f.getName().equals("outputMap")), "has an outputMap");
    }

    @Test
    void outputsAmoutToRecipient() {
        BigDecimal expectAmount = transaction.outputMap.get(senderWallet.publicKey);
        BigDecimal actualAmount = senderWallet.balance.subtract(amount);
        assertEquals(expectAmount, actualAmount, "outputs the remaining balance for the senderWallet");
    }

}