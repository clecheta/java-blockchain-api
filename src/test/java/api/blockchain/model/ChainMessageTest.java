package api.blockchain.model;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@QuarkusTest
public class ChainMessageTest {

    @Test
    void testFields() {
        Field[] fields = ChainMessage.class.getDeclaredFields();
        assertTrue(fields.length == 2, "class fields violated");
        for (Field f : fields) {
            if (f.getName().equals("from")) {
                assertEquals(0, f.toGenericString().indexOf("public"));
                assertEquals(true, f.toGenericString().contains("String"));
                break;
            } else if (f.getName().equals("chain")) {
                assertEquals(0, f.toGenericString().indexOf("public"));
                assertEquals(true, f.toGenericString().contains("List<Block>"));
            }
        }
    }

    @Test
    public void testDefaultConstructor() {
        ChainMessage message = new ChainMessage();
        assertEquals(message.from, null);
        assertEquals(message.chain, null);
    }

    @Test
    void testMethods() {
        Method[] methods = ChainMessage.class.getDeclaredMethods();
        assertTrue(methods.length == 1, "this class should not have any method");
    }

}