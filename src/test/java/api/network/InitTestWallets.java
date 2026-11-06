package api.network;

import api.repository.WalletCodecProvider;
import com.R07r35cInc.cryptocurrency.Wallet;
import com.R07r35cInc.util.Parse;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import java.math.BigDecimal;

/**
 * Seeds local Mongo with funded wallets for manual / smoke testing.
 * HTTP {@code POST /W4LL37/cRE47E} always starts at balance 0.
 *
 * Usage (from java-blockchain-api):
 * {@code ./mvnw -q exec:java -Dexec.classpathScope=test -Dexec.mainClass=api.network.InitTestWallets}
 *
 * Prints one JSON object with alice (sender) and bob (recipient).
 */
public class InitTestWallets {

    public static void main(String[] args) {
        String mongoUri = System.getProperty("quarkus.mongodb.connection-string",
                System.getenv().getOrDefault("MONGO_URI", "mongodb://localhost:27017"));
        BigDecimal aliceBalance = new BigDecimal(System.getProperty("init.alice.balance", "10000"));
        BigDecimal bobBalance = new BigDecimal(System.getProperty("init.bob.balance", "1000"));

        CodecRegistry registry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(new WalletCodecProvider())
        );

        try (MongoClient client = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new com.mongodb.ConnectionString(mongoUri))
                .codecRegistry(registry)
                .build())) {
            MongoCollection<Wallet> collection = client.getDatabase("blockchain")
                    .getCollection("wallet", Wallet.class);

            String aliceJson = insertFunded(collection, aliceBalance);
            String bobJson = insertFunded(collection, bobBalance);

            System.out.printf("{\"alice\":%s,\"bob\":%s}%n", aliceJson, bobJson);
        }
    }

    private static String insertFunded(MongoCollection<Wallet> collection, BigDecimal amount) {
        Wallet wallet = new Wallet(amount);
        String originalUid = wallet.uid;
        wallet.uid = Parse.toMD5(originalUid);
        collection.insertOne(wallet);
        return String.format(
                "{\"uid\":\"%s\",\"address\":\"%s\",\"balance\":%s}",
                originalUid,
                wallet.publicKey,
                amount.toPlainString());
    }
}
