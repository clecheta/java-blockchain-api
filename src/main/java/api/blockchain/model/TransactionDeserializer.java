package api.blockchain.model;

import com.R07r35cInc.wallet.Transaction;
import io.quarkus.kafka.client.serialization.JsonbDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@RegisterForReflection
public class TransactionDeserializer extends JsonbDeserializer<Transaction> {

    public TransactionDeserializer() {
        super(Transaction.class);
    }
}
