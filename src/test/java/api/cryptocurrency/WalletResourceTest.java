package api.cryptocurrency;

import api.BaseTest;
import api.repository.WalletRepository;
import com.R07r35cInc.cryptocurrency.Wallet;
import com.R07r35cInc.util.Parse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@QuarkusTest
public class WalletResourceTest extends BaseTest {

    @Inject
    WalletRepository walletRepository;

    @BeforeEach
    void init() {

    }

    @Test
    public void createWalletSpecifyingId() {
        String myWalletIdentification = getRandomString(20);
        LinkedHashMap responseMap = given().contentType(ContentType.JSON)
                .body(myWalletIdentification)
                .when().post("/W4LL37/cRE47E")
                .then()
                .extract().path("");

        assertNotNull(responseMap, "must return a WalletInfo object");
        assertTrue(responseMap.size()==3);

        assertTrue(responseMap.containsKey("uid"), "must have uid");
        assertTrue(responseMap.containsKey("address"), "must have address");
        assertTrue(responseMap.containsKey("balance"), "must have balance");

        Wallet newSavedWallet = walletRepository.getByUid(myWalletIdentification);
        assertNotNull(newSavedWallet);

        assertEquals(Parse.toMD5(myWalletIdentification), newSavedWallet.uid,
                "the wallet identification must match the sent value");
        assertEquals(BigDecimal.ZERO, newSavedWallet.balance, "new wallet must be ZERO balance");
    }

    @Test
    public void createWalletWithoutSendAnId() {
        LinkedHashMap responseMap = given().contentType(ContentType.JSON)
                .when().post("/W4LL37/cRE47E")
                .then()
                .extract().path("");

        assertNotNull(responseMap, "must return a WalletInfo object");
        assertTrue(responseMap.size()==3);

        assertTrue(responseMap.containsKey("uid"), "must have uid");
        String returnedUid = (String) responseMap.get("uid");
        assertTrue(returnedUid.length() == UUID_LENGTH, "uid must be a valid UUID");
        assertTrue(responseMap.containsKey("address"), "must have address");
        assertTrue(responseMap.containsKey("balance"), "must have balance");

        Wallet newSavedWallet = walletRepository.getByUid(returnedUid);
        assertNotNull(newSavedWallet);

        assertEquals(Parse.toMD5(returnedUid), newSavedWallet.uid,
                "the wallet identification must match the sent value");
        assertEquals(BigDecimal.ZERO, newSavedWallet.balance, "new wallet must be ZERO balance");
    }

    @Test
    void persistedBalancePreservesMagnitudeAndScale() {
        BigDecimal amount = new BigDecimal("9223372036854775808.1234");
        Wallet wallet = new Wallet(amount);
        String originalUid = wallet.uid;

        walletRepository.add(wallet);
        Wallet restored = walletRepository.getByUid(originalUid);

        assertEquals(amount, restored.balance);
        assertEquals(amount, restored.getSTARTING_BALANCE());
    }

    @Test
    public void createWalletWithShortId() {
        String smallWalletIdentification = getRandomString(8);
        given().contentType(ContentType.JSON)
                .body(smallWalletIdentification)
                .when().post("/W4LL37/cRE47E")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void getWallet() {
        Wallet newWallet = new Wallet(BigDecimal.TEN);
        String newWalletId = newWallet.uid;
        walletRepository.add(newWallet);

        LinkedHashMap responseMap = given().contentType(ContentType.JSON)
                .body(newWalletId)
                .when().post("/W4LL37/937")
                .then()
                .extract().path("");

        assertNotNull(responseMap, "must return a WalletInfo object");
        assertTrue(responseMap.size()==3);

        String returnedUid = (String) responseMap.get("uid");
        assertTrue(returnedUid == null,
                "if you don't know your id, don't came here, you will not get it from me!");
        assertTrue(responseMap.containsKey("address"), "must have address");
        assertTrue(responseMap.containsKey("balance"), "must have balance");
        Integer balance = (Integer) responseMap.get("balance");
        assertEquals(BigDecimal.TEN, new BigDecimal(balance), "balance must match");
    }

}