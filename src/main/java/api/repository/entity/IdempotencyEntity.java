package api.repository.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "api_idempotency")
public class IdempotencyEntity extends PanacheEntityBase {
    @Id
    @Column(name = "idempotency_key", length = 128)
    public String idempotencyKey;

    @Column(nullable = false, length = 32)
    public String operation;

    @Column(nullable = false, length = 128)
    public String subject;

    @Column(name = "request_hash", nullable = false, length = 128)
    public String requestHash;

    @Column(name = "response_status")
    public Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "text")
    public String responseBody;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
