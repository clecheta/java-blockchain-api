package api;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * @author Cleber Lecheta
 * https://gitlab.com/cleberlecheta/java-blockchain-api
 */
public class PeerNodeTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "ledger.node.role", "peer",
                "mining.settlement.enabled", "false");
    }
}
