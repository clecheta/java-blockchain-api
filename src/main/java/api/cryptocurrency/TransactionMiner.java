package api.cryptocurrency;

import api.blockchain.Blockchain;
import api.blockchain.pubsub.PublishService;
import com.R07r35cInc.wallet.Transaction;
import com.R07r35cInc.wallet.TransactionPool;
import com.R07r35cInc.wallet.Wallet;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@Singleton
public class TransactionMiner {

    /**
     * Main transaction pool for the application
     */
    public TransactionPool transactionPool = new TransactionPool();

    /**
     * Wallet instance for the overall application
     */
    public Wallet wallet = new Wallet();

    @Inject
    Blockchain blockchain;

    @Inject
    PublishService publisher;

    /**
     * All necessary functionality to add a block of transaction poll data to the blockchain
     */
    void mineTransactions() {
        //1. get the transaction pool's valid transactions @See ValidTransactionsTest
        List<Transaction> validTransactions = this.transactionPool.validTransactions();
        if (validTransactions.size() == 0) {
            throw new RuntimeException("no valid transactions found, do not mine");
        }

        //2. generate the miner's reward @See RewardTransactionTest
        validTransactions.add(Transaction.rewardTransaction(this.wallet));

        //3. add a block consisting of these transactions to the blockchain
        this.blockchain.chain.addBlock(validTransactions);

        //4. broadcast the updated blockchain
        publisher.broadcastChain(this.blockchain.chain.getChain());

        //5. clear the pool @See ClearTransactionPoolTest
        this.transactionPool.clear();
    }

}
