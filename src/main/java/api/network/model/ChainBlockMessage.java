package api.network.model;

import com.R07r35cInc.blockchain.Block;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Incremental generic blockchain sync payload: one mined tip block over Kafka.
 *
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@RegisterForReflection
public class ChainBlockMessage {

    public String origin;
    public long senderHeight;
    public Block block;

    public ChainBlockMessage() {
    }

    public ChainBlockMessage(String origin, Block block, long senderHeight) {
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
