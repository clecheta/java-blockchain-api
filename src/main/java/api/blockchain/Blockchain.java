package api.blockchain;

import com.R07r35cInc.blockchain.NetChain;

import javax.inject.Singleton;

/**
 * Maintain a unique instance of the blockchain in the node
 *
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@Singleton
public class Blockchain {

    public NetChain chain = new NetChain();

}
