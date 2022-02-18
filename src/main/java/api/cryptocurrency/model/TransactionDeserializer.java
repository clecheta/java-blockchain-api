package api.cryptocurrency.model;

import com.R07r35cInc.cryptocurrency.PayTypeTransaction;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@RegisterForReflection
public class TransactionDeserializer extends ObjectMapperDeserializer<PayTypeTransaction> {

    public TransactionDeserializer() {
        super(PayTypeTransaction.class);
    }
}
