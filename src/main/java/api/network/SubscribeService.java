package api.network;

import api.blockchain.Blockchain;
import api.cryptocurrency.TransactionMiner;
import api.network.model.ChainBlockMessage;
import api.network.model.CryptoBlockMessage;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.PayTypeTransaction;
import com.R07r35cInc.cryptocurrency.Tx;
import com.R07r35cInc.util.Parse;
import io.quarkus.runtime.configuration.ProfileManager;
import io.smallrye.reactive.messaging.annotations.Blocking;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

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

    @Inject
    PeerSyncService peerSyncService;

    @Incoming("blockchain-in")
    @Blocking
    /** Append tip block or catch up when gap detected */
    public void onChainBlock(ChainBlockMessage message) {
        if (message == null || message.block == null) {
            return;
        }
        LOGGER.infof("Chain block #%d come from _%s", message.block.getNumber(), message.origin);
        if (!Boolean.TRUE.equals(message.isValidForReplacement(Util.getHostName()))) {
            return;
        }
        try {
            List chain = blockchain.chain.getChain();
            Block tip = (Block) chain.get(chain.size() - 1);
            if (message.block.getNumber() > tip.getNumber() + 1
                    || !tip.getHash().equals(message.block.getPreviousHash())) {
                LOGGER.warnf("Gap on blockchain (local tip #%d, incoming #%d) — catch-up",
                        tip.getNumber(), message.block.getNumber());
                peerSyncService.catchUpBlockchain(tip.getNumber() + 1);
                return;
            }
            blockchain.chain.appendValidatedBlock(message.block);
        } catch (RuntimeException re) {
            LOGGER.error(re.getMessage());
            List chain = blockchain.chain.getChain();
            Block tip = (Block) chain.get(chain.size() - 1);
            peerSyncService.catchUpBlockchain(tip.getNumber() + 1);
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
    /** Append tip crypto block or catch up when gap detected */
    public void onCryptoBlock(CryptoBlockMessage message) {
        if (message == null || message.block == null) {
            return;
        }
        LOGGER.infof("Cryptochain block #%d come from _%s", message.block.getNumber(), message.origin);
        if (!Boolean.TRUE.equals(message.isValidForReplacement(Util.getHostName()))) {
            return;
        }
        try {
            List<Block<List<Tx>>> local = miner.cryptoChain.getChain();
            Block<List<Tx>> tip = local.get(local.size() - 1);
            if (message.block.getNumber() > tip.getNumber() + 1
                    || !tip.getHash().equals(message.block.getPreviousHash())) {
                LOGGER.warnf("Gap on cryptochain (local tip #%d, incoming #%d) — catch-up",
                        tip.getNumber(), message.block.getNumber());
                peerSyncService.catchUpCryptochain(tip.getNumber() + 1);
                return;
            }
            if (miner.cryptoChain.appendValidatedBlock(message.block)) {
                miner.transactionPool.clearBlockchainTransactions(
                        Collections.singletonList(message.block));
            }
        } catch (RuntimeException re) {
            LOGGER.error(re.getMessage());
            List<Block<List<Tx>>> local = miner.cryptoChain.getChain();
            Block<List<Tx>> tip = local.get(local.size() - 1);
            peerSyncService.catchUpCryptochain(tip.getNumber() + 1);
        }
    }

}
