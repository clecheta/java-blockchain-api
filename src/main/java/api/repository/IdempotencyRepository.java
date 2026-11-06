package api.repository;

import api.repository.entity.IdempotencyEntity;
import com.R07r35cInc.util.CryptoHash;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class IdempotencyRepository {

    @Inject
    EntityManager entityManager;

    @Transactional
    public boolean reserve(String key, String operation, String subject, String request) {
        int inserted = entityManager.createNativeQuery("""
                INSERT INTO api_idempotency
                    (idempotency_key, operation, subject, request_hash, created_at)
                VALUES (:key, :operation, :subject, :requestHash, :createdAt)
                ON CONFLICT (idempotency_key) DO NOTHING
                """)
                .setParameter("key", key)
                .setParameter("operation", operation)
                .setParameter("subject", subject)
                .setParameter("requestHash", CryptoHash.generateHash(request))
                .setParameter("createdAt", Instant.now())
                .executeUpdate();
        return inserted == 1;
    }

    public long recentCount(String operation, String subject, long minutes) {
        return IdempotencyEntity.count(
                "operation = ?1 and subject = ?2 and createdAt >= ?3",
                operation, subject, Instant.now().minus(minutes, ChronoUnit.MINUTES));
    }
}
