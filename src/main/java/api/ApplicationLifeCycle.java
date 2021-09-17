package api;

import api.blockchain.Blockchain;
import api.cryptocurrency.TransactionMiner;
import api.util.Network;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.util.Parse;
import com.R07r35cInc.cryptocurrency.Transaction;
import com.fasterxml.jackson.core.type.TypeReference;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
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
import java.util.List;
import java.util.Map;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
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
                Parse<List<Block>> parse = new Parse();
                List<Block> blockList = parse.fromJson(response.body().toString(),
                        new TypeReference<List<Block>>() {});

                LOGGER.info("Sync chain with ROOT");
                blockchain.chain.replaceChain(blockList);
            }
        } catch (ConnectException ce) {
            //do nothing we probably are in the ROOT node
        } catch (Exception e) {
            LOGGER.error("Sync chain on connect FAILED: " + e.getMessage());
        }
    }

    private void syncTransactionPollMap() {
        HttpRequest request = HttpRequest.newBuilder().GET()
                .uri(URI.create(ROOT_NODE_ADDRESS + "/p00L/7r4n54C7ioNM4p")).build();
        try {
            HttpResponse response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) { //Success, lets replace the node chain
                Parse<Map<String, Transaction>> parse = new Parse();
                Map<String, Transaction> rootTransactionPoolMap = parse.fromJson(response.body().toString(),
                        new TypeReference<Map<String, Transaction>>() {});

                LOGGER.info("Sync transaction pool with ROOT");
                miner.transactionPool.setMap(rootTransactionPoolMap);
            }
        } catch (ConnectException ce) {
            //do nothing we probably are in the ROOT node
        } catch (Exception e) {
            LOGGER.error("Sync Transaction Poll MAP on Connect FAILED: " + e.getMessage());
        }
    }
}
