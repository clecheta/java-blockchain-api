package api.repository.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_settled_transaction")
public class SettledTransactionEntity extends PanacheEntityBase {

    @Id
    @Column(name = "transaction_id")
    public UUID transactionId;

    @Column(name = "block_hash", nullable = false, length = 128)
    public String blockHash;

    @Column(name = "settled_at", nullable = false)
    public Instant settledAt;
}
