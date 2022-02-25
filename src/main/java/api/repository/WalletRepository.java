package api.repository;

import com.R07r35cInc.cryptocurrency.Wallet;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@ApplicationScoped
public class WalletRepository {

    //private static final Logger LOGGER = Logger.getLogger("WalletRepository");
    private static final String DATABASE = "blockchain";
    private static final String COLLECTION = "wallet";

    @Inject
    MongoClient mongoClient;

    public Wallet get(String uid) {
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("uid", uid);
        Wallet wallet = getCollection().find(new Document("uid", uid)).first();
        return wallet;
    }

    public void add(Wallet wallet) {
        getCollection().insertOne(wallet);
    }

    private MongoCollection<Wallet> getCollection() {
        return mongoClient.getDatabase(DATABASE).getCollection(COLLECTION, Wallet.class);
    }

}
