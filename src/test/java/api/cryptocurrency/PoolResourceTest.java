package api.cryptocurrency;

import api.BaseTest;
import com.R07r35cInc.cryptocurrency.PayTypeEnum;
import com.R07r35cInc.cryptocurrency.TransactionCommand;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@QuarkusTest
public class PoolResourceTest extends BaseTest {

    private static final int UUID_LENGTH = 36;
    TransactionCommand cmd;

    @BeforeEach
    void init() {
        this.cmd = new TransactionCommand();
        cmd.recipient = publicKey;
        cmd.amount = BigDecimal.TEN;
        cmd.type = PayTypeEnum.WALLET;
    }

    @Test
    public void transactSuccess() {
        given().contentType(ContentType.JSON)
                .body(cmd)
                .when().post("/p00L/tRaN5ACt")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void validTransaction() {
        LinkedHashMap responseMap = given().contentType(ContentType.JSON)
                .body(cmd)
                .when().post("/p00L/tRaN5ACt")
                .then()
                .extract().path("");

        assertNotNull(responseMap, "must return a transaction object");
        String uuid = (String) responseMap.get("id");

        assertTrue(responseMap.containsKey("id"), "must have an id");
        assertTrue(uuid.length() == UUID_LENGTH, "id must be a valid UUID");
        assertTrue(responseMap.containsKey("outputMap"), "must have an outputMap");
        assertTrue(responseMap.containsKey("input"), "must have a input");
    }

    @Test
    public void transactFail() {
        cmd.amount = tooHighAmount;
        given().contentType(ContentType.JSON)
                .body(cmd)
                .when().post("/p00L/tRaN5ACt")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void getTransactionMap() {
        given()
          .when().post("/p00L/7r4n54C7ioNM4p")
          .then()
             .statusCode(200);
    }

}