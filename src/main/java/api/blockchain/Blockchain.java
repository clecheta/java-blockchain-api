package api.blockchain;

import com.R07r35cInc.NetChain;

import javax.inject.Singleton;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@Singleton
public class Blockchain {

    /**
     * Maintain a unique instance of the blockchain in the node
     * TODO: maybe it should have two instances the node local in the resource and the replaced one in the subscriber
     *        by the end of the api we have to check what is the expected result in a sync chain for each block mined in each
     *        node
     */
    public NetChain chain = new NetChain();

}
