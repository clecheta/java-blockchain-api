package api;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
public class RewardMinerTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("mining.reward.enabled", "true");
    }

}
