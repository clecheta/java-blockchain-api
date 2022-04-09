package api;

import api.blockchain.Blockchain;
import api.cryptocurrency.TransactionMiner;
import api.network.Util;
import com.R07r35cInc.blockchain.CryptoChain;
import com.R07r35cInc.util.Parse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 * <p>
 * Load existing blockchain and cryptochain at api startup and verifify the integrety of database saved chains.
 * The aim is to keep original data readable after any change in the existing code.
 * We must ensure we can properly load the most old information saved in the chain
 * together with new data formats that will raise.
 */
@QuarkusTest
@TestProfile(RewardMinerTestProfile.class)
public class LoadFromDatabaseTest extends BaseTest {

    private static final Logger LOGGER = Logger.getLogger("LoadFromDatabaseTest");

    @Inject
    TransactionMiner miner;

    @Inject
    Blockchain blockchain;

    @ConfigProperty(name = "mining.reward.wallet")
    private String rewardWalletIdentification;

    @BeforeEach
    void init() {
        LOGGER.info("ApplicationLifeCycle already run, let's test: loadFromDatabase()");
        assertNotNull(miner);
    }

    @Test
    public void loadWallet() {
        assertNotNull(miner.rewardWallet);
        assertEquals(Parse.toMD5(rewardWalletIdentification), miner.rewardWallet.uid,
                "reward wallet identification must match application.properties value");
        assertEquals(BigDecimal.ZERO, miner.rewardWallet.balance,
                "reward balance must match database saved value");
        LOGGER.info("Reward wallet: IN PLACE");
    }

    @Test
    public void loadBlockchain() {
        assertNotNull(blockchain.chain);
    }

    /* FIXME https://git.okaut.com/Jesus/api.blockchain/-/issues/14#note_6741
     * Test does Work as expected in localhost but not in gitlab-ci pipeline */
    @Test
    public void loadCryptochain() {
        assertNotNull(miner.cryptoChain);
        LOGGER.info("loadCryptochain: " + Util.getHostIp() + " " + Util.getHostName() + " | " + Util.profile);

        //Always compare the database size by adding +1 to size referring the in memory (Genesis Block)
        assertEquals(10 + 1, miner.cryptoChain.getChain().size(),
                "initial chain size must match the saved data");
        LOGGER.info("Cryptochain: IN PLACE");

        CryptoChain newChain = new CryptoChain();
        assertDoesNotThrow(() -> {
            newChain.replaceChain(miner.cryptoChain.getChain());
        }, "The incoming chain has valid data");
        LOGGER.info("Cryptochain: REPLACE CHAIN - Successfully");

        Boolean isValid = miner.cryptoChain.isValidTransactionData(newChain.getChain(), BigDecimal.ZERO);
        assertTrue(isValid, "the transaction data is valid");
        LOGGER.info("Cryptochain: DATA IS VALID");
    }
}