package api.blockchain;

import com.R07r35cInc.NetChain;
import cryptocurrency.TransactionPool;
import cryptocurrency.Wallet;

import javax.inject.Singleton;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@Singleton //https://quarkus.io/guides/cdi#what-scopes-can-i-actually-use-in-my-quarkus-application
public class SharedService {

    /** Maintain a unique instance of the blockchain in the node
     * TODO: maybe it should have two instances the node local in the resource and the replaced one in the subscriber
     *        by the end of the api we have to check what is the expected result in a sync chain for each block mined in each
     *        node
     */
    public NetChain blockchain = new NetChain();

    /** Main transaction pool for the application */
    public TransactionPool transactionPool = new TransactionPool();

    /** Wallet instance for the overall application */
    public Wallet wallet = new Wallet();

}
