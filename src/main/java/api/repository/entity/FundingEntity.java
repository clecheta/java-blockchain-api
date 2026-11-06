package api.repository.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "funding_record")
public class FundingEntity extends PanacheEntityBase {

    @Id
    @Column(name = "idempotency_key", length = 128)
    public String idempotencyKey;

    @Column(name = "uid_hash", nullable = false, length = 64)
    public String uidHash;

    @Column(nullable = false, columnDefinition = "numeric")
    public BigDecimal amount;

    @Column(nullable = false, length = 16)
    public String status;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "completed_at")
    public Instant completedAt;
}
