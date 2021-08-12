package api.blockchain;

import api.blockchain.util.NetUtil;
import com.R07r35cInc.Block;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import cryptocurrency.wallet.Transaction;
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
import java.lang.reflect.Type;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@ApplicationScoped
public class ApplicationLifeCycle {

    private static final Logger LOGGER = Logger.getLogger("ApplicationLifeCycle");
    public static final String profile = ProfileManager.getActiveProfile();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(60))
            .build();
    @Inject
    SharedService sharedService;
    @ConfigProperty(name = "root.node.address")
    private String ROOT_NODE_ADDRESS;

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("Blockchain network node _" + NetUtil.getHostName() + " is starting...");
        //syncing with root state
        syncChains();
        syncTransactionPollMap();
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOGGER.info("Blockchain network node _" + NetUtil.getHostName() + " stopped.");
    }

    private void syncChains() {
        HttpRequest request = HttpRequest.newBuilder().GET()
                .uri(URI.create(ROOT_NODE_ADDRESS + "/Ch41N/8l0cx")).build();
        try {
            HttpResponse response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) { //Success, lets replace the node chain
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Block[] blocks = gson.fromJson(response.body().toString(), Block[].class);
                List<Block> blockList = Arrays.asList(blocks);
                sharedService.blockchain.replaceChain(blockList);

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
                Gson gson = new GsonBuilder().create();
                Type type = new TypeToken<Map<String, Transaction>>(){}.getType();
                Map<String, Transaction> rootTransactionPoolMap = gson.fromJson(response.body().toString(), type);
                sharedService.transactionPool.setMap(rootTransactionPoolMap);

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
