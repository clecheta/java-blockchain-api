package cryptocurrency;

import org.apache.kafka.common.protocol.types.Field;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
public class Transaction {

    public UUID id;
    public Map<String, BigDecimal> outputMap;

    /** Only wallets should be able to create transactions */
    public Transaction(Wallet senderWallet, String recipient, BigDecimal amount) {
        this.id = UUID.randomUUID();
        //use outputMap so that we can have more than one recipient in the transaction
        this.outputMap = createOutputMap(senderWallet, recipient, amount);
    }

    private Map createOutputMap(Wallet senderWallet, String recipient, BigDecimal amount) {
        Map<String, BigDecimal> outputMap = new HashMap<String, BigDecimal>();

        outputMap.put(recipient, amount);
        outputMap.put(senderWallet.publicKey, senderWallet.balance.subtract(amount));

        return outputMap;
    }

}
