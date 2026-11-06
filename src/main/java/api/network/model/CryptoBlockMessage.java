package api.network.model;

import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Tx;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

/**
 * Incremental cryptochain sync payload: one mined tip block over Kafka.
 *
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@RegisterForReflection
public class CryptoBlockMessage {

    /** Sending node hostname — used by isValidForReplacement */
    public String origin;

    /** Tip height of the sender after mining this block */
    public long senderHeight;

    public Block<List<Tx>> block;

    public CryptoBlockMessage() {
    }

    public CryptoBlockMessage(String origin, Block<List<Tx>> block, long senderHeight) {
        this.origin = origin;
        this.block = block;
        this.senderHeight = senderHeight;
    }

    public Boolean isValidForReplacement(String currentHost) {
        Boolean isValid = false;
        if (this.origin != null && this.block != null) {
            isValid = !this.origin.equals(currentHost);
        }
        return isValid;
    }
}
