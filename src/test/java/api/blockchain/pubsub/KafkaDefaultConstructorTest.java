package api.blockchain.pubsub;

import com.R07r35cInc.Block;
import cryptocurrency.Input;
import cryptocurrency.Transaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * In order to broadcast an object in a kafka channel it must have a public constructor
 *
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
public class KafkaDefaultConstructorTest {

    @Test
    void blockHasDefaultConstructor() {
        Block block = new Block();
        assertNotNull(block);
        assertTrue(block instanceof com.R07r35cInc.Block);
    }

    @Test
    void transactionHasDefaultConstructor() {
        Transaction transaction = new Transaction();
        assertNotNull(transaction);
        assertTrue(transaction instanceof cryptocurrency.Transaction);
    }

    @Test
    void inputHasDefaultConstructor() {
        Input input = new Input();
        assertNotNull(input);
        assertTrue(input instanceof cryptocurrency.Input);
    }

}
