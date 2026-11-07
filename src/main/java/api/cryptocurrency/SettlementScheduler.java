package api.cryptocurrency;

import api.repository.PendingTransactionRepository;
import io.quarkus.scheduler.Scheduled;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;

/**
 * Portable fallback scheduler. Production may invoke the same settlement
 * service from EventBridge Scheduler, Cloud Scheduler, or Azure Logic Apps.
 * Gates on durable pending count so a settler restart still drains
 * {@code ledger_pending_transaction} when the in-memory pool is empty.
 */
@ApplicationScoped
public class SettlementScheduler {

    private static final Logger LOGGER = Logger.getLogger(SettlementScheduler.class);

    @Inject
    TransactionMiner miner;

    @Inject
    PendingTransactionRepository pendingTransactions;

    @ConfigProperty(name = "mining.settlement.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "mining.settlement.min-pool-size", defaultValue = "1")
    int minPoolSize;

    @Scheduled(cron = "${mining.settlement.cron:disabled}", concurrentExecution = SKIP)
    void settle() {
        if (!enabled || pendingTransactions.count() < minPoolSize) {
            return;
        }
        if (miner.mineTransactions() == null) {
            LOGGER.debug("Settlement skipped: another run is active or no valid transactions exist");
        }
    }
}
