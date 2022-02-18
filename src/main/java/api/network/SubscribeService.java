package api.network;

import api.blockchain.Blockchain;
import api.cryptocurrency.TransactionMiner;
import com.R07r35cInc.blockchain.CryptoChain;
import com.R07r35cInc.blockchain.NetChain;
import com.R07r35cInc.cryptocurrency.PayTypeTransaction;
import com.R07r35cInc.cryptocurrency.Transaction;
import com.R07r35cInc.util.Parse;
import io.quarkus.runtime.configuration.ProfileManager;
import io.smallrye.reactive.messaging.annotations.Blocking;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
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
    public void replaceBlockchain(NetChain blockchain) {
        LOGGER.infof("Blockchain come from _%s", blockchain.origin);
        if (blockchain.isValidForReplacement(Util.getHostName())) {
            try {
                this.blockchain.chain.replaceChain(blockchain.getChain());
            } catch (RuntimeException re) {
                LOGGER.error(re.getMessage());
            }
        }
    }

    @Incoming("transaction-in")
    @Blocking
    /** Set the transaction within the local transaction pool */
    public void setTransaction(PayTypeTransaction transaction) {
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
    public void replaceCryptochain(CryptoChain cryptoChain) {
        LOGGER.infof("Cryptochain come from _%s", cryptoChain.origin);
        if (cryptoChain.isValidForReplacement(Util.getHostName())) {
            try {
                if (miner.cryptoChain.replaceChain(cryptoChain.getChain())) {
                    miner.transactionPool.clearBlockchainTransactions(cryptoChain.getChain());
                }
            } catch (RuntimeException re) {
                LOGGER.error(re.getMessage());
            }
        }
    }

}
