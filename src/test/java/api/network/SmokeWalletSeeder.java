package api.network;

import com.R07r35cInc.cryptocurrency.Wallet;
import com.R07r35cInc.util.Parse;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import api.repository.WalletCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import java.math.BigDecimal;

/**
 * Seeds a funded wallet into local Mongo for multi-node HTTP smoke.
 * HTTP create always starts at balance 0 — smoke needs a funded source.
 *
 * Usage (from java-blockchain-api):
 * {@code ./mvnw -q exec:java -Dexec.classpathScope=test -Dexec.mainClass=api.network.SmokeWalletSeeder}
 *
 * Prints one JSON line: {@code {"uid":"...","address":"...","balance":10000}}
 */
public class SmokeWalletSeeder {

    public static void main(String[] args) {
        String mongoUri = System.getProperty("quarkus.mongodb.connection-string",
                System.getenv().getOrDefault("MONGO_URI", "mongodb://localhost:27017"));
        BigDecimal amount = new BigDecimal(System.getProperty("smoke.balance", "10000"));

        CodecRegistry registry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(new WalletCodecProvider())
        );

        try (MongoClient client = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new com.mongodb.ConnectionString(mongoUri))
                .codecRegistry(registry)
                .build())) {
            Wallet wallet = new Wallet(amount);
            String originalUid = wallet.uid;
            wallet.uid = Parse.toMD5(originalUid);

            MongoCollection<Wallet> collection = client.getDatabase("blockchain")
                    .getCollection("wallet", Wallet.class);
            collection.insertOne(wallet);

            System.out.printf(
                    "{\"uid\":\"%s\",\"address\":\"%s\",\"balance\":%s}%n",
                    originalUid,
                    wallet.publicKey,
                    amount.toPlainString());
        }
    }
}
