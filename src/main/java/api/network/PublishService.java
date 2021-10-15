package api.network;

import com.R07r35cInc.blockchain.CryptoChain;
import com.R07r35cInc.blockchain.NetChain;
import com.R07r35cInc.cryptocurrency.Transaction;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
    Emitter<CryptoChain> cryptochainEmitter;

    public void broadcastChain(NetChain blockchain) {
        blockchain.origin = Util.getHostName();
        blockchainEmitter.send(blockchain);
    }

    public void broadcastTransaction(Transaction transaction) {
        transactionEmitter.send(transaction);
    }


    public void broadcastCryptochain(CryptoChain cryptoChain) {
        cryptoChain.origin = Util.getHostName();
        cryptochainEmitter.send(cryptoChain);
    }

}
