package api.cryptocurrency;

import api.cryptocurrency.model.CryptoBlockAddedCommand;
import api.network.PublishService;
import api.repository.BlockRepository;
import api.repository.SettlementLease;
import api.repository.SettledTransactionRepository;
import api.repository.model.ChainTypeEnum;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.blockchain.CryptoChain;
import com.R07r35cInc.cryptocurrency.Transaction;
import com.R07r35cInc.cryptocurrency.TransactionPool;
import com.R07r35cInc.cryptocurrency.Tx;
import com.R07r35cInc.cryptocurrency.Wallet;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import io.quarkus.narayana.jta.QuarkusTransaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Main transaction pool, wallet instance and cryptochain
 * for overall the application.
 *
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@Singleton
public class TransactionMiner {

    public volatile CryptoChain cryptoChain = new CryptoChain();
    public TransactionPool transactionPool = new TransactionPool();
    public Wallet rewardWallet = new Wallet();
    private final ReentrantLock miningLock = new ReentrantLock();

    @Inject
    PublishService publisher;

    @Inject
    BlockRepository blockRepository;

    @Inject
    SettlementLease settlementLease;

    @Inject
    BlockService blockService;

    @Inject
    SettledTransactionRepository settledTransactions;

    @ConfigProperty(name = "mining.reward.enabled")
    private Boolean rewardEnabled;

    @ConfigProperty(name = "mining.settlement.max-batch-size", defaultValue = "500")
    int maxBatchSize;

    /**
     * All necessary functionality to add a block of transaction poll data to the blockchain
     */
    public Block<List<Tx>> mineTransactions() {
        if (!miningLock.tryLock()) {
            return null;
        }
        try {
            SettlementBatch batch = QuarkusTransaction.requiringNew()
                    .call(this::settleInDatabaseTransaction);
            if (batch == null) {
                return null;
            }

            // Database commit completed: now remove the exact snapshot and fan out.
            this.transactionPool.removeTransactions(batch.poolTransactions());
            long height = this.cryptoChain.getChain().size() - 1L;
            publisher.broadcastCryptoBlock(batch.block(), height);
            return batch.block();
        } finally {
            miningLock.unlock();
        }
    }

    private SettlementBatch settleInDatabaseTransaction() {
        if (!settlementLease.tryAcquire()) {
            return null;
        }

        synchronizeWithLedger();

        // Stable snapshot: accepts that arrive after this point stay pending.
        List<Tx> poolTransactions = new ArrayList<>(
                this.transactionPool.getValidTransactions(maxBatchSize));
        if (poolTransactions.isEmpty()) {
            return null;
        }

        Set<UUID> settledIds = settledTransactions.findSettledIds(
                poolTransactions.stream().map(Tx::getId).toList());
        if (!settledIds.isEmpty()) {
            List<Tx> duplicates = poolTransactions.stream()
                    .filter(tx -> settledIds.contains(tx.getId())).toList();
            this.transactionPool.removeTransactions(duplicates);
            poolTransactions.removeIf(tx -> settledIds.contains(tx.getId()));
        }
        if (poolTransactions.isEmpty()) {
            return null;
        }

        List<Tx> blockTransactions = new ArrayList<>(poolTransactions);
        if (Boolean.TRUE.equals(rewardEnabled)) {
            blockTransactions.add(Transaction.rewardTransaction(this.rewardWallet));
        }

        Block<List<Tx>> addedBlock = this.cryptoChain.addBlock(blockTransactions);
        if (addedBlock == null || addedBlock.getData() == null
                || addedBlock.getData().isEmpty()) {
            return null;
        }

        blockRepository.add(addedBlock, ChainTypeEnum.CRYPTO);
        settledTransactions.record(addedBlock.getHash(), poolTransactions);
        blockService.blockAddedPostActions(
                new CryptoBlockAddedCommand(addedBlock, true));
        return new SettlementBatch(addedBlock, List.copyOf(poolTransactions));
    }

    private void synchronizeWithLedger() {
        CryptoChain localChain = this.cryptoChain;
        List<Block<List<Tx>>> localBlocks = localChain.getChain();
        Block<List<Tx>> localTip = localBlocks.get(localBlocks.size() - 1);
        Block<List<Tx>> databaseTip = blockRepository.loadCryptochainTip();

        if (databaseTip.getHash().equals(localTip.getHash())) {
            return;
        }
        if (databaseTip.getNumber() > localTip.getNumber()) {
            try {
                List<Block<List<Tx>>> missing = blockRepository
                        .loadCryptochainFromNumber(localTip.getNumber() + 1);
                for (Block<List<Tx>> block : missing) {
                    localChain.appendValidatedBlock(block);
                }
                Block<List<Tx>> synchronizedTip = localChain.getChain()
                        .get(localChain.getChain().size() - 1);
                if (databaseTip.getHash().equals(synchronizedTip.getHash())) {
                    return;
                }
            } catch (RuntimeException ignored) {
                // Fall through to an authoritative full reload.
            }
        }

        List<Block<List<Tx>>> persisted = blockRepository.loadCryptochain();
        CryptoChain ledgerChain = new CryptoChain();
        if (persisted.size() > 1) {
            ledgerChain.replaceChain(persisted, BigDecimal.ZERO);
        }
        this.cryptoChain = ledgerChain;
    }

    private record SettlementBatch(Block<List<Tx>> block,
                                   List<Tx> poolTransactions) {
    }
}
