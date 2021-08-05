package cryptocurrency;

import api.blockchain.util.EcUtil;
import com.google.gson.GsonBuilder;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
public class Transaction {

    private static final Logger LOGGER = Logger.getLogger("Transaction");

    private UUID id;
    private Map<String, BigDecimal> outputMap;
    private Input input;

    /** Only wallets should be able to create transactions */
    public Transaction(Wallet senderWallet, String recipient, BigDecimal amount) {
        this.id = UUID.randomUUID();
        //use outputMap so that we can have more than one recipient in the transaction
        this.outputMap = createOutputMap(senderWallet, recipient, amount);
        this.input = createInput(senderWallet, this.outputMap);
    }

    private Map createOutputMap(Wallet senderWallet, String recipient, BigDecimal amount) {
        Map<String, BigDecimal> outputMap = new HashMap<String, BigDecimal>();

        outputMap.put(recipient, amount);
        outputMap.put(senderWallet.publicKey, senderWallet.balance.subtract(amount));

        return outputMap;
    }

    private Input createInput(Wallet senderWallet, Map<String, BigDecimal>outputMap) {
        return new Input(new Date(),
                senderWallet.balance,
                senderWallet.publicKey,
                senderWallet.sign(outputMap));
    }

    /** Validate if the input amount is equal to the total values contained in the outputMap */
    public static boolean isValidTransaction(Transaction transaction) {
        String address = transaction.getInput().address;
        BigDecimal amount = transaction.getInput().amount;
        String signature = transaction.getInput().signature;
        Map<String, BigDecimal> outputMap = transaction.outputMap;

        BigDecimal outputTotal = BigDecimal.ZERO;
        Iterator<Map.Entry<String, BigDecimal>> iterator = outputMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, BigDecimal> entry = iterator.next();
            outputTotal = outputTotal.add(entry.getValue());
        }

        if (amount.compareTo(outputTotal) != 0) {
            LOGGER.error("Invalid transaction from address: " + address);
            return false;
        }

        EcUtil ec = new EcUtil();
        String serializedData = new GsonBuilder().create().toJson(outputMap);
        if (!ec.verifySignature(address, serializedData, signature)) {
            LOGGER.error("Invalid signature from address: " + address);
            return false;
        }

        return true;
    }

    public void update(Wallet senderWallet, String recipient, BigDecimal amount) {
        BigDecimal senderBalance = this.outputMap.get(senderWallet.publicKey);
        if (amount.compareTo(senderBalance)==1) {
            throw new RuntimeException("Amount exceeds balance");
        }
        this.outputMap.put(recipient, amount);

        BigDecimal newAmount = this.outputMap.get(senderWallet.publicKey).subtract(amount);
        this.outputMap.put(senderWallet.publicKey, newAmount);

        this.input = this.createInput(senderWallet, this.outputMap);
    }

    public UUID getId() {
        return id;
    }

    public Map<String, BigDecimal> getOutputMap() {
        return outputMap;
    }

    public Input getInput() {
        return this.input;
    }
}