package cryptocurrency;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
public class Input {
    public Date date;
    public BigDecimal amount;
    public String address;
    public String signature;

    public Input() {}

    public Input(Date date, BigDecimal amount, String address, String signature) {
        this.date = date;
        this.amount = amount;
        this.address = address;
        this.signature = signature;
    }

    //TODO: Make test for equals() and hasCode() comparison implementation

    //FIXME: use data in the comparison breaks RewardTransactionTest but it is a good thing do have date in this comparison

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Input)) return false;
        Input input = (Input) o;
        return Objects.equals(amount, input.amount) && Objects.equals(address, input.address) && Objects.equals(signature, input.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, address, signature);
    }
}
