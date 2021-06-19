package api.blockchain.model;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class ChainMessageTest {

    @Test
    void testFields() {
        Field[] fields = ChainMessage.class.getDeclaredFields();
        assertTrue(fields.length == 2, "class fields violated");
        for (Field f : fields) {
            if (f.getName().equals("node")) {
                assertEquals(0, f.toGenericString().indexOf("private"));
                assertEquals(true, f.toGenericString().contains("String"));
                break;
            } else if (f.getName().equals("blockList")) {
                assertEquals(0, f.toGenericString().indexOf("private"));
                assertEquals(true, f.toGenericString().contains("List<Block>"));
            }
        }
    }

    @Test
    public void testDefaultConstructor() {
        ChainMessage message = new ChainMessage();
        assertEquals(message.getNode(), null);
        assertEquals(message.getBlockList(), null);
    }

}