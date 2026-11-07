package api.repository.entity;

import com.R07r35cInc.cryptocurrency.Tx;
import com.R07r35cInc.util.Parse;
import com.fasterxml.jackson.core.type.TypeReference;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_pending_transaction")
public class PendingTransactionEntity extends PanacheEntityBase {

    @Id
    @Column(name = "transaction_id")
    public UUID transactionId;

    @Column(name = "sender_public_key", nullable = false, columnDefinition = "text")
    public String senderPublicKey;

    @Column(nullable = false, columnDefinition = "numeric")
    public BigDecimal amount;

    @Column(name = "transaction_json", nullable = false, columnDefinition = "text")
    public String transactionJson;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    public static PendingTransactionEntity fromDomain(Tx transaction, BigDecimal amount) {
        PendingTransactionEntity entity = new PendingTransactionEntity();
        entity.transactionId = transaction.getId();
        entity.senderPublicKey = transaction.getInput().address;
        entity.amount = amount;
        entity.transactionJson = Parse.toJson(transaction);
        entity.createdAt = Instant.now();
        return entity;
    }

    public Tx toDomain() {
        Parse<Tx> parser = new Parse<>();
        return parser.fromJson(transactionJson, new TypeReference<Tx>() {
        });
    }
}
