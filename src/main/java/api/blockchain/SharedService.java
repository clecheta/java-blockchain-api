package api.blockchain;

import com.R07r35cInc.NetChain;

import javax.inject.Singleton;

/* Maintain a unique instance of the blockchain in the node
 * https://quarkus.io/guides/cdi#what-scopes-can-i-actually-use-in-my-quarkus-application
 */
@Singleton
public class SharedService {

    public NetChain blockchain = new NetChain();

}
