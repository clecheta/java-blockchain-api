package api.repository;

import api.repository.model.ChainTypeEnum;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Transaction;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
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
        MongoCursor<Block> cursor = getCollection().find(whereQuery).iterator();
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

    public List<Block<List<Transaction>>> loadCryptochain() {
        List<Block<List<Transaction>>> list = new ArrayList<>();
        list.add(0, Block.genesis()); //add the Genesis block to begin of chain

        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("chain", ChainTypeEnum.CRYPTO.getValue());
        MongoCursor<Block> cursor = getCollection().find(whereQuery).iterator();
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

    public void add(Block block, ChainTypeEnum chainType) {
        block.setChain(chainType.getValue());
        getCollection().insertOne(block);
    }

    private MongoCollection<Block> getCollection() {
        return mongoClient.getDatabase(DATABASE).getCollection(COLLECTION, Block.class);
    }

}
