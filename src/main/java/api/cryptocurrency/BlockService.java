package api.cryptocurrency;

import api.cryptocurrency.model.CryptoBlockAddedCommand;
import api.repository.BlockRepository;
import api.repository.WalletRepository;
import com.R07r35cInc.cryptocurrency.Tx;
import io.quarkus.vertx.ConsumeEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@ApplicationScoped
public class BlockService {

    //private static final Logger LOGGER = Logger.getLogger("BlockService");

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

        List<Tx> transactionList = cmd.addedBlock.getData();
        if (transactionList.size() == 0) {
            throw new RuntimeException("block missing transactions something really went wrong!");
        }

        // Persist only when miner did not already write the block (legacy / non-mine paths)
        if (!cmd.blockAlreadyPersisted) {
            blockRepository.add(cmd.addedBlock, CryptoBlockAddedCommand.chain);
        }

        // Apply all wallet deltas and nonce increments in one database read.
        walletRepository.applySettledTransactions(transactionList);
    }

}
