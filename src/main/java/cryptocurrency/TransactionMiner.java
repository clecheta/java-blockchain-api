package cryptocurrency;

import api.blockchain.Blockchain;
import cryptocurrency.wallet.TransactionPool;
import cryptocurrency.wallet.Wallet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@ApplicationScoped
public class TransactionMiner {

    /** Main transaction pool for the application */
    public TransactionPool transactionPool = new TransactionPool();

    /** Wallet instance for the overall application */
    public Wallet wallet = new Wallet();

    @Inject
    Blockchain blockchain;

    /** All necessary functionality to add a block of transaction poll data to the blockchain */
    void mineTransaction() {
        //1. get the transaction pool's valid transactions @See ValidTransactionsTest

        //2. generate the miner's reward @See RewardTransactionTest

        //3. add a block consisting of these transactions to the blockchain

        //4. broadcast the updated blockchain

        //5. clear the pool @See ClearTransactionPoolTest
    }

}
