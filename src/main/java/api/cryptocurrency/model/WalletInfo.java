package api.cryptocurrency.model;

import com.R07r35cInc.cryptocurrency.Wallet;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 *
 * Wrapper for Wallet to ensure to not
 * expose any sensetive data in the endpoint.
 */
@RegisterForReflection
public class WalletInfo {

    public String uid;
    public String address;
    public BigDecimal balance;

    public WalletInfo(String address, BigDecimal balance) {
        this.address = address;
        this.balance = balance;
    }

    /* New wallet just created must inform it`s id to be saved by the caller
     * all others calls should use the default constructor
    public WalletInfo(Wallet wallet) {
        this.address = wallet.publicKey;
        this.balance = wallet.balance;
        this.uid = wallet.uid;
    }*/

}
