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

import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
@Order(1)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MoneyFlowTest extends BaseTest {

    @Inject
    WalletRepository walletRepository;

    @Inject
    TransactionMiner miner;

    //FIXME https://git.okaut.com/Jesus/api.blockchain/-/issues/14#note_6718
    BigDecimal totalAmount = new BigDecimal(10000L); //Error in Kafka channel with bigger values
    BigDecimal walletsAmount = BigDecimal.ZERO;
    Long totalWallets = 100L;
    Long minerInterationCount = 0L;

    String srcUid;
    Wallet srcWallet;
    Wallet destWallet;
    Wallet rewardWallet;
    List<String> destKeys = new ArrayList<>();

    @BeforeEach
    void init() {
        //Creates the origin wallet with the total amount to be distributed in the whole chain history/existence
        srcWallet = new Wallet(totalAmount);
        srcUid = srcWallet.uid;
        walletRepository.add(srcWallet);

        //Creates the reward wallet that will receive the miner recompense and assign it to the miner
        rewardWallet = new Wallet();
        walletRepository.add(rewardWallet);
        miner.rewardWallet = rewardWallet; //we must ensure that reward wallet is the one Miner will work with
    }

    @Test
    @Order(1)
    //by the end of this test the origin wallet must have 80, destination 20, and reward 1
    public void walletToWalletSingleTransaction() throws InterruptedException {
        destWallet = new Wallet();
        walletRepository.add(destWallet);

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

        Wallet savedSrcWallet = walletRepository.getByKey(srcWallet.publicKey);
        Wallet savedDestWallet = walletRepository.getByKey(destWallet.publicKey);
        Wallet savedRewardWallet = walletRepository.getByKey(rewardWallet.publicKey);
        assertNotNull(savedSrcWallet);
        assertNotNull(savedDestWallet);
        assertNotNull(savedRewardWallet);
        assertEquals(new BigDecimal(9980), savedSrcWallet.balance, "expected src wallet balance must match");
        assertEquals(1, savedSrcWallet.nonce, "src wallet must have ONE sent value");
        assertEquals(new BigDecimal(20), savedDestWallet.balance, "expected dest wallet balance must match");
        assertEquals(0, savedDestWallet.nonce, "dest wallet must have ZERO sent value");

        assertEquals(new BigDecimal(1), savedRewardWallet.balance, "expected reward wallet balance must match");
        assertEquals(0, savedRewardWallet.nonce, "reward wallet must have ZERO sent value");
    }

    @Test
    @Order(2)
    //by the end of this test the total money in the chain must be equal to the original totalAmount initial specified
    public void expendTotalChainMoney() throws InterruptedException {
        miner.transactionPool.clear(); //release any previous test (not mined) transaction
        Wallet destWallet;
        for (long i = 1L; i <= totalWallets; i++) {
            destWallet = new Wallet();
            destKeys.add(destWallet.publicKey);
            walletRepository.add(destWallet);
        }

        assertEquals(totalWallets, destKeys.size(), "there must have the right number of wallets to test");
        long leftLimit = 0L;
        long rightLimit = destKeys.size() - 1;
        Long generatedLong = leftLimit + (long) (Math.random() * (rightLimit - leftLimit));

        TransactionCommand cmd = new TransactionCommand();
        cmd.type = PayTypeEnum.WALLET;
        cmd.uid = srcUid;
        try {
            while (totalAmount.compareTo(BigDecimal.ZERO) > 0) { //There still money to expend
                for (int i = 0; i < 10; i++) { //Let's create one block per 10 transactions
                    cmd.recipient = destKeys.get(generatedLong.intValue());
                    cmd.amount = new BigDecimal(100);
                    given().contentType(ContentType.JSON)
                            .body(cmd)
                            .when().post("/p00L/tRaN5ACt")
                            .then()
                            .statusCode(HttpStatus.SC_OK);
                    walletsAmount = walletsAmount.add(cmd.amount);
                    totalAmount = totalAmount.subtract(cmd.amount);
                }
                if (miner.mineTransactions() != null) {
                    minerInterationCount++;
                }
            }
            //let's ensure one last running of the miner to guarantee there is no left transactions behind in the pool
            if (miner.mineTransactions() != null) {
                minerInterationCount++;
            }
        } catch (RuntimeException re) { //Means no more transaction to mine, all good
            assertEquals("no valid transactions found", re.getMessage());
        }

        assertEquals(BigDecimal.ZERO, totalAmount, "the origin wallet must be zero");
        assertEquals(new BigDecimal(10000L), walletsAmount,
                "the destination wallets must have received all the money from the origin wallet");

        Wallet savedRewardWallet = walletRepository.getByKey(rewardWallet.publicKey);
        assertEquals(new BigDecimal(minerInterationCount), savedRewardWallet.balance,
                "reward wallet must have the right amount as: +1 per miner interactions");
        assertEquals(0, savedRewardWallet.nonce,
                "reward wallet must have not sent any transaction value");

        Wallet savedSrcWallet = walletRepository.getByKey(srcWallet.publicKey);
        assertEquals(totalWallets, savedSrcWallet.nonce,
                "src wallet must have the right amount of executed transactions");

        Wallet savedDestWallet;
        String walletKey;
        for (Long i = 0L; i <= totalWallets - 1; i++) {
            walletKey = destKeys.get(i.intValue());
            savedDestWallet = walletRepository.getByKey(walletKey);
            assertEquals(0, savedDestWallet.nonce,
                    "destination wallets shouldn't have transacted");
        }
    }

}