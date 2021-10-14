package api.blockchain.model;

import com.R07r35cInc.blockchain.NetChain;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@RegisterForReflection
public class BlockchainDeserializer extends ObjectMapperDeserializer<NetChain> {

    public BlockchainDeserializer() {
        super(NetChain.class);
    }
}
