package api.network.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

/**
 * Typed Kafka deserializer for generic chain tip blocks.
 *
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@RegisterForReflection
public class ChainBlockMessageDeserializer implements Deserializer<ChainBlockMessage> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<ChainBlockMessage> TYPE =
            new TypeReference<ChainBlockMessage>() {
            };

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // no-op
    }

    @Override
    public ChainBlockMessage deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return MAPPER.readValue(data, TYPE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize ChainBlockMessage", e);
        }
    }

    @Override
    public void close() {
        // no-op
    }
}
