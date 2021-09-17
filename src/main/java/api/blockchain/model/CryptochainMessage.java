package api.blockchain.model;

import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Transaction;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

/**
 * Parsed objects through network channels must have all attributes made public
 * or they must have the getters and setters
 *
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@RegisterForReflection
public class CryptochainMessage extends ChainMessage<Block<List<Transaction>>> {
    public List<Block<List<Transaction>>> chain;

    @Override
    public List<Block<List<Transaction>>> getChain() {
        return this.chain;
    }
}
