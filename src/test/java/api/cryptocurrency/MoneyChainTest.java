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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@QuarkusTest
@TestProfile(RewardMinerTestProfile.class)
public class MoneyChainTest extends BaseTest {

    //private static final Logger LOGGER = Logger.getLogger("MoneyChainTest");

    //FIXME https://git.okaut.com/Jesus/api.blockchain/-/issues/14#note_6718
    BigDecimal totalAmount = new BigDecimal(10000L); //Error in Kafka channel with bigger values
    BigDecimal walletsAmount = BigDecimal.ZERO;
    Long totalWallets = 100L;
    Long minerInterationCount = 0L;
    Long transactionNonce = 0L;

    @Inject
    WalletRepository walletRepository;

    @Inject
    TransactionMiner miner;

    String srcUid;
    Wallet srcWallet;
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

        Wallet destWallet;
        for (long i = 1L; i <= totalWallets; i++) {
            destWallet = new Wallet();
            destKeys.add(destWallet.publicKey);
            walletRepository.add(destWallet);
        }
    }

    @Test
    //by the end of this test the total money in the chain must be equal to the original totalAmount initial specified
    public void expendTotalChainMoney() throws InterruptedException {
        assertEquals(totalWallets, destKeys.size(), "there must have the right number of wallets to test");
        long leftLimit = 0L;
        long rightLimit = destKeys.size() - 1;
        Long generatedLong = leftLimit + (long) (Math.random() * (rightLimit - leftLimit));

        TransactionCommand cmd = new TransactionCommand();
        cmd.type = PayTypeEnum.WALLET;
        cmd.uid = srcUid;
        try {
            while (totalAmount.compareTo(BigDecimal.ZERO)>0) { //There still money to expend
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
                if (miner.mineTransactions()) {
                    minerInterationCount++;
                    transactionNonce++;
                }
            }
            //let's ensure one last running of the miner to guarantee there is no left transactions behind in the pool
            if (miner.mineTransactions()) {
                minerInterationCount++;
                transactionNonce++;
            }
        } catch (RuntimeException re) { //Means no more transaction to mine, all good
            assertEquals("no valid transactions found", re.getMessage());
        }

        assertEquals(BigDecimal.ZERO, totalAmount, "the origin wallet must be zero");
        assertEquals(new BigDecimal(10000L), walletsAmount,
                "the destination wallets must have received all the money from the origin wallet");

        //wait bus.send("blockAddedPostActions"...) to reach BlockService and persist the changes in transacted wallets
        TimeUnit.SECONDS.sleep(3);

        Wallet savedRewardWallet = walletRepository.getByKey(rewardWallet.publicKey);
        assertEquals(new BigDecimal(minerInterationCount), savedRewardWallet.balance,
                "reward wallet must have the right amount as: +1 per miner interactions");
        assertEquals(0, savedRewardWallet.nonce,
                "reward wallet must have not sent any transaction value");

        Wallet savedSrcWallet = walletRepository.getByKey(srcWallet.publicKey);
        assertEquals(transactionNonce, savedSrcWallet.nonce,
                "src wallet must have the right amount of executed transactions");

        Wallet savedDestWallet;
        String walletKey;
        for (Long i = 0L; i <= totalWallets-1; i++) {
            walletKey = destKeys.get(i.intValue());
            savedDestWallet = walletRepository.getByKey(walletKey);
            assertEquals(0, savedDestWallet.nonce,
                    "destination wallets shouldn't have transacted");
        }
    }
}