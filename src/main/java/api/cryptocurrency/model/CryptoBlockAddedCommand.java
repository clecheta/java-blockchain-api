package api.cryptocurrency.model;

import api.repository.model.ChainTypeEnum;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Transaction;

import java.util.List;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
public class CryptoBlockAddedCommand {
    public static final ChainTypeEnum chain = ChainTypeEnum.CRYPTO;
    public Block<List<Transaction>> addedBlock;

    public CryptoBlockAddedCommand() {}

    public CryptoBlockAddedCommand(Block<List<Transaction>> block) {
        this.addedBlock = block;
    }

}
