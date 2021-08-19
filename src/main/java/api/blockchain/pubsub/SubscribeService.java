package api.blockchain.pubsub;

import api.blockchain.Blockchain;
import api.blockchain.model.ChainMessage;
import api.blockchain.util.NetUtil;
import com.google.gson.GsonBuilder;
import cryptocurrency.TransactionMiner;
import cryptocurrency.wallet.Transaction;
import io.quarkus.runtime.configuration.ProfileManager;
import io.smallrye.reactive.messaging.annotations.Blocking;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
    public void replaceChain(ChainMessage message) {
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
    public Boolean isValidMessage(ChainMessage message) {
        String currentHost = NetUtil.getHostName();
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
                String serializedTransactionPool = new GsonBuilder().setPrettyPrinting()
                        .create().toJson(miner.transactionPool);
                LOGGER.info(serializedTransactionPool);
            }
        }
    }

}
