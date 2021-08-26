package api.blockchain.model;

import com.R07r35cInc.blockchain.Block;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

/**
 * Parsed objects through network channels must have all attributes made public
 * or they must have the getters and setters.
 *
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@RegisterForReflection
public class BlockchainMessage<T> {
    public String from;
    public List<Block<T>> chain;
}
