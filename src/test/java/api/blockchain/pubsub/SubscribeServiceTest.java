package api.blockchain.pubsub;

import api.blockchain.model.ChainMessage;
import api.blockchain.util.NetUtil;
import com.R07r35cInc.Block;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class SubscribeServiceTest {

    @Inject
    SubscribeService subscriber;

    @Test
    void isValidMessage() {
        String master = NetUtil.getHostName();
        String node = "node";

        ChainMessage message = new ChainMessage();
        assertFalse(subscriber.isValidMessage(message), "missing required fields");
        message.from = master;
        assertFalse(subscriber.isValidMessage(message), "missing chain");
        message.chain = new ArrayList<Block>();
        assertFalse(subscriber.isValidMessage(message), "is hostname different?");

        message.from = node;
        assertTrue(subscriber.isValidMessage(message), "is hostname different?");
    }

}