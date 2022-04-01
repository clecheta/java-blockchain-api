package api.repository;

import com.R07r35cInc.cryptocurrency.Wallet;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Base64;

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

    public Wallet getByUid(String uid) {
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("a0", uid);
        Wallet wallet = getCollection().find(new Document("a0", uid)).first();
        if (wallet == null) {
            throw new RuntimeException("Wallet not found!");
        }
        return wallet;
    }

    public Wallet getByKey(String key) {
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("d7", key);
        Wallet wallet = getCollection().find(new Document("d7", key)).first();
        return wallet;
    }

    public void add(Wallet wallet) {
        getCollection().insertOne(wallet);
    }

    public void update(Wallet wallet, BigDecimal currentValue) {
        //query — holds the field name and current value of that field for the document to be updated
        BasicDBObject query = new BasicDBObject();
        query.put("a0", wallet.uid);

        //newDocument — holds the new value of the field balance for the document we’re updating
        BasicDBObject newDocument = new BasicDBObject();
        newDocument.put("c3", Base64.getEncoder().encodeToString(currentValue.unscaledValue().toByteArray()));
        newDocument.put("j1", wallet.nonce); //TODO shall we encrypt this value as well?

        //updateObject — we pass in the $set operator to update the specified field.
        BasicDBObject updateObject = new BasicDBObject();
        updateObject.put("$set", newDocument);

        getCollection().updateOne(query, updateObject);
    }

    private MongoCollection<Wallet> getCollection() {
        return mongoClient.getDatabase(DATABASE).getCollection(COLLECTION, Wallet.class);
    }

}
