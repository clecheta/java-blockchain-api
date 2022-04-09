package api;

import api.blockchain.Blockchain;
import api.cryptocurrency.TransactionMiner;
import api.network.Util;
import api.repository.BlockRepository;
import api.repository.DatabaseRepository;
import api.repository.WalletRepository;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Tx;
import com.R07r35cInc.util.Parse;
import com.fasterxml.jackson.core.type.TypeReference;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@ApplicationScoped
public class ApplicationLifeCycle {

    private static final Logger LOGGER = Logger.getLogger("ApplicationLifeCycle");
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(60))
            .build();

    @Inject
    Blockchain blockchain;

    @Inject
    TransactionMiner miner;

    @Inject
    BlockRepository blockRepository;

    @Inject
    WalletRepository walletRepository;

    @Inject
    DatabaseRepository databaseRepository;

    @ConfigProperty(name = "root.node.address")
    private String ROOT_NODE_ADDRESS;

    @ConfigProperty(name = "quarkus.http.port")
    private String HTTP_PORT;

    @ConfigProperty(name = "mining.reward.wallet")
    private String rewardWallet;

    @ConfigProperty(name = "mining.reward.enabled")
    private Boolean rewardEnabled;

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("Blockchain network node _" + Util.getHostName() + " is starting...");

        //load chain from database
        loadFromDatabase();

        //syncing with root state
        syncBlockchain();
        syncCryptochain();
        syncTransactionPollMap();

        databaseRepository.createIndex();
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOGGER.info("Blockchain network node _" + Util.getHostName() + " stopped.");
    }

    private void syncBlockchain() {
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(ROOT_NODE_ADDRESS + "/Ch41N/8l0cx")).build();
        try {
            HttpResponse response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) { //Success, lets replace the node chain
                Parse<List<Block>> parse = new Parse();
                List<Block> blockList = parse.fromJson(response.body().toString(),
                        new TypeReference<List<Block>>() {
                        });

                LOGGER.info("Sync blockchain with ROOT");
                blockchain.chain.replaceChain(blockList);
            }
        } catch (ConnectException ce) {
            //do nothing we probably are in the ROOT node
        } catch (Exception e) {
            LOGGER.error("Sync blockchain on connect FAILED: " + e.getMessage());
        }
    }

    private void syncCryptochain() {
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(ROOT_NODE_ADDRESS + "/CrYP70/Ch41N")).build();
        try {
            HttpResponse response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) { //Success, lets replace the node chain
                Parse<List<Block<List<Tx>>>> parse = new Parse();
                List<Block<List<Tx>>> blockList = parse.fromJson(response.body().toString(),
                        new TypeReference<List<Block<List<Tx>>>>() {
                        });

                LOGGER.info("Sync cryptochain with ROOT");
                miner.cryptoChain.replaceChain(blockList);
            }
        } catch (ConnectException ce) {
            //do nothing we probably are in the ROOT node
        } catch (Exception e) {
            LOGGER.error("Sync cryptochain on connect FAILED: " + e.getMessage());
        }
    }

    private void syncTransactionPollMap() {
        HttpRequest request = HttpRequest.newBuilder().GET()
                .uri(URI.create(ROOT_NODE_ADDRESS + "/p00L/7r4n54C7ioNM4p")).build();
        try {
            HttpResponse response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) { //Success, lets replace the node chain
                Parse<Map<String, Tx>> parse = new Parse();
                Map<String, Tx> rootTransactionPoolMap = parse.fromJson(response.body().toString(),
                        new TypeReference<Map<String, Tx>>() {
                        });

                LOGGER.info("Sync transaction pool with ROOT");
                miner.transactionPool.setMap(rootTransactionPoolMap);
            }
        } catch (ConnectException ce) {
            //do nothing we probably are in the ROOT node
        } catch (Exception e) {
            LOGGER.error("Sync Transaction Poll MAP on Connect FAILED: " + e.getMessage());
        }
    }

    private void loadFromDatabase() {
        loadWallet();
        loadChain();
    }

    private void loadWallet() {
        if (rewardEnabled != null && rewardEnabled == true) {
            try {
                miner.rewardWallet = walletRepository.getByUid(rewardWallet);
            } catch (RuntimeException re) {
                LOGGER.error("Reward Wallet: " + re.getMessage());
            }
        }
    }

    /* Failure on one chain must not interfere in the other, so the process of loading should not be interrupted */
    private void loadChain() {
        LOGGER.warn("loadingChain: " + Util.getHostIp() + " " + Util.getHostName());
        if (HTTP_PORT.equals("8018")) { //make load chain from database operation only from root note, other nodes must sync
            try {
                blockchain.chain.replaceChain(blockRepository.loadBlockchain());
            } catch (RuntimeException re) {
                LOGGER.warn("Blockchain: " + re.getMessage());
            }

            try {
                //FIXME: https://git.okaut.com/Jesus/api.blockchain/-/issues/19
                // cannot replaceChain as the calculation of balance mismatch
                // in the next api starts, the wallet change it's address, so even if i save the balance and load it again
                // calculation will fail as we are talking about different addresses now
                miner.cryptoChain.replaceChain(blockRepository.loadCryptochain(), BigDecimal.ZERO);
            } catch (RuntimeException re) {
                LOGGER.warn("Cryptochain: " + re.getMessage());
            }
        }
    }

}
