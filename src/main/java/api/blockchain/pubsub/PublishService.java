package api.blockchain.pubsub;

import api.blockchain.model.ChainMessage;
import api.blockchain.util.NetUtil;
import com.R07r35cInc.Block;
import cryptocurrency.Transaction;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class PublishService {

    @Inject
    @Broadcast
    @Channel("blockchain-to-kafka")
    Emitter<ChainMessage> blockchainEmitter;

    @Inject
    @Broadcast
    @Channel("transaction-to-kafka")
    Emitter<Transaction> transactionEmitter;

    public void broadcastChain(List<Block> nodeBlocks) {
        ChainMessage message = new ChainMessage();
        message.from = NetUtil.getHostName();
        message.chain = nodeBlocks;
        blockchainEmitter.send(message);
    }

    public void broadcastTransaction(Transaction transaction) {
        transactionEmitter.send(transaction);
    }

}
