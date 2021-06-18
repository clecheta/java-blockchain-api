package api.blockchain;

import api.blockchain.model.ChainMessage;
import com.R07r35cInc.NetChain;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped //https://quarkus.io/guides/cdi#what-scopes-can-i-actually-use-in-my-quarkus-application
public class PubSubService {

    public NetChain blockchain;

    PubSubService() {
        this.blockchain = new NetChain();
    }

    @Incoming("blockchain-in")
    /** Replaces this chain if receive a valid blockchain message */
    public void replaceChain(ChainMessage message) {
        System.out.println("message.getBlockList().get(1)");
        this.blockchain.replaceChain(message.getBlockList());
    }

}
