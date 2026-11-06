package api.cryptocurrency;

import api.cryptocurrency.model.CryptoBlockAddedCommand;
import api.network.PublishService;
import api.repository.BlockRepository;
import api.repository.model.ChainTypeEnum;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.blockchain.CryptoChain;
import com.R07r35cInc.cryptocurrency.Transaction;
import com.R07r35cInc.cryptocurrency.TransactionPool;
import com.R07r35cInc.cryptocurrency.Tx;
import com.R07r35cInc.cryptocurrency.Wallet;
import io.vertx.core.eventbus.EventBus;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * Main transaction pool, wallet instance and cryptochain
 * for overall the application.
 *
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@Singleton
public class TransactionMiner {

    public CryptoChain cryptoChain = new CryptoChain();
    public TransactionPool transactionPool = new TransactionPool();
    public Wallet rewardWallet = new Wallet();

    @Inject
    PublishService publisher;

    @Inject
    EventBus bus;

    @Inject
    BlockRepository blockRepository;

    @ConfigProperty(name = "mining.reward.enabled")
    private Boolean rewardEnabled;

    /**
     * All necessary functionality to add a block of transaction poll data to the blockchain
     */
    Boolean mineTransactions() {
        //1. get the transaction pool's valid transactions @See ValidTransactionsTest
        List<Tx> validTransactions = this.transactionPool.getValidTransactions();
        if (validTransactions.size() == 0) {
            //FIXME sometimes getting a empty array as there is no validTransaction, but they are - maybe related to kafka
            throw new RuntimeException("no valid transactions found"); //do not mine!
        }

        //2. generate the miner's reward @See RewardTransactionTest
        if (rewardEnabled != null && rewardEnabled == true) {
            validTransactions.add(Transaction.rewardTransaction(this.rewardWallet));
        }

        //3. add a block consisting of these transactions to the blockchain
        Block<List<Tx>> addedBlock = this.cryptoChain.addBlock(validTransactions);
        if (addedBlock == null || addedBlock.getData() == null || addedBlock.getData().size() == 0) {
            return false;
        }

        //4. persist tip sync before Kafka so catch-up from Mongo/root cannot miss the block
        blockRepository.add(addedBlock, ChainTypeEnum.CRYPTO);

        //5. wallet updates async (block already persisted)
        bus.send("blockAddedPostActions", new CryptoBlockAddedCommand(addedBlock, true));

        //6. broadcast the mined tip only (incremental sync)
        long height = this.cryptoChain.getChain().size() - 1L;
        publisher.broadcastCryptoBlock(addedBlock, height);

        //7. clear the pool @See ClearTransactionPoolTest
        this.transactionPool.clear();

        return true;
    }

}
