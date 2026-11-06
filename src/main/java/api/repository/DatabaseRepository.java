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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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

    //TODO at startup verify if the indexes that should be unique are UNIQUE indeed (check then by name).

    public void createIndex() {
        MongoDatabase database = mongoClient.getDatabase(DATABASE);

        //Wallet
        int count = 0;
        for (Document index : getWalletCollection().listIndexes()) {
            count++;
        }
        if (count < 3) { //this number must ensure the total number of indexes expected in the database
            IndexOptions options = new IndexOptions().unique(true).name("wal_1");
            getWalletCollection().createIndex(Indexes.ascending("a0"), options); //uid
            options = new IndexOptions().unique(true).name("wal_2");
            BasicDBObject publicKeyIndex = new BasicDBObject("d7", 1);
            getWalletCollection().createIndex(publicKeyIndex, options);
        }

        //Block
        count = 0;
        for (Document index : getBlockCollection().listIndexes()) {
            count++;
        }
        if (count < 4) { //this number must ensure the total number of indexes expected in the database
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
