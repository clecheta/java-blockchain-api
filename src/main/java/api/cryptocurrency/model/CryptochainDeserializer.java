package api.cryptocurrency.model;

import com.R07r35cInc.blockchain.CryptoChain;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@RegisterForReflection
public class CryptochainDeserializer extends ObjectMapperDeserializer<CryptoChain> {

    public CryptochainDeserializer() {
        super(CryptoChain.class);
    }
}
