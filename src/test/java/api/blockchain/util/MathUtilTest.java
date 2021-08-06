package api.blockchain.util;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@QuarkusTest
public class MathUtilTest {

    Map<String, BigDecimal> amountValues;

    @BeforeEach
    void init() {
        amountValues = new HashMap<String, BigDecimal>();
        for (int i=0; i<100; i++) {
            amountValues.put("index"+i, new BigDecimal(i));
        }
    }

    @Test
    void sumAmountValuesTotal() {
        BigDecimal actualSum = MathUtil.calculateMapTotal(amountValues);
        BigDecimal expectedSum = new BigDecimal(4950);
        assertTrue(amountValues.size()==100);
        assertEquals(expectedSum, actualSum, "must sum all elements in the map");
    }

    //TODO create a test with a real transactionMap use case

}
