package api.cryptocurrency.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 *
 * Wrapper for Wallet to ensure to not
 * expose any sensetive data in the endpoint.
 */
@RegisterForReflection
public class WalletInfo {

    public String address;
    public BigDecimal balance;

}
