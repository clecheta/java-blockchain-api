package api.repository;

import api.repository.entity.WalletEntity;
import com.R07r35cInc.cryptocurrency.RewardTransaction;
import com.R07r35cInc.cryptocurrency.Tx;
import com.R07r35cInc.cryptocurrency.Wallet;
import com.R07r35cInc.util.Parse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@ApplicationScoped
public class WalletRepository {

    public Wallet getByUid(String uid) {
        String md5Id = Parse.toMD5(uid);
        WalletEntity entity = WalletEntity.findById(md5Id);
        if (entity == null) {
            throw new RuntimeException("Wallet not found!");
        }
        return entity.toDomain();
    }

    public Wallet getByKey(String publicKey) {
        WalletEntity entity = WalletEntity.find("publicKey", publicKey).firstResult();
        return entity == null ? null : entity.toDomain();
    }

    @Transactional
    public void add(Wallet wallet) {
        String md5Id = Parse.toMD5(wallet.uid);
        wallet.uid = md5Id;
        WalletEntity.fromDomain(wallet).persistAndFlush();
    }

    @Transactional
    public void update(Wallet wallet, BigDecimal currentValue) {
        WalletEntity entity = WalletEntity.findById(wallet.uid);
        if (entity == null) {
            throw new RuntimeException("Wallet not found!");
        }
        entity.balance = currentValue;
        entity.nonce = wallet.nonce;
        entity.updatedAt = Instant.now();
    }

    /**
     * Authorized issuance/deposit path. The starting balance is advanced with
     * the materialized balance so restart/recalculation keeps the credit.
     * PostgreSQL P2 makes this update and its funding audit record atomic.
     */
    @Transactional
    public BigDecimal fund(String uid, BigDecimal amount) {
        String md5Id = Parse.toMD5(uid);
        WalletEntity entity = WalletEntity.findById(md5Id);
        if (entity == null) {
            throw new RuntimeException("Wallet not found!");
        }
        entity.balance = entity.balance.add(amount);
        entity.startingBalance = entity.startingBalance.add(amount);
        entity.updatedAt = Instant.now();
        return entity.balance;
    }

    /**
     * Apply one settled block with a single wallet read. This is both faster
     * than per-transaction lookups and atomic with block persistence.
     */
    @Transactional
    public void applySettledTransactions(List<Tx> transactions) {
        Map<String, BigDecimal> deltas = new HashMap<>();
        Map<String, Long> nonceIncrements = new HashMap<>();

        for (Tx transaction : transactions) {
            if (transaction instanceof RewardTransaction) {
                transaction.getOutputMap().forEach((address, amount) ->
                        deltas.merge(address, amount, BigDecimal::add));
                continue;
            }

            String sender = transaction.getInput().address;
            BigDecimal change = transaction.getOutputMap()
                    .getOrDefault(sender, BigDecimal.ZERO);
            BigDecimal spent = transaction.getInput().amount.subtract(change);
            if (spent.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Settled transfer must spend a positive amount");
            }
            deltas.merge(sender, spent.negate(), BigDecimal::add);
            nonceIncrements.merge(sender, 1L, Long::sum);
            transaction.getOutputMap().forEach((address, amount) -> {
                if (!address.equals(sender)) {
                    deltas.merge(address, amount, BigDecimal::add);
                }
            });
        }

        List<WalletEntity> entities = WalletEntity.list(
                "publicKey in ?1", deltas.keySet());
        if (entities.size() != deltas.size()) {
            throw new IllegalStateException("Settled block references an unknown wallet");
        }

        Instant now = Instant.now();
        for (WalletEntity entity : entities) {
            BigDecimal updated = entity.balance.add(deltas.get(entity.publicKey));
            if (updated.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException(
                        "Settlement would overdraw wallet " + entity.publicKey);
            }
            entity.balance = updated;
            entity.nonce += nonceIncrements.getOrDefault(entity.publicKey, 0L);
            entity.updatedAt = now;
        }
    }

}
