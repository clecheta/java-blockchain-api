package api.blockchain.pubsub;

import api.blockchain.model.ChainMessage;
import api.util.Network;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.wallet.Transaction;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
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
        message.from = Network.getHostName();
        message.chain = nodeBlocks;
        blockchainEmitter.send(message);
    }

    public void broadcastTransaction(Transaction transaction) {
        transactionEmitter.send(transaction);
    }

}
