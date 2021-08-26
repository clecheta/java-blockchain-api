package api.blockchain.model;

import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.wallet.Transaction;
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
public class CryptochainMessage {
    public String from;
    public List<Block<List<Transaction>>> chain;

    /*
    Unable to deserialize property 'chain' because of: Unable to deserialize property 'data'
     because of: Error deserialize JSON value into type: java.util.List<com.R07r35cInc.wallet.Transaction>
     */
}
