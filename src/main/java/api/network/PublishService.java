package api.network;

import api.network.model.ChainBlockMessage;
import api.network.model.CryptoBlockMessage;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.PayTypeTransaction;
import com.R07r35cInc.cryptocurrency.Tx;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@ApplicationScoped
public class PublishService {

    @Inject
    @Broadcast
    @Channel("blockchain-to-kafka")
    Emitter<ChainBlockMessage> blockchainEmitter;

    @Inject
    @Broadcast
    @Channel("transaction-to-kafka")
    Emitter<PayTypeTransaction> transactionEmitter;

    @Inject
    @Broadcast
    @Channel("cryptochain-to-kafka")
    Emitter<CryptoBlockMessage> cryptochainEmitter;

    public void broadcastChainBlock(Block block, long senderHeight) {
        ChainBlockMessage message = new ChainBlockMessage(Util.getHostName(), block, senderHeight);
        blockchainEmitter.send(message);
    }

    public void broadcastTransaction(PayTypeTransaction transaction) {
        transactionEmitter.send(transaction);
    }

    public void broadcastCryptoBlock(Block<List<Tx>> block, long senderHeight) {
        CryptoBlockMessage message = new CryptoBlockMessage(Util.getHostName(), block, senderHeight);
        cryptochainEmitter.send(message);
    }

}
