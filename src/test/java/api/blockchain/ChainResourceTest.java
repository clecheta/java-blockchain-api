package api.blockchain;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * Minimal HTTP coverage for the generic chain endpoints.
 */
@QuarkusTest
public class ChainResourceTest {

    @Test
    public void mineThenListBlocks() {
        given()
                .contentType("application/json")
                .body("\"chain-resource-test\"")
                .when().post("/Ch41N/M1n3")
                .then()
                .statusCode(200);

        given()
                .when().post("/Ch41N/8l0cx")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(2));
    }

}
