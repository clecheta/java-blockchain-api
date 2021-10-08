package api.blockchain.pubsub;

import api.blockchain.Blockchain;
import api.blockchain.model.BlockchainMessage;
import api.blockchain.model.CryptochainMessage;
import api.cryptocurrency.TransactionMiner;
import com.R07r35cInc.util.Parse;
import com.R07r35cInc.cryptocurrency.Transaction;
import io.quarkus.runtime.configuration.ProfileManager;
import io.smallrye.reactive.messaging.annotations.Blocking;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@ApplicationScoped
public class SubscribeService {

    public static final String profile = ProfileManager.getActiveProfile();
    private static final Logger LOGGER = Logger.getLogger("SubscribeService");

    @Inject
    Blockchain blockchain;

    @Inject
    TransactionMiner miner;

    @Incoming("blockchain-in")
    @Blocking
    /** Replaces this chain if receive a valid blockchain message */
    public void replaceBlockchain(BlockchainMessage message) {
        LOGGER.infof("BlockchainMessage come from _%s", message.from);
        if (message.isValid()) {
            try {
                blockchain.chain.replaceChain(message.getChain());
            } catch (RuntimeException re) {
                LOGGER.error(re.getMessage());
            }
        }
    }

    @Incoming("transaction-in")
    @Blocking
    /** Set the transaction within the local transaction pool */
    public void setTransaction(Transaction transaction) {
        if (transaction != null) {
            miner.transactionPool.setTransaction(transaction);
            if (!profile.startsWith("prod")) {
                LOGGER.info(Parse.toJson(miner.transactionPool));
            }
        }
    }

    @Incoming("cryptochain-in")
    @Blocking
    /** Replaces this chain if receive a valid blockchain message */
    public void replaceCryptochain(CryptochainMessage message) {
        LOGGER.infof("CryptochainMessage come from _%s", message.from);
        if (message.isValid()) {
            try {
                if (miner.cryptoChain.replaceChain(message.getChain())) {
                    miner.transactionPool.clearBlockchainTransactions(message.getChain());
                };
            } catch (RuntimeException re) {
                LOGGER.error(re.getMessage());
            }
        }
    }

}
