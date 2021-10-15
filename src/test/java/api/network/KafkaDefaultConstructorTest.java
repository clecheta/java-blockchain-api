package api.network;

import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Input;
import com.R07r35cInc.cryptocurrency.Transaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * In order to broadcast an object in a kafka channel it must have a public constructor
 *
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
public class KafkaDefaultConstructorTest {

    @Test
    void blockHasDefaultConstructor() {
        Object block = new Block();
        assertNotNull(block);
        assertTrue(block instanceof com.R07r35cInc.blockchain.Block);
    }

    @Test
    void transactionHasDefaultConstructor() {
        Transaction transaction = new Transaction();
        assertNotNull(transaction);
        assertTrue(transaction instanceof com.R07r35cInc.cryptocurrency.Transaction);
    }

    @Test
    void inputHasDefaultConstructor() {
        Object input = new Input();
        assertNotNull(input);
        assertTrue(input instanceof com.R07r35cInc.cryptocurrency.Input);
    }

}
