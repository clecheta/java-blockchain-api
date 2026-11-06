package api.repository;

import api.repository.entity.SettledTransactionEntity;
import com.R07r35cInc.cryptocurrency.Tx;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class SettledTransactionRepository {

    public Set<UUID> findSettledIds(Collection<UUID> transactionIds) {
        if (transactionIds.isEmpty()) {
            return Set.of();
        }
        List<SettledTransactionEntity> settled = SettledTransactionEntity.list(
                "transactionId in ?1", transactionIds);
        Set<UUID> ids = new HashSet<>(settled.size());
        settled.forEach(entity -> ids.add(entity.transactionId));
        return ids;
    }

    public void record(String blockHash, Collection<? extends Tx> transactions) {
        Instant now = Instant.now();
        for (Tx transaction : transactions) {
            SettledTransactionEntity entity = new SettledTransactionEntity();
            entity.transactionId = transaction.getId();
            entity.blockHash = blockHash;
            entity.settledAt = now;
            entity.persist();
        }
        SettledTransactionEntity.flush();
    }
}
