package cryptocurrency;

import api.blockchain.util.EcUtil;
import com.R07r35cInc.CryptoHash;
import com.google.gson.GsonBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.Signature;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
public class Wallet {
    private EcUtil ec; //utility class to work with ECDSA algorithms

    @ConfigProperty(name = "starting.balance")
    private int STARTING_BALANCE;
    public BigDecimal balance;

    private KeyPair keyPair;
    public String publicKey;

    Wallet() {
        ec = new EcUtil();
        this.balance = new BigDecimal(STARTING_BALANCE);
        this.keyPair = ec.genKeyPair();
        this.publicKey = ec.getPublic();
    }

    /**
     * Generate Signatures:
     * verify the signature of other wallets: we don't want to accept any transactions
     * unless the actual underling wallet gave a proper signature with its private key.
     */
    public String sign(Object data) {
        String serializedData = new GsonBuilder().create().toJson(data);
        String signature = ec.sign(serializedData);
        return signature;
    }

}
