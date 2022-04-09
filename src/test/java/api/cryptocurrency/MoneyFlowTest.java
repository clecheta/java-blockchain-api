package api.cryptocurrency;

import api.BaseTest;
import api.RewardMinerTestProfile;
import api.repository.WalletRepository;
import com.R07r35cInc.cryptocurrency.PayTypeEnum;
import com.R07r35cInc.cryptocurrency.TransactionCommand;
import com.R07r35cInc.cryptocurrency.Wallet;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.*;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 * <p>
 * - An origin that starts with some amount (100)
 * - An destination starting with 0 and receiving the transaction/input amount from origin (20)
 * - A reward starting with 0 and receiving the reward, (1)
 */
@QuarkusTest
@TestProfile(RewardMinerTestProfile.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@Order(2)
public class MoneyFlowTest extends BaseTest {

    @Inject
    WalletRepository walletRepository;

    @Inject
    TransactionMiner miner;

    String srcUid;
    Wallet srcWallet;
    Wallet destWallet;
    Wallet rewardWallet;

    @BeforeEach
    void init() {
        srcWallet = new Wallet(new BigDecimal(100));
        srcUid = srcWallet.uid;
        destWallet = new Wallet();
        rewardWallet = new Wallet();

        walletRepository.add(srcWallet);
        walletRepository.add(destWallet);
        walletRepository.add(rewardWallet);

        //we must ensure that reward wallet is the one Miner will work with
        miner.rewardWallet = rewardWallet;
    }

    @Test
    //by the end of this test the origin wallet must have 80, destination 20, and reward 1
    public void walletToWalletSingleTransaction() throws InterruptedException {
        TransactionCommand cmd = new TransactionCommand(destWallet.publicKey, new BigDecimal(20), PayTypeEnum.WALLET);
        cmd.uid = srcUid; //original UID before encryption on add to repository

        given().contentType(ContentType.JSON)
                .body(cmd)
                .when().post("/p00L/tRaN5ACt")
                .then()
                .statusCode(HttpStatus.SC_OK);

        given().contentType(ContentType.JSON)
                .when().post("/M1n3/7r4N54C710nZ")
                .then()
                .statusCode(HttpStatus.SC_OK);

        //wait bus.send("blockAddedPostActions"...) to reach BlockService and persist the changes in transacted wallets
        TimeUnit.SECONDS.sleep(10);

        Wallet savedSrcWallet = walletRepository.getByKey(srcWallet.publicKey);
        Wallet savedDestWallet = walletRepository.getByKey(destWallet.publicKey);
        Wallet savedRewardWallet = walletRepository.getByKey(rewardWallet.publicKey);
        assertNotNull(savedSrcWallet);
        assertNotNull(savedDestWallet);
        assertNotNull(savedRewardWallet);
        assertEquals(new BigDecimal(80), savedSrcWallet.balance, "expected src wallet balance must match");
        assertEquals(1, savedSrcWallet.nonce, "src wallet must have ONE sent value");
        assertEquals(new BigDecimal(20), savedDestWallet.balance, "expected dest wallet balance must match");
        assertEquals(0, savedDestWallet.nonce, "dest wallet must have ZERO sent value");

        assertEquals(new BigDecimal(1), savedRewardWallet.balance, "expected reward wallet balance must match");
        assertEquals(0, savedRewardWallet.nonce, "reward wallet must have ZERO sent value");
    }


}