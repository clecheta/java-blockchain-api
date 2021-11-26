package api.repository;

import api.repository.model.ChainTypeEnum;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Transaction;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

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

    @Inject
    MongoClient mongoClient;

    public void loadBlockchain() {
        //List<Block> blockDatabaseList = list("chain", ChainTypeEnum.GENERIC.getValue());
        System.out.println("load blockchain");
    }

    public List<Block<List<Transaction>>> loadCryptochain() {
        System.out.println("load cryptochain");
        List<Block<List<Transaction>>> list = new ArrayList<>();
        MongoCursor<Block> cursor = getCollection().find().iterator();

        try {
            while (cursor.hasNext()) {
                list.add(cursor.next());
            }
        } finally {
            cursor.close();
        }

        if (list.size()>0) { //add the Genesis block to begin of chain
            list.add(0, Block.genesis());
        }

        return list;
    }

    public void add(Block block, ChainTypeEnum chainType) {
        block.setChain(chainType.getValue());
        getCollection().insertOne(block);
    }

    private MongoCollection<Block> getCollection() {
        return mongoClient.getDatabase("blockchain").getCollection("block", Block.class);
    }

}
