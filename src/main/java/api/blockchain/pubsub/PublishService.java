package api.blockchain.pubsub;

import api.blockchain.model.ChainMessage;
import com.R07r35cInc.Block;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class PublishService {

    @Inject
    @Channel("blockchain-to-kafka")
    Emitter<ChainMessage> blockchainEmitter;

    public void broadcastChain(List<Block> blockList) {
        ChainMessage message = new ChainMessage(blockList);
        blockchainEmitter.send(message);
    }

}
