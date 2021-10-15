package api.cryptocurrency.model;

import com.R07r35cInc.cryptocurrency.Transaction;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@RegisterForReflection
public class TransactionDeserializer extends ObjectMapperDeserializer<Transaction> {

    public TransactionDeserializer() {
        super(Transaction.class);
    }
}
