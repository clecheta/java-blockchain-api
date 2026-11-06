package api;

import api.blockchain.Blockchain;
import api.cryptocurrency.TransactionMiner;
import api.network.PeerSyncService;
import api.network.Util;
import api.repository.BlockRepository;
import api.repository.DatabaseRepository;
import api.repository.LedgerMigrationService;
import api.repository.WalletRepository;
import com.R07r35cInc.cryptocurrency.Wallet;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.math.BigDecimal;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@ApplicationScoped
public class ApplicationLifeCycle {

    private static final Logger LOGGER = Logger.getLogger("ApplicationLifeCycle");

    @Inject
    Blockchain blockchain;

    @Inject
    TransactionMiner miner;

    @Inject
    BlockRepository blockRepository;

    @Inject
    WalletRepository walletRepository;

    @Inject
    DatabaseRepository databaseRepository;

    @Inject
    LedgerMigrationService ledgerMigration;

    @Inject
    PeerSyncService peerSyncService;

    @ConfigProperty(name = "mining.reward.wallet")
    private String rewardWallet;

    @ConfigProperty(name = "mining.reward.enabled")
    private Boolean rewardEnabled;

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("Blockchain network node _" + Util.getHostName() + " is starting...");

        ledgerMigration.migrateIfEnabled();

        //load chain from database
        loadFromDatabase();

        if (!peerSyncService.isRoot()) {
            // Peers bootstrap from the single settlement/root node.
            peerSyncService.syncBlockchainFull();
            peerSyncService.syncCryptochainFull();
            peerSyncService.syncTransactionPoolMap();
        }

        databaseRepository.createIndex();
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOGGER.info("Blockchain network node _" + Util.getHostName() + " stopped.");
    }

    private void loadFromDatabase() {
        loadWallet();
        loadChain();
    }

    private void loadWallet() {
        if (Boolean.TRUE.equals(rewardEnabled)) {
            try {
                miner.rewardWallet = walletRepository.getByUid(rewardWallet);
            } catch (RuntimeException re) {
                LOGGER.warn("Reward Wallet missing; bootstrapping from config uid: " + re.getMessage());
                Wallet bootstrap = new Wallet(rewardWallet);
                walletRepository.add(bootstrap);
                miner.rewardWallet = bootstrap;
            }
        }
    }

    /* Failure on one chain must not interfere in the other, so the process of loading should not be interrupted */
    private void loadChain() {
        if (peerSyncService.isRoot()) {
            try {
                blockchain.chain.replaceChain(blockRepository.loadBlockchain());
            } catch (RuntimeException re) {
                LOGGER.warn("Blockchain: " + re.getMessage());
            }

            try {
                //FIXME: https://git.okaut.com/Jesus/api.blockchain/-/issues/19
                // cannot replaceChain as the calculation of balance mismatch
                // in the next api starts, the wallet change it's address, so even if i save the balance and load it again
                // calculation will fail as we are talking about different addresses now
                miner.cryptoChain.replaceChain(blockRepository.loadCryptochain(), BigDecimal.ZERO);
            } catch (RuntimeException re) {
                LOGGER.warn("Cryptochain: " + re.getMessage());
            }
        }
    }

}
