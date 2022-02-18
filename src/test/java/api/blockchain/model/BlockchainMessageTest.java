package api.blockchain.model;

import com.R07r35cInc.blockchain.NetChain;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@QuarkusTest
public class BlockchainMessageTest {

    @Test
    void testFields() {
        Field[] fields = NetChain.class.getDeclaredFields();
        assertEquals(true, Arrays.stream(fields).anyMatch(f -> f.getName().equals("origin")));
    }

    @Test
    void testMethods() {
        Method[] methods = NetChain.class.getDeclaredMethods();
        assertEquals(true, Arrays.stream(methods).anyMatch(m -> m.getName().equals("isValidForReplacement")));
    }

}