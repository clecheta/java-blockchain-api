package api.blockchain.model;

import io.quarkus.kafka.client.serialization.JsonbDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@RegisterForReflection
public class BlockchainDeserializer extends JsonbDeserializer<ChainMessage> {

    public BlockchainDeserializer() {
        super(ChainMessage.class);
    }
}
