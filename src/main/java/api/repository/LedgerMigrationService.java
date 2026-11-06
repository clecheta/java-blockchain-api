package api.repository;

import api.repository.entity.BlockEntity;
import api.repository.entity.WalletEntity;
import api.repository.model.ChainTypeEnum;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Wallet;
import com.mongodb.client.MongoClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Explicit, restartable Mongo -> PostgreSQL backfill. Enable only during the
 * migration window; normal runtime reads and writes PostgreSQL.
 */
@ApplicationScoped
public class LedgerMigrationService {

    private static final Logger LOGGER = Logger.getLogger(LedgerMigrationService.class);

    @Inject
    MongoClient mongo;

    @ConfigProperty(name = "ledger.migration.mongo.enabled", defaultValue = "false")
    boolean enabled;

    @Transactional
    public void migrateIfEnabled() {
        if (!enabled) {
            return;
        }

        long walletCount = 0;
        for (Wallet wallet : mongo.getDatabase("blockchain")
                .getCollection("wallet", Wallet.class).find()) {
            if (WalletEntity.findById(wallet.uid) == null) {
                WalletEntity.fromDomain(wallet).persist();
                walletCount++;
            }
        }

        long blockCount = 0;
        for (Block block : mongo.getDatabase("blockchain")
                .getCollection("block", Block.class).find()) {
            if (BlockEntity.findById(block.getHash()) == null) {
                ChainTypeEnum type = ChainTypeEnum.CRYPTO.getValue().equals(block.getChain())
                        ? ChainTypeEnum.CRYPTO : ChainTypeEnum.GENERIC;
                BlockEntity.fromDomain(block, type).persist();
                blockCount++;
            }
        }
        LOGGER.infof("Mongo -> PostgreSQL backfill complete: wallets=%d blocks=%d",
                walletCount, blockCount);
    }
}
