package api.blockchain.pubsub;

import api.blockchain.Blockchain;
import api.blockchain.model.BlockchainMessage;
import api.blockchain.model.CryptochainMessage;
import api.util.Network;
import com.R07r35cInc.util.Parse;
import com.R07r35cInc.wallet.Transaction;
import api.cryptocurrency.TransactionMiner;
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
    public void replaceChain(BlockchainMessage message) {
        LOGGER.infof("new message come from _%s", message.from);
        if (isValidMessage(message)) {
            try {
                blockchain.chain.replaceChain(message.chain);
            } catch (RuntimeException re) {
                LOGGER.error(re.getMessage());
            }
        }
    }

    /**
     * Avoid redundant interactions on replace chain
     */
    public Boolean isValidMessage(BlockchainMessage message) {
        String currentHost = Network.getHostName();
        Boolean isValid = false;
        if (message != null && message.from != null && message.chain != null) {
            isValid = !message.from.equals(currentHost);
        }
        return isValid;
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
    public void replaceChain(CryptochainMessage message) {
        LOGGER.infof("new crypto come from _%s", message.from);
        try {
            blockchain.chain.replaceChain(message.chain);
        } catch (RuntimeException re) {
            LOGGER.error(re.getMessage());
        }
    }

}
