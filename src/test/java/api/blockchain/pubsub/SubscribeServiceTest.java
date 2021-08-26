package api.blockchain.pubsub;

import api.blockchain.model.BlockchainMessage;
import api.util.Network;
import com.R07r35cInc.blockchain.Block;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@QuarkusTest
public class SubscribeServiceTest {

    @Inject
    SubscribeService subscriber;

    @Test
    void isValidMessage() {
        String master = Network.getHostName();
        String node = "node";

        BlockchainMessage message = new BlockchainMessage();
        assertFalse(subscriber.isValidMessage(message), "missing required fields");
        message.from = master;
        assertFalse(subscriber.isValidMessage(message), "missing chain");
        message.chain = new ArrayList<Block>();
        assertFalse(subscriber.isValidMessage(message), "is hostname different?");

        message.from = node;
        assertTrue(subscriber.isValidMessage(message), "is hostname different?");
    }

}