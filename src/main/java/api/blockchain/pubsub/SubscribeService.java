package api.blockchain.pubsub;

import api.blockchain.SharedService;
import api.blockchain.model.ChainMessage;
import api.blockchain.util.NetUtil;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class SubscribeService {

    @Inject
    SharedService sharedService;

    @Incoming("blockchain-in")
    /** Replaces this chain if receive a valid blockchain message */
    public void replaceChain(ChainMessage message) {
        String currentHost = NetUtil.getHostName();
        System.out.println("\nblockchain in: " + currentHost);
        System.out.println("\nblockchain from: " + message.getNode());
        if (!currentHost.equals(message.getNode())) {
            System.out.println("\nstarting processing the chain");
            sharedService.blockchain.replaceChain(message.getBlockList());
        }
    }

}
