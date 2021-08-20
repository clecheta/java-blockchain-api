package api.cryptocurrency;

import com.R07r35cInc.wallet.Input;
import com.R07r35cInc.wallet.Transaction;
import com.R07r35cInc.wallet.Wallet;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@QuarkusTest
public class RewardTransactionTest {

    static Wallet minerWallet;
    static Transaction rewardTransaction;
    @ConfigProperty(name = "reward.input")
    private String REWARD_INPUT;
    @ConfigProperty(name = "mining.reward")
    private BigDecimal MINING_REWARD;

    @BeforeEach
    void init() {
        minerWallet = new Wallet();
        rewardTransaction = Transaction.rewardTransaction(minerWallet);
    }

    @Test
    void createRewardTransaction() {
        Input expectedInput = Transaction.createRewardInput(REWARD_INPUT);
        assertEquals(expectedInput, rewardTransaction.input, "creates a transaction with the reward input");
    }

    @Test
    void createMinerTransaction() {
        BigDecimal expectedReward = MINING_REWARD;
        assertEquals(expectedReward, rewardTransaction.outputMap.get(minerWallet.publicKey));
    }

}
