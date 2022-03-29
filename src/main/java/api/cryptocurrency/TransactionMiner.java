package api.cryptocurrency;

import api.network.PublishService;
import api.repository.BlockRepository;
import api.repository.WalletRepository;
import api.repository.model.ChainTypeEnum;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.blockchain.CryptoChain;
import com.R07r35cInc.cryptocurrency.Transaction;
import com.R07r35cInc.cryptocurrency.TransactionPool;
import com.R07r35cInc.cryptocurrency.Tx;
import com.R07r35cInc.cryptocurrency.Wallet;
import com.R07r35cInc.util.Calculate;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
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
    public Wallet wallet = new Wallet();

    @Inject
    PublishService publisher;

    @Inject
    BlockRepository blockRepository;

    @Inject
    WalletRepository walletRepository;

    /**
     * All necessary functionality to add a block of transaction poll data to the blockchain
     */
    void mineTransactions() {
        //1. get the transaction pool's valid transactions @See ValidTransactionsTest
        List<Tx> validTransactions = this.transactionPool.getValidTransactions();
        //FIXME sometimes getting a empty array as there is no validTransaction, but they are - maybe related to kafka
        if (validTransactions.size() == 0) {
            throw new RuntimeException("no valid transactions found"); //do not mine!
        }

        //2. generate the miner's reward @See RewardTransactionTest
        validTransactions.add(Transaction.rewardTransaction(this.wallet));

        //3. add a block consisting of these transactions to the blockchain
        Block<List<Transaction>> addedBlock = this.cryptoChain.addBlock(validTransactions);
        blockRepository.add(addedBlock, ChainTypeEnum.CRYPTO); //TODO: use Vert.x bus event to write to disk
        //FIXME for new let's do it here, update the wallet balance
        BigDecimal currentValue = Calculate.walletBalance(this.cryptoChain.getChain(),
                this.wallet.publicKey,
                this.wallet.getSTARTING_BALANCE());
        this.wallet.nonce += 1; //Should only update the number of output transactions (money that goes out) from the wallet
        walletRepository.update(this.wallet, currentValue); //TODO: use Vert.x bus event to write to disk

        //4. broadcast the updated cryptochain
        publisher.broadcastCryptochain(this.cryptoChain);

        //5. clear the pool @See ClearTransactionPoolTest
        this.transactionPool.clear();
    }

}
