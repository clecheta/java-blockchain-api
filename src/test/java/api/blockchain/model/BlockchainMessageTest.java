package api.blockchain.model;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@QuarkusTest
public class BlockchainMessageTest {

    @Test
    void testFields() {
        Field[] fields = BlockchainMessage.class.getDeclaredFields();
        assertTrue(fields.length == 1, "class fields violated");
        assertEquals(true, Arrays.stream(fields).anyMatch(f -> f.getName().equals("chain")));
    }

    @Test
    public void testDefaultConstructor() {
        BlockchainMessage message = new BlockchainMessage();
        assertEquals(message.from, null);
        assertEquals(message.chain, null);
    }

    @Test
    void testMethods() {
        Method[] methods = BlockchainMessage.class.getDeclaredMethods();
        assertTrue(methods.length == 2, "this class should implement getChain()");
    }

}