package api;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Keeps Quarkus profile {@code test} so %test JDBC/Mongo/Kafka URLs apply.
 * Only toggles bank security — never re-enable Dev Services (CI has no Docker).
 */
public class SecurityEnabledTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "bank.security.enabled", "true",
                "bank.security.force-enabled", "true",
                "bank.client.api-key", "test-client-key",
                "bank.service.api-key", "test-service-key",
                "quarkus.http.port", "0",
                "quarkus.http.test-port", "0");
    }
}
