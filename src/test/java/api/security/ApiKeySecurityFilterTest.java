package api.security;

import api.SecurityEnabledTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(SecurityEnabledTestProfile.class)
class ApiKeySecurityFilterTest {

    @Test
    void protectedEndpointRejectsMissingCredential() {
        given()
                .when().post("/W4LL37/cRE47E")
                .then().statusCode(401);
    }

    @Test
    void clientCredentialCannotExecuteSettlement() {
        given()
                .header("X-API-Key", "test-client-key")
                .when().post("/M1n3/7r4N54C710nZ")
                .then().statusCode(401);
    }

    /**
     * Asserts authorization only: 204 (empty mempool) or 200 (block mined) both prove the
     * service key reached the settlement role. Mempool state depends on earlier test classes
     * when the suite shares one Postgres (GitLab CI services), so the exact code is not pinned.
     */
    @Test
    void serviceCredentialCanReachSettlementRole() {
        given()
                .header("X-API-Key", "test-service-key")
                .when().post("/M1n3/7r4N54C710nZ")
                .then().statusCode(anyOf(is(200), is(204)));
    }
}
