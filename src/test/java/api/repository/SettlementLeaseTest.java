package api.repository;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class SettlementLeaseTest {

    @Inject
    SettlementLease lease;

    @Test
    void onlyOneDatabaseTransactionCanHoldSettlementLease() throws Exception {
        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> first = executor.submit(() ->
                    QuarkusTransaction.requiringNew().call(() -> {
                        boolean held = lease.tryAcquire();
                        acquired.countDown();
                        release.await(10, TimeUnit.SECONDS);
                        return held;
                    }));

            assertTrue(acquired.await(10, TimeUnit.SECONDS));
            boolean second = QuarkusTransaction.requiringNew()
                    .call(lease::tryAcquire);
            assertFalse(second, "a concurrent node must not acquire the lease");

            release.countDown();
            assertTrue(first.get(10, TimeUnit.SECONDS));

            boolean afterRelease = QuarkusTransaction.requiringNew()
                    .call(lease::tryAcquire);
            assertTrue(afterRelease, "lease must be released at transaction end");
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }
}
