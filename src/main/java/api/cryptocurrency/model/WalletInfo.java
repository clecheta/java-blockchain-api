package api.cryptocurrency.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@RegisterForReflection
public class WalletInfo {

    public String address;
    public BigDecimal balance;

}
