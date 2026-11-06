package api.cryptocurrency;

import api.repository.WalletRepository;
import com.R07r35cInc.cryptocurrency.Wallet;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class FundingResourceTest {

    @Inject
    WalletRepository wallets;

    @Test
    void fundingIsAuthorizedAndIdempotent() {
        Wallet wallet = new Wallet();
        String uid = wallet.uid;
        wallets.add(wallet);
        String key = UUID.randomUUID().toString();
        String body = """
                {"uid":"%s","amount":"100.25","idempotencyKey":"%s"}
                """.formatted(uid, key);

        given().contentType(ContentType.JSON)
                .header("X-API-Key", "local-development-only-change-me")
                .body(body)
                .when().post("/W4LL37/fUND")
                .then().statusCode(200);

        given().contentType(ContentType.JSON)
                .header("X-API-Key", "local-development-only-change-me")
                .body(body)
                .when().post("/W4LL37/fUND")
                .then().statusCode(409);

        assertEquals(0, wallets.getByUid(uid).balance.compareTo(new BigDecimal("100.25")));
    }
}
