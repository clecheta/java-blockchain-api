package api;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class SecurityEnabledTestProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "security-enabled";
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "bank.security.enabled", "true",
                "bank.security.force-enabled", "true",
                "bank.client.api-key", "test-client-key",
                "bank.service.api-key", "test-service-key",
                "quarkus.http.port", "0",
                "quarkus.http.test-port", "0",
                "quarkus.datasource.devservices.enabled", "true",
                "quarkus.mongodb.devservices.enabled", "true",
                "quarkus.kafka.devservices.enabled", "true");
    }
}
