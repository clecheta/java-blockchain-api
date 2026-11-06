package api.cryptocurrency;

import api.PeerNodeTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestProfile(PeerNodeTestProfile.class)
class PeerMineResourceTest {

    @Test
    void peerCannotRunSettlement() {
        given()
                .when().post("/M1n3/7r4N54C710nZ")
                .then().statusCode(403);
    }
}
