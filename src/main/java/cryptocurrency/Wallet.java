package cryptocurrency;

import api.blockchain.util.EcUtil;
import com.google.gson.GsonBuilder;
import io.quarkus.runtime.configuration.ProfileManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.security.KeyPair;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
public class Wallet {
    public BigDecimal balance;
    public String publicKey;
    private final EcUtil ec; //utility class to work with ECDSA algorithms
    private final KeyPair keyPair;

    public static final String profile = ProfileManager.getActiveProfile();

    Wallet() {
        ec = new EcUtil();
        this.keyPair = ec.genKeyPair();
        this.publicKey = ec.getPublic();

        if (profile.startsWith("prod")) {
            this.balance = BigDecimal.ZERO;
        } else {
            this.balance = ConfigProvider.getConfig().getValue("starting.balance", BigDecimal.class);
        }
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
