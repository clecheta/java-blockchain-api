package api.blockchain.util;

import com.google.gson.GsonBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@QuarkusTest
public class EcUtilTest {

    static EcUtil ec;
    String data = "fuba";

    @BeforeAll
    public static void init() {
        ec = new EcUtil();
        ec.genKeyPair();
    }

    @Test
    void getPublic() {
        String publicKey = null;
        publicKey = ec.getPublic();
        assertNotNull(publicKey);
    }

    @Test
    void signAndVerify() {
        String signature = null;
        signature = ec.sign(data);
        assertNotNull(signature);

        Boolean verifiedMustBeTrue = ec.verifySignature(ec.getPublic(), data, signature);
        assertTrue(verifiedMustBeTrue);
    }

    @Test
    void signAndVerifyWithSerializedData() {
        String serializedData = new GsonBuilder().create().toJson(data);

        String signature = null;
        signature = ec.sign(serializedData);
        assertNotNull(signature);

        Boolean verifiedMustBeTrue = ec.verifySignature(ec.getPublic(), serializedData, signature);
        assertTrue(verifiedMustBeTrue);
    }

}
