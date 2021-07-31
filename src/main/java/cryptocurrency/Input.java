package cryptocurrency;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
public class Input {
    public Date date;
    public BigDecimal amount;
    public String address;
    public String signature;

    public Input(Date date, BigDecimal amount, String address, String signature) {
        this.date = date;
        this.amount = amount;
        this.address = address;
        this.signature = signature;
    }
}
