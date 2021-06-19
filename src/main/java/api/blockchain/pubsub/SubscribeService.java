package api.blockchain.pubsub;

import api.blockchain.SharedService;
import api.blockchain.model.ChainMessage;
import api.blockchain.util.NetUtil;
import com.google.gson.GsonBuilder;
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
        //String currentHost = NetUtil.getHostName();
        //System.out.println("\nwhoiam: " + currentHost);
        //System.out.println("\nblockchain from: " + message.from);
        //String blockchainJson = new GsonBuilder().setPrettyPrinting().create().toJson(message.chain);
        //System.out.println(blockchainJson);
        //if (!currentHost.equals(message.from)) {
        //if (message.isValid(currentHost)) {
            sharedService.blockchain.replaceChain(message.chain);
        //}
    }

}
