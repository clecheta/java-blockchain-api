package api.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class SettlementLease {

    // Stable application-specific PostgreSQL advisory-lock key.
    private static final long LOCK_KEY = 7_074_625_671L;

    @Inject
    EntityManager entityManager;

    /**
     * Transaction-scoped lock: automatically released on commit/rollback.
     */
    public boolean tryAcquire() {
        Object value = entityManager.createNativeQuery(
                        "SELECT pg_try_advisory_xact_lock(:lockKey)")
                .setParameter("lockKey", LOCK_KEY)
                .getSingleResult();
        return Boolean.TRUE.equals(value);
    }
}
