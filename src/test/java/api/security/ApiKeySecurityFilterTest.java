package api.security;

import api.SecurityEnabledTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

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

    @Test
    void serviceCredentialCanReachSettlementRole() {
        given()
                .header("X-API-Key", "test-service-key")
                .when().post("/M1n3/7r4N54C710nZ")
                .then().statusCode(204);
    }
}
