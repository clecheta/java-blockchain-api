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
        sharedService.blockchain.replaceChain(message.chain);
    }

    public Boolean isValidMessage(ChainMessage message) {
        String currentHost = NetUtil.getHostName();
        Boolean isValid = false;
        if (message != null && message.from != null && message.chain != null) {
            isValid = !message.from.equals(currentHost);
        }
        return isValid;
    }

}
