package api.blockchain;

import com.R07r35cInc.Block;
import com.R07r35cInc.NetChain;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class PubSubService {

    public NetChain blockchain;

    PubSubService() {
        this.blockchain = new NetChain();
    }

    @Incoming("blockchain-in")
    /** Replaces this chain if receive a valid blockchain message */
    public void replaceChain(List<Block> newChain) {
        if (newChain != null) {
            this.blockchain.replaceChain(newChain);
        }
    }

}
