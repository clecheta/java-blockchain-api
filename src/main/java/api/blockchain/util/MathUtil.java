package api.blockchain.util;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
public class MathUtil {

    public static BigDecimal calculateMapTotal(Map<String, BigDecimal> map) {
        BigDecimal outputTotal = BigDecimal.ZERO;
        Iterator<Map.Entry<String, BigDecimal>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, BigDecimal> entry = iterator.next();
            outputTotal = outputTotal.add(entry.getValue());
        }
        return outputTotal;
    }

}
