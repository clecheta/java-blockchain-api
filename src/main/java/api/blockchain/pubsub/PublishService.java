package api.blockchain.pubsub;

import api.blockchain.model.CryptochainMessage;
import api.util.Network;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.blockchain.NetChain;
import com.R07r35cInc.cryptocurrency.Transaction;
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
    Emitter<NetChain> blockchainEmitter;

    @Inject
    @Broadcast
    @Channel("transaction-to-kafka")
    Emitter<Transaction> transactionEmitter;

    @Inject
    @Broadcast
    @Channel("cryptochain-to-kafka")
    Emitter<CryptochainMessage> cryptochainEmitter;

    public void broadcastChain(NetChain chain) {
        chain.origin = Network.getHostName();
        blockchainEmitter.send(chain);
    }

    public void broadcastTransaction(Transaction transaction) {
        transactionEmitter.send(transaction);
    }


    public void broadcastCryptochain(List<Block<List<Transaction>>> nodeBlocks) {
        CryptochainMessage message = new CryptochainMessage();
        message.from = Network.getHostName();
        message.chain = nodeBlocks;
        cryptochainEmitter.send(message);
    }

}
