package api;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
public class BaseTest {
    public static final int UUID_LENGTH = 36;

    public static final String publicKey = "Bar-Jesus-pu8L1C-K3y"; //https://en.wikipedia.org/wiki/Elymas
    public static final BigDecimal tooHighAmount = new BigDecimal(9000);
    public static final String REWARD_INPUT = "*authorized-reward*";
    public static final BigDecimal REWARD_MINER = new BigDecimal(1);

    /* This is the "ideal" values to stress chain test with total amount and wallets quantity
    public static final Long TEN_TRILLION = 10000000000000L;
    public static final Long FIVE_MILLION = 5000000L;*/

    public static String getRandomString(int length) {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return generatedString;
    }
}
