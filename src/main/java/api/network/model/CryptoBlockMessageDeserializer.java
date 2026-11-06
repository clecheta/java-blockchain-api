package api.network.model;

import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Tx;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.List;
import java.util.Map;

/**
 * Typed Kafka deserializer preserving {@code Block<List<Tx>>} (not LinkedHashMap).
 *
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@RegisterForReflection
public class CryptoBlockMessageDeserializer implements Deserializer<CryptoBlockMessage> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<CryptoBlockMessage> TYPE =
            new TypeReference<CryptoBlockMessage>() {
            };
    private static final TypeReference<Block<List<Tx>>> BLOCK_TYPE =
            new TypeReference<Block<List<Tx>>>() {
            };

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // no-op
    }

    @Override
    public CryptoBlockMessage deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            CryptoBlockMessage message = MAPPER.readValue(data, TYPE);
            if (message != null && message.block != null) {
                // Force nested block/data through parametric type so Tx polymorphism applies
                byte[] blockBytes = MAPPER.writeValueAsBytes(message.block);
                message.block = MAPPER.readValue(blockBytes, BLOCK_TYPE);
            }
            return message;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize CryptoBlockMessage", e);
        }
    }

    @Override
    public void close() {
        // no-op
    }
}
