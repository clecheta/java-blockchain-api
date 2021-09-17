package api.cryptocurrency;

import api.blockchain.pubsub.PublishService;
import com.R07r35cInc.blockchain.CryptoChain;
import com.R07r35cInc.cryptocurrency.Transaction;
import com.R07r35cInc.cryptocurrency.TransactionPool;
import com.R07r35cInc.cryptocurrency.Wallet;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * Main transaction pool, wallet instance and cryptochain
 * for overall the application.
 *
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@Singleton
public class TransactionMiner {

    public CryptoChain cryptoChain = new CryptoChain();
    public TransactionPool transactionPool = new TransactionPool();
    public Wallet wallet = new Wallet();

    @Inject
    PublishService publisher;

    /**
     * All necessary functionality to add a block of transaction poll data to the blockchain
     */
    void mineTransactions() {
        //1. get the transaction pool's valid transactions @See ValidTransactionsTest
        List<Transaction> validTransactions = this.transactionPool.getValidTransactions();
        if (validTransactions.size() == 0) {
            throw new RuntimeException("no valid transactions found"); //do not mine!
        }

        //2. generate the miner's reward @See RewardTransactionTest
        validTransactions.add(Transaction.rewardTransaction(this.wallet));

        //3. add a block consisting of these transactions to the blockchain
        this.cryptoChain.addBlock(validTransactions);

        //4. broadcast the updated cryptochain
        publisher.broadcastCryptochain(this.cryptoChain.getChain());

        //5. clear the pool @See ClearTransactionPoolTest
        this.transactionPool.clear();
    }

}
