package api.cryptocurrency.model;

import api.repository.model.ChainTypeEnum;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Tx;

import java.util.List;

/**
 * EventBus command after a crypto block is mined.
 * When {@link #blockAlreadyPersisted} is true, {@code BlockService} only updates wallets.
 *
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
public class CryptoBlockAddedCommand {
    public static final ChainTypeEnum chain = ChainTypeEnum.CRYPTO;
    public Block<List<Tx>> addedBlock;
    /** Miner persists sync before Kafka; EventBus must not insert again. */
    public boolean blockAlreadyPersisted;

    public CryptoBlockAddedCommand() {
    }

    public CryptoBlockAddedCommand(Block<List<Tx>> block) {
        this(block, false);
    }

    public CryptoBlockAddedCommand(Block<List<Tx>> block, boolean blockAlreadyPersisted) {
        this.addedBlock = block;
        this.blockAlreadyPersisted = blockAlreadyPersisted;
    }

}
