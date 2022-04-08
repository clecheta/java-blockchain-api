package api.cryptocurrency;

import api.BaseTest;
import com.R07r35cInc.cryptocurrency.Input;
import com.R07r35cInc.cryptocurrency.Transaction;
import com.R07r35cInc.cryptocurrency.Tx;
import com.R07r35cInc.cryptocurrency.Wallet;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@QuarkusTest
public class RewardTransactionTest extends BaseTest {

    static Wallet minerWallet;
    static Tx rewardTransaction;

    @BeforeEach
    void init() {
        minerWallet = new Wallet();
        rewardTransaction = Transaction.rewardTransaction(minerWallet);
    }

    @Test
    void createRewardTransaction() {
        Input expectedInput = Transaction.createRewardInput(REWARD_INPUT);
        assertEquals(expectedInput, rewardTransaction.getInput(), "creates a transaction with the reward input");
    }

    @Test
    void createMinerTransaction() {
        BigDecimal expectedReward = REWARD_MINER;
        assertEquals(expectedReward, rewardTransaction.getOutputMap().get(minerWallet.publicKey));
    }

}
