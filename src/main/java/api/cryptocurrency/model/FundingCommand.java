package api.cryptocurrency.model;

import com.R07r35cInc.cryptocurrency.LosslessBigDecimalDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;

/**
 * @author Cleber Lecheta
 * https://gitlab.com/cleberlecheta/java-blockchain-api
 */
public class FundingCommand {
    public String uid;
    public String idempotencyKey;

    @JsonDeserialize(using = LosslessBigDecimalDeserializer.class)
    public BigDecimal amount;
}
