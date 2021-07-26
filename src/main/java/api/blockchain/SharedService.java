package api.blockchain;

import com.R07r35cInc.NetChain;

import javax.inject.Singleton;

/* Maintain a unique instance of the blockchain in the node
 * https://quarkus.io/guides/cdi#what-scopes-can-i-actually-use-in-my-quarkus-application
 */
@Singleton
public class SharedService {

    /* TODO: maybe it should have two instances the node local in the resource and the replaced one in the subscriber
       by the end of the api we have to check what is the expected result in a sync chain for each block mined in each
       node
     */
    public NetChain blockchain = new NetChain();

}
