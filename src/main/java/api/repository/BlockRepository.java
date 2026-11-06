package api.repository;

import api.repository.model.ChainTypeEnum;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Tx;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@ApplicationScoped
public class BlockRepository {

    private static final Logger LOGGER = Logger.getLogger("BlockRepository");
    private static final String DATABASE = "blockchain";
    private static final String COLLECTION = "block";

    @Inject
    MongoClient mongoClient;

    public List<Block> loadBlockchain() {
        List<Block> list = new ArrayList<>();
        list.add(0, Block.genesis()); //add the Genesis block to begin of chain

        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("chain", ChainTypeEnum.GENERIC.getValue());
        MongoCursor<Block> cursor = getCollection().find(whereQuery).sort(Sorts.ascending("number")).iterator();
        try {
            while (cursor.hasNext()) {
                list.add(cursor.next());
            }
        } finally {
            cursor.close();
        }
        LOGGER.debug("load: Blockchain");
        return list;
    }

    public List<Block<List<Tx>>> loadCryptochain() {
        List<Block<List<Tx>>> list = new ArrayList<>();
        list.add(0, Block.genesis()); //add the Genesis block to begin of chain

        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("chain", ChainTypeEnum.CRYPTO.getValue());
        MongoCursor<Block> cursor = getCollection().find(whereQuery).sort(Sorts.ascending("number")).iterator();
        try {
            while (cursor.hasNext()) {
                list.add(cursor.next());
            }
        } finally {
            cursor.close();
        }
        LOGGER.debug("load: Cryptochain");

        return list;
    }

    /**
     * Load generic blocks with number &gt;= fromInclusive (no genesis prepend).
     * Used by peer catch-up after a Kafka gap.
     */
    public List<Block> loadBlockchainFromNumber(long fromInclusive) {
        List<Block> list = new ArrayList<>();
        MongoCursor<Block> cursor = getCollection()
                .find(Filters.and(
                        Filters.eq("chain", ChainTypeEnum.GENERIC.getValue()),
                        Filters.gte("number", fromInclusive)))
                .sort(Sorts.ascending("number"))
                .iterator();
        try {
            while (cursor.hasNext()) {
                list.add(cursor.next());
            }
        } finally {
            cursor.close();
        }
        LOGGER.debugf("load: Blockchain from number %d (%d blocks)", fromInclusive, list.size());
        return list;
    }

    /**
     * Load crypto blocks with number &gt;= fromInclusive (no genesis prepend).
     */
    public List<Block<List<Tx>>> loadCryptochainFromNumber(long fromInclusive) {
        List<Block<List<Tx>>> list = new ArrayList<>();
        MongoCursor<Block> cursor = getCollection()
                .find(Filters.and(
                        Filters.eq("chain", ChainTypeEnum.CRYPTO.getValue()),
                        Filters.gte("number", fromInclusive)))
                .sort(Sorts.ascending("number"))
                .iterator();
        try {
            while (cursor.hasNext()) {
                list.add(cursor.next());
            }
        } finally {
            cursor.close();
        }
        LOGGER.debugf("load: Cryptochain from number %d (%d blocks)", fromInclusive, list.size());
        return list;
    }

    public void add(Block block, ChainTypeEnum chainType) {
        block.setChain(chainType.getValue());
        getCollection().insertOne(block);
    }

    private MongoCollection<Block> getCollection() {
        return mongoClient.getDatabase(DATABASE).getCollection(COLLECTION, Block.class);
    }

}
