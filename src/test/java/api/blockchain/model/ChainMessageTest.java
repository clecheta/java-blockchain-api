package api.blockchain.model;

import api.util.Network;
import com.R07r35cInc.blockchain.Block;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@QuarkusTest
public class ChainMessageTest {

    @Test
    void testFields() {
        Field[] fields = ChainMessage.class.getDeclaredFields();
        assertTrue(fields.length == 1, "class fields violated");
        assertEquals(true, Arrays.stream(fields).anyMatch(f -> f.getName().equals("from")));
    }

    @Test
    void isValidMessage() {
        String master = Network.getHostName();
        String node = "node";

        BlockchainMessage message = new BlockchainMessage();
        assertFalse(message.isValid(), "missing required fields");
        message.from = master;
        assertFalse(message.isValid(), "missing chain");
        message.chain = new ArrayList<Block>();
        assertFalse(message.isValid(), "is hostname different?");

        message.from = node;
        assertTrue(message.isValid(), "is hostname different?");
    }

}