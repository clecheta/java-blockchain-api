package api.cryptocurrency;

import api.cryptocurrency.model.CryptoBlockAddedCommand;
import api.repository.BlockRepository;
import api.repository.WalletRepository;
import com.R07r35cInc.cryptocurrency.Transaction;
import com.R07r35cInc.cryptocurrency.Tx;
import com.R07r35cInc.cryptocurrency.Wallet;
import com.R07r35cInc.util.Calculate;
import io.quarkus.vertx.ConsumeEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@ApplicationScoped
public class BlockService {

    //private static final Logger LOGGER = Logger.getLogger("BlockService");

    @Inject
    TransactionMiner miner;

    @Inject
    BlockRepository blockRepository;

    @Inject
    WalletRepository walletRepository;

    /**
     * Post actions after a mined block is added to the chain.
     * - persist the new block added to the chain
     * - update involved wallets balance according to the transactions made
     * please call using EventBus only
     */
    @ConsumeEvent("blockAddedPostActions")
    public void blockAddedPostActions(CryptoBlockAddedCommand cmd) {
        if (cmd.addedBlock == null || cmd.addedBlock.getData() == null) {
            throw new RuntimeException("error adding block to the chain, nothing to do.");
        }

        List<Transaction> transactionList = cmd.addedBlock.getData();
        if (transactionList.size() == 0) {
            throw new RuntimeException("block missing transactions something really went wrong!");
        }

        //Persist the just mined block in the database
        blockRepository.add(cmd.addedBlock, CryptoBlockAddedCommand.chain);

        //Update involved wallets balance according to the transactions made
        for (Tx transaction : transactionList) {
            if (!transaction.getInput().address.equals(Transaction.REWARD_INPUT)) { //Regular Transactions
                Wallet inputWallet = walletRepository.getByKey(transaction.getInput().address);
                updateWalletBalance(inputWallet, true); //Money sender

                Iterator<Map.Entry<String, BigDecimal>> iterator = transaction.getOutputMap().entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, BigDecimal> entry = iterator.next();
                    Wallet outputWallet = walletRepository.getByKey(entry.getKey());
                    //Let's save some database load by not updating the same wallet more than once
                    if (!outputWallet.publicKey.equals(inputWallet.publicKey)) { //input wallet is contained in the output map!
                        updateWalletBalance(outputWallet, false); //Money receiver
                    }
                }
            }
        }

        //Update reward wallet balance, do not increase nonce in this case
        updateWalletBalance(miner.rewardWallet, false);
    }

    /**
     * Should only update nonce in the wallet that money goes out from it
     */
    private void updateWalletBalance(Wallet wallet, Boolean increaseNonce) {
        BigDecimal currentValue = Calculate.walletBalance(this.miner.cryptoChain.getChain(),
                wallet.publicKey,
                wallet.getSTARTING_BALANCE());
        if (increaseNonce != null && increaseNonce == true) {
            wallet.nonce += 1;
        }
        walletRepository.update(wallet, currentValue);
    }

}
