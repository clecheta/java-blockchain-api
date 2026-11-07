package api.cryptocurrency;

import api.BaseTest;
import api.repository.PendingTransactionRepository;
import api.repository.SettledTransactionRepository;
import api.repository.WalletRepository;
import com.R07r35cInc.cryptocurrency.PayTypeEnum;
import com.R07r35cInc.cryptocurrency.PayTypeTransaction;
import com.R07r35cInc.cryptocurrency.TransactionCommand;
import com.R07r35cInc.cryptocurrency.Wallet;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * After a settler restart the in-memory pool is empty while
 * {@code ledger_pending_transaction} still holds accepts. Settlement must
 * gate on the durable pending count and drain Postgres.
 */
@QuarkusTest
class SettlementSchedulerTest extends BaseTest {

    @Inject
    WalletRepository walletRepository;

    @Inject
    PendingTransactionRepository pendingTransactions;

    @Inject
    SettledTransactionRepository settledTransactions;

    @Inject
    TransactionMiner miner;

    @Inject
    SettlementScheduler scheduler;

    Wallet srcWallet;
    Wallet destWallet;
    String srcUid;

    @BeforeEach
    void init() {
        miner.transactionPool.clear();
        srcWallet = new Wallet(new BigDecimal("100"));
        srcUid = srcWallet.uid;
        destWallet = new Wallet();
        walletRepository.add(srcWallet);
        walletRepository.add(destWallet);
    }

    @Test
    void settlesDurablePendingWhenInMemoryPoolIsEmpty() {
        TransactionCommand cmd = new TransactionCommand(
                destWallet.publicKey, BigDecimal.TEN, PayTypeEnum.WALLET);
        cmd.uid = srcUid;
        PayTypeTransaction accepted = pendingTransactions.reserveAndCreate(cmd);
        assertNotNull(accepted.getId());

        miner.transactionPool.clear();
        assertEquals(0, miner.transactionPool.size());
        assertTrue(pendingTransactions.count() >= 1);

        scheduler.settle();

        assertEquals(0, pendingTransactions.count());
        Set<UUID> settled = settledTransactions.findSettledIds(Set.of(accepted.getId()));
        assertTrue(settled.contains(accepted.getId()));

        Wallet savedSrc = walletRepository.getByKey(srcWallet.publicKey);
        Wallet savedDest = walletRepository.getByKey(destWallet.publicKey);
        assertEquals(new BigDecimal("90"), savedSrc.balance);
        assertEquals(new BigDecimal("10"), savedDest.balance);
    }
}
