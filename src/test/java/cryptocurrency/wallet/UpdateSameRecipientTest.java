package cryptocurrency.wallet;

import api.blockchain.util.MathUtil;
import cryptocurrency.wallet.Transaction;
import cryptocurrency.wallet.Wallet;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Making another update for the same recipient
 *
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@QuarkusTest
public class UpdateSameRecipientTest {

    private static final String publicKey = "recipient-foo-public-key";
    static Transaction transaction;
    static Wallet senderWallet;
    static String recipient;
    static BigDecimal amount;
    BigDecimal originalSenderOutput;
    String nextRecipient;
    BigDecimal nextAmount;
    BigDecimal addedAmount;
    @ConfigProperty(name = "starting.balance")
    private BigDecimal STARTING_BALANCE;

    @BeforeAll
    static void init() {
        senderWallet = new Wallet();
        recipient = publicKey;
        amount = BigDecimal.TEN;
        transaction = new Transaction(senderWallet, recipient, amount);
    }

    @BeforeEach
    void setup() {
        originalSenderOutput = transaction.outputMap.get(senderWallet.publicKey);
        nextRecipient = "next-recipient";
        nextAmount = new BigDecimal(5);

        transaction.update(senderWallet, nextRecipient, nextAmount);
    }

    @Test
    void sumToRecipient() {
        addedAmount = new BigDecimal(7);
        transaction.update(senderWallet, nextRecipient, addedAmount);

        BigDecimal expectedAmount = transaction.outputMap.get(nextRecipient);
        BigDecimal actualAmount = nextAmount.add(addedAmount);
        assertEquals(expectedAmount, actualAmount, "adds to the recipient amount");

        assertEquals(STARTING_BALANCE, MathUtil.calculateMapTotal(transaction.outputMap));
    }

    @Test
    void subtractsFromOriginalSender() {
        addedAmount = new BigDecimal(8);
        transaction.update(senderWallet, nextRecipient, addedAmount);

        BigDecimal expectedAmount = transaction.outputMap.get(senderWallet.publicKey);
        BigDecimal actualAmount = originalSenderOutput.subtract(nextAmount).subtract(addedAmount);
        assertEquals(expectedAmount, actualAmount,
                "subtracts the amount from the original sender output amount");

        assertEquals(STARTING_BALANCE, MathUtil.calculateMapTotal(transaction.outputMap));
    }
}