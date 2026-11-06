package api.network;

import api.network.model.CryptoBlockMessage;
import api.network.model.CryptoBlockMessageDeserializer;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.blockchain.CryptoChain;
import com.R07r35cInc.cryptocurrency.Transaction;
import com.R07r35cInc.cryptocurrency.Tx;
import com.R07r35cInc.cryptocurrency.Wallet;
import com.R07r35cInc.util.CryptoHash;
import com.R07r35cInc.util.Parse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Locks api#40 SerDes: Block.data must remain List&lt;Tx&gt; after Kafka ObjectMapper round-trip,
 * and recalculated hash must match the stored hash.
 */
public class CryptoBlockMessageSerDesTest {

    CryptoChain cryptoChain;
    Block<List<Tx>> minedBlock;

    @BeforeEach
    void init() {
        cryptoChain = new CryptoChain();
        Wallet sender = new Wallet();
        Transaction tx = new Transaction(sender, "recipient-pub", new BigDecimal("10"));
        List<Tx> txs = new ArrayList<>();
        txs.add(tx);
        minedBlock = cryptoChain.addBlock(txs);
    }

    @Test
    void roundTripKeepsTxTypeAndHash() {
        CryptoBlockMessage original = new CryptoBlockMessage("root-host", minedBlock, 1L);

        ObjectMapperSerializer<CryptoBlockMessage> serializer = new ObjectMapperSerializer<>();
        byte[] bytes = serializer.serialize("cryptochain", original);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        // Incremental payload must be far smaller than a multi-block full chain dump
        assertTrue(bytes.length < 50_000, "single-block message should stay small");

        CryptoBlockMessageDeserializer deserializer = new CryptoBlockMessageDeserializer();
        CryptoBlockMessage restored = deserializer.deserialize("cryptochain", bytes);

        assertNotNull(restored);
        assertEquals("root-host", restored.origin);
        assertEquals(1L, restored.senderHeight);
        assertNotNull(restored.block);
        assertNotNull(restored.block.getData());
        assertFalse(restored.block.getData().isEmpty());

        Object first = restored.block.getData().get(0);
        assertFalse(first instanceof LinkedHashMap || first instanceof Map,
                "data[0] must not be a raw Map after deserialize");
        assertTrue(first instanceof Tx, "data[0] must be Tx");

        String originalHash = minedBlock.getHash();
        String restoredHash = CryptoHash.calculateHash(restored.block);
        assertEquals(originalHash, restoredHash, "hash must survive typed Kafka SerDes");

        String originalJson = Parse.toJson(minedBlock.getData());
        String restoredJson = Parse.toJson(restored.block.getData());
        assertEquals(originalJson, restoredJson, "tx JSON must match for hash stability");
    }

    @Test
    void objectMapperTypeReferencePreservesBlock() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(minedBlock);
        Block<List<Tx>> parsed = mapper.readValue(json,
                new com.fasterxml.jackson.core.type.TypeReference<Block<List<Tx>>>() {
                });
        assertEquals(CryptoHash.calculateHash(minedBlock), CryptoHash.calculateHash(parsed));
        assertTrue(parsed.getData().get(0) instanceof Tx);
    }
}
