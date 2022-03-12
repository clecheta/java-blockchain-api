package api.repository;

import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Wallet;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@ApplicationScoped
public class DatabaseRepository {

    private static final Logger LOGGER = Logger.getLogger("DatabaseIndex");
    private static final String DATABASE = "blockchain";

    @Inject
    MongoClient mongoClient;

    public void createIndex() {
        MongoDatabase database = mongoClient.getDatabase(DATABASE);

        //Wallet
        int count = 0;
        for (Document index : getWalletCollection().listIndexes()) {
            count++;
        }
        if (count <= 1) {
            IndexOptions options = new IndexOptions().unique(true);
            getWalletCollection().createIndex(Indexes.ascending("a0"), options); //uid
            BasicDBObject publicKeyIndex = new BasicDBObject("d7", 1);
            getWalletCollection().createIndex(publicKeyIndex, options);
            BasicDBObject privateKeyIndex = new BasicDBObject("e5", 1);
            getWalletCollection().createIndex(privateKeyIndex, options);
        }

        //Block
        count = 0;
        for (Document index : getBlockCollection().listIndexes()) {
            count++;
        }
        if (count <= 1) {
            getBlockCollection().createIndex(Indexes.ascending("number"));
            getBlockCollection().createIndex(Indexes.descending("date"));
            getBlockCollection().createIndex(Indexes.ascending("hash"));
        }
    }

    private MongoCollection<Block> getBlockCollection() {
        return mongoClient.getDatabase(DATABASE).getCollection("block", Block.class);
    }

    private MongoCollection<Wallet> getWalletCollection() {
        return mongoClient.getDatabase(DATABASE).getCollection("wallet", Wallet.class);
    }
}
