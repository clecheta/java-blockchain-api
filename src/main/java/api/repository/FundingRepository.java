package api.repository;

import api.cryptocurrency.model.FundingCommand;
import api.repository.entity.FundingEntity;
import com.R07r35cInc.util.Parse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Instant;

@ApplicationScoped
public class FundingRepository {

    @Inject
    EntityManager entityManager;

    /**
     * Reserve an idempotency key before applying the credit.
     */
    @Transactional
    public boolean reserve(FundingCommand command) {
        int inserted = entityManager.createNativeQuery("""
                INSERT INTO funding_record
                    (idempotency_key, uid_hash, amount, status, created_at)
                VALUES (:key, :uidHash, :amount, 'PENDING', :createdAt)
                ON CONFLICT (idempotency_key) DO NOTHING
                """)
                .setParameter("key", command.idempotencyKey)
                .setParameter("uidHash", Parse.toMD5(command.uid))
                .setParameter("amount", command.amount)
                .setParameter("createdAt", Instant.now())
                .executeUpdate();
        return inserted == 1;
    }

    @Transactional
    public void complete(String idempotencyKey) {
        FundingEntity record = FundingEntity.findById(idempotencyKey);
        record.status = "COMPLETED";
        record.completedAt = Instant.now();
    }
}
