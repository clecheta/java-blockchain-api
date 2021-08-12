package cryptocurrency.wallet;

import api.blockchain.util.EcUtil;
import com.google.gson.GsonBuilder;
import cryptocurrency.wallet.Wallet;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@QuarkusTest
public class WalletTest {

    private static final int BASE64_LENGHT = 120;

    Wallet wallet;
    String data = "Jiu-jitsu";

    @ConfigProperty(name = "starting.balance")
    private int STARTING_BALANCE;

    @BeforeEach
    void init() {
        wallet = new Wallet();
    }

    @Test
    void hasBalance() {
        assertTrue(wallet.balance != null);
        assertTrue(wallet.balance instanceof BigDecimal);
        assertTrue(wallet.balance.compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void balanceHasRightValue() {
        BigDecimal startBalance = new BigDecimal(this.STARTING_BALANCE);
        assertTrue(startBalance.compareTo(new BigDecimal(100))==0,
                "wallet balance must have the initial configured starting balance value");
        assertEquals(startBalance, wallet.balance);
    }

    @Test
    void hasPublicKey() {
        assertTrue(wallet.publicKey != null);
        assertTrue(wallet.publicKey instanceof String);
        assertTrue(wallet.publicKey.length() == BASE64_LENGHT);
    }

    @Test
    void verifySignature() {
        String signature = wallet.sign(data); //serialize data
        EcUtil ec = new EcUtil();
        /* we must serialize data before call ec.verifySignature as sign() method on Wallet does serialize the data
        * before call the ec.sign() ang generate signature */
        String serializedData = new GsonBuilder().create().toJson(data);
        assertTrue(ec.verifySignature(wallet.publicKey, serializedData, signature));
    }

    @Test
    void doesNotVerifyInvalidSignature() {
        EcUtil ec = new EcUtil();
        String serializedData = new GsonBuilder().create().toJson(data);
        assertFalse(ec.verifySignature(wallet.publicKey, serializedData, new Wallet().sign(serializedData)));
    }

}