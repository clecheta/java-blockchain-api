package api;

import api.blockchain.Blockchain;
import api.util.Network;
import api.cryptocurrency.TransactionMiner;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.util.Parse;
import com.R07r35cInc.wallet.Transaction;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@ApplicationScoped
public class ApplicationLifeCycle {

    public static final String profile = ProfileManager.getActiveProfile();
    private static final Logger LOGGER = Logger.getLogger("ApplicationLifeCycle");
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(60))
            .build();
    @Inject
    Blockchain blockchain;
    @Inject
    TransactionMiner miner;
    @ConfigProperty(name = "root.node.address")
    private String ROOT_NODE_ADDRESS;

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("Blockchain network node _" + Network.getHostName() + " is starting...");
        //syncing with root state
        syncChains();
        syncTransactionPollMap();
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOGGER.info("Blockchain network node _" + Network.getHostName() + " stopped.");
    }

    private void syncChains() {
        HttpRequest request = HttpRequest.newBuilder().GET()
                .uri(URI.create(ROOT_NODE_ADDRESS + "/Ch41N/8l0cx")).build();
        try {
            HttpResponse response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) { //Success, lets replace the node chain
                Parse<Block[]> parse = new Parse(Block[].class);
                Block[] blocks = parse.fromJson(response.body().toString());
                List<Block> blockList = Arrays.asList(blocks);
                blockchain.chain.replaceChain(blockList);

                if (!profile.startsWith("prod")) {
                    LOGGER.warn("replace chain on a sync with ROOT");
                    LOGGER.warn(blocks);
                }
            }
        } catch (ConnectException ce) {
            //do nothing we probably are in the ROOT node
        } catch (Exception e) {
            LOGGER.error("Sync Chain on Connect FAILED: " + e.getMessage());
        }
    }

    private void syncTransactionPollMap() {
        HttpRequest request = HttpRequest.newBuilder().GET()
                .uri(URI.create(ROOT_NODE_ADDRESS + "/p00L/7r4n54C7ioNM4p")).build();
        try {
            HttpResponse response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) { //Success, lets replace the node chain
                Map<String, Transaction> rootTransactionPoolMap = null;
                Parse<Map<String, Transaction>> parse = new Parse(rootTransactionPoolMap.getClass());
                rootTransactionPoolMap = parse.fromJson(response.body().toString());
                miner.transactionPool.setMap(rootTransactionPoolMap);

                if (!profile.startsWith("prod")) {
                    LOGGER.warn("replace transaction pool map on a sync with ROOT");
                }
            }
        } catch (ConnectException ce) {
            //do nothing we probably are in the ROOT node
        } catch (Exception e) {
            LOGGER.error("Sync Transaction Poll MAP on Connect FAILED: " + e.getMessage());
        }
    }
}
