package api.blockchain;

import com.R07r35cInc.Block;
import io.quarkus.kafka.client.serialization.JsonbDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class BlockchainDeserializer extends JsonbDeserializer<List<Block>> {

    public BlockchainDeserializer() {
        super(List.class);
    }
}
