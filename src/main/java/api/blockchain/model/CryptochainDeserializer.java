package api.blockchain.model;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@RegisterForReflection
public class CryptochainDeserializer extends ObjectMapperDeserializer<CryptochainMessage> {

    public CryptochainDeserializer() {
        super(CryptochainMessage.class);
    }
}
