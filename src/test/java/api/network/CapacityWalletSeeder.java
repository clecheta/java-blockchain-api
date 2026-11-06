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
import java.util.ArrayList;
import java.util.List;

/**
 * Batch-seeds funded wallets for capacity Suite A/C.
 * HTTP {@code POST /W4LL37/cRE47E} always starts at balance 0.
 *
 * Usage:
 * {@code ./mvnw -q exec:java -Dexec.classpathScope=test -Dexec.mainClass=api.network.CapacityWalletSeeder
 *   -Dcapacity.count=2 -Dcapacity.balance=10000}
 *
 * Prints a JSON array of {@code {uid,address,balance}}.
 */
public class CapacityWalletSeeder {

    public static void main(String[] args) {
        String mongoUri = System.getProperty("quarkus.mongodb.connection-string",
                System.getenv().getOrDefault("MONGO_URI", "mongodb://localhost:27017"));
        int count = Integer.parseInt(System.getProperty("capacity.count", "1"));
        BigDecimal balance = new BigDecimal(System.getProperty("capacity.balance", "10000"));

        CodecRegistry registry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(new WalletCodecProvider())
        );

        List<String> items = new ArrayList<>();
        try (MongoClient client = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new com.mongodb.ConnectionString(mongoUri))
                .codecRegistry(registry)
                .build())) {
            MongoCollection<Wallet> collection = client.getDatabase("blockchain")
                    .getCollection("wallet", Wallet.class);
            for (int i = 0; i < count; i++) {
                Wallet wallet = new Wallet(balance);
                String originalUid = wallet.uid;
                wallet.uid = Parse.toMD5(originalUid);
                collection.insertOne(wallet);
                items.add(String.format(
                        "{\"uid\":\"%s\",\"address\":\"%s\",\"balance\":%s}",
                        originalUid,
                        wallet.publicKey,
                        balance.toPlainString()));
            }
        }
        System.out.printf("[%s]%n", String.join(",", items));
    }
}
