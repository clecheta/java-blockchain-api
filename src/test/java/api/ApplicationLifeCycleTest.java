package api;

import api.blockchain.Blockchain;
import api.cryptocurrency.TransactionMiner;
import api.network.Util;
import com.R07r35cInc.util.Parse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.*;

import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Startup wiring against compose Postgres + Kafka — empty DB is a valid baseline.
 */
@QuarkusTest
@TestProfile(RewardMinerTestProfile.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@Order(1)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApplicationLifeCycleTest extends BaseTest {

    private static final Logger LOGGER = Logger.getLogger("LoadFromDatabaseTest");

    @Inject
    TransactionMiner miner;

    @Inject
    Blockchain blockchain;

    @ConfigProperty(name = "mining.reward.wallet")
    private String rewardWalletIdentification;

    @BeforeEach
    void init() {
        assertNotNull(miner);
    }

    @Test
    @Order(1)
    public void loadWallet() {
        assertNotNull(miner.rewardWallet);
        assertEquals(Parse.toMD5(rewardWalletIdentification), miner.rewardWallet.uid,
                "reward wallet identification must match application.properties value");
        assertEquals(BigDecimal.ZERO, miner.rewardWallet.balance,
                "fresh reward wallet starts at zero when DB has no prior balance");
        LOGGER.info("Reward wallet: IN PLACE");
    }

    @Test
    @Order(2)
    public void loadBlockchain() {
        assertNotNull(blockchain.chain);
        assertFalse(blockchain.chain.getChain().isEmpty(), "generic chain must include at least genesis");
    }

    @Test
    @Order(3)
    public void loadCryptochain() {
        assertNotNull(miner.cryptoChain);
        assertFalse(miner.cryptoChain.getChain().isEmpty(), "cryptochain must include at least genesis");
        LOGGER.info("loadCryptochain: " + Util.getHostIp() + " " + Util.getHostName() + " | " + Util.profile
                + " size=" + miner.cryptoChain.getChain().size());

        assertTrue(miner.cryptoChain.isValidChain(miner.cryptoChain.getChain()),
                "in-memory cryptochain must be structurally valid");
        assertTrue(miner.cryptoChain.isValidTransactionData(miner.cryptoChain.getChain(), BigDecimal.ZERO),
                "transaction data must be valid");
        LOGGER.info("Cryptochain: DATA IS VALID");
    }
}
