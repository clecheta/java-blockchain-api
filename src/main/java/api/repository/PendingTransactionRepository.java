package api.repository;

import api.repository.entity.PendingTransactionEntity;
import api.repository.entity.WalletEntity;
import com.R07r35cInc.cryptocurrency.PayTypeTransaction;
import com.R07r35cInc.cryptocurrency.TransactionCommand;
import com.R07r35cInc.cryptocurrency.Tx;
import com.R07r35cInc.cryptocurrency.Wallet;
import com.R07r35cInc.util.Parse;
import io.quarkus.narayana.jta.QuarkusTransaction;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PostgreSQL-backed inbox and spend reservation for accepted, unsettled
 * transactions. This is the cross-node correctness boundary for acceptance.
 */
@ApplicationScoped
public class PendingTransactionRepository {

    @Inject
    EntityManager entityManager;

    /**
     * Sign outside the database transaction, then take a short pessimistic
     * lock only for balance recheck, pending insert, and reserved_balance.
     */
    public PayTypeTransaction reserveAndCreate(TransactionCommand command) {
        if (command.amount == null || command.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        String uidHash = Parse.toMD5(command.uid);
        WalletEntity snapshot = QuarkusTransaction.requiringNew()
                .call(() -> entityManager.find(WalletEntity.class, uidHash));
        if (snapshot == null) {
            throw new IllegalArgumentException("Wallet not found");
        }

        BigDecimal available = snapshot.balance.subtract(snapshot.reservedBalance);
        if (command.amount.compareTo(available) > 0) {
            throw new IllegalArgumentException("Amount exceeds available balance");
        }

        // ECDSA runs here — no JDBC connection or row lock held.
        Wallet wallet = snapshot.toDomain();
        wallet.balance = available;
        PayTypeTransaction transaction =
                (PayTypeTransaction) wallet.createTransaction(command);

        return QuarkusTransaction.requiringNew()
                .call(() -> persistReservation(uidHash, command, transaction));
    }

    private PayTypeTransaction persistReservation(
            String uidHash,
            TransactionCommand command,
            PayTypeTransaction transaction) {
        WalletEntity source = entityManager.find(
                WalletEntity.class, uidHash, LockModeType.PESSIMISTIC_WRITE);
        if (source == null) {
            throw new IllegalArgumentException("Wallet not found");
        }

        BigDecimal available = source.balance.subtract(source.reservedBalance);
        if (command.amount.compareTo(available) > 0) {
            throw new IllegalArgumentException("Amount exceeds available balance");
        }

        PendingTransactionEntity.fromDomain(transaction, command.amount)
                .persistAndFlush();
        int updated = entityManager.createNativeQuery("""
                UPDATE ledger_wallet
                   SET reserved_balance = reserved_balance + :amount,
                       updated_at = CURRENT_TIMESTAMP
                 WHERE uid_hash = :uidHash
                """)
                .setParameter("amount", command.amount)
                .setParameter("uidHash", uidHash)
                .executeUpdate();
        if (updated != 1) {
            throw new IllegalStateException("Failed to reserve wallet balance");
        }
        return transaction;
    }

    public List<Tx> loadOldest(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return PendingTransactionEntity.find(
                        "order by createdAt, transactionId")
                .page(0, limit)
                .list()
                .stream()
                .map(PendingTransactionEntity.class::cast)
                .map(PendingTransactionEntity::toDomain)
                .toList();
    }

    @Transactional
    public void removeAndRelease(List<Tx> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return;
        }
        List<UUID> ids = transactions.stream().map(Tx::getId).toList();
        List<PendingTransactionEntity> pending = new ArrayList<>(
                PendingTransactionEntity.list("transactionId in ?1", ids));
        if (pending.isEmpty()) {
            return;
        }

        Map<String, BigDecimal> releasedBySender = new HashMap<>();
        pending.forEach(item -> releasedBySender.merge(
                item.senderPublicKey, item.amount, BigDecimal::add));

        releasedBySender.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> {
                    int updated = entityManager.createNativeQuery("""
                            UPDATE ledger_wallet
                               SET reserved_balance = reserved_balance - :amount,
                                   updated_at = CURRENT_TIMESTAMP
                             WHERE public_key = :sender
                               AND reserved_balance >= :amount
                            """)
                            .setParameter("amount", entry.getValue())
                            .setParameter("sender", entry.getKey())
                            .executeUpdate();
                    if (updated != 1) {
                        throw new IllegalStateException(
                                "Pending reservation underflow for sender");
                    }
                });
        PendingTransactionEntity.delete("transactionId in ?1", ids);
    }

    public long count() {
        return PendingTransactionEntity.count();
    }

    public boolean exists(UUID transactionId) {
        return PendingTransactionEntity.findById(transactionId) != null;
    }
}
