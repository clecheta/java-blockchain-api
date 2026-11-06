package api.cryptocurrency;

import api.BaseTest;
import api.repository.WalletRepository;
import com.R07r35cInc.cryptocurrency.PayTypeEnum;
import com.R07r35cInc.cryptocurrency.TransactionCommand;
import com.R07r35cInc.cryptocurrency.Wallet;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@QuarkusTest
public class PoolResourceTest extends BaseTest {

    @Inject
    WalletRepository walletRepository;

    TransactionCommand cmd;
    String srcUid;
    Wallet srcWallet;
    Wallet destWallet;

    @BeforeEach
    void init() {
        srcWallet = new Wallet(new BigDecimal(100));
        srcUid = srcWallet.uid;
        destWallet = new Wallet();
        walletRepository.add(srcWallet);
        walletRepository.add(destWallet);

        this.cmd = new TransactionCommand();
        cmd.uid = srcUid;
        cmd.recipient = destWallet.publicKey;
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
    void unsafeJsonNumberIsRejected() {
        String body = """
                {"uid":"%s","recipient":"%s","amount":9007199254740992,"type":"WALLET"}
                """.formatted(srcUid, destWallet.publicKey);

        given().contentType(ContentType.JSON)
                .body(body)
                .when().post("/p00L/tRaN5ACt")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    void decimalStringIsAccepted() {
        String body = """
                {"uid":"%s","recipient":"%s","amount":"10.25","type":"WALLET"}
                """.formatted(srcUid, destWallet.publicKey);

        given().contentType(ContentType.JSON)
                .body(body)
                .when().post("/p00L/tRaN5ACt")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void duplicateIdempotencyKeyIsRejected() {
        String key = UUID.randomUUID().toString();
        String body = """
                {"uid":"%s","recipient":"%s","amount":"10","type":"WALLET"}
                """.formatted(srcUid, destWallet.publicKey);

        given().contentType(ContentType.JSON)
                .header("Idempotency-Key", key)
                .body(body)
                .when().post("/p00L/tRaN5ACt")
                .then().statusCode(HttpStatus.SC_OK);

        given().contentType(ContentType.JSON)
                .header("Idempotency-Key", key)
                .body(body)
                .when().post("/p00L/tRaN5ACt")
                .then().statusCode(HttpStatus.SC_CONFLICT);
    }

    @Test
    void getTransactionMap() {
        given()
          .when().post("/p00L/7r4n54C7ioNM4p")
          .then()
             .statusCode(200);
    }

}