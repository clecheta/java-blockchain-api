package cryptocurrency;

import api.blockchain.util.EcUtil;
import com.google.gson.GsonBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@QuarkusTest
public class TransactionInputTest {

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
    void hasInput() {
        Field[] fields = Transaction.class.getDeclaredFields();
        assertEquals(true, Arrays.stream(fields).anyMatch(f -> f.getName().equals("input")),
                "has an input");
    }

    @Test
    void hasDate() {
        Field[] fields = Input.class.getDeclaredFields();
        assertEquals(true, Arrays.stream(fields).anyMatch(f -> f.getName().equals("date")),
                "has a date in the input");
    }

    @Test
    void setsAmount() {
        BigDecimal expectAmount = transaction.getInput().amount;
        BigDecimal actualAmount = senderWallet.balance;
        //ensure that the amount matches the balance that is contained in the sender wallet
        assertEquals(expectAmount, actualAmount, "sets the amount to the senderWallet balance");
    }

    @Test
    void setsAddress() {
        String expectedAddress = transaction.getInput().address;
        String actualAddress = senderWallet.publicKey;
        assertEquals(expectedAddress, actualAddress, "sets the address to the senderWallet publicKey");
    }

    @Test
    void signInput() {
        EcUtil ec = new EcUtil();
        String serializedData = new GsonBuilder().create().toJson(transaction.getOutputMap());
        assertTrue(ec.verifySignature(senderWallet.publicKey, serializedData, transaction.getInput().signature),
                "signs the input");
    }

}