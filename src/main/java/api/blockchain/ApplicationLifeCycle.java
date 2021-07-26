package api.blockchain;

import api.blockchain.util.NetUtil;
import com.R07r35cInc.Block;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.Reader;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ApplicationLifeCycle {

    @Inject
    SharedService sharedService;

    private static final Logger LOGGER = Logger.getLogger("ApplicationLifeCycle");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(60))
            .build();

    @ConfigProperty(name = "root.node.address")
    private String ROOT_NODE_ADDRESS;

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("Blockchain network node _" + NetUtil.getHostName() + " is starting...");

        HttpRequest request = HttpRequest.newBuilder().GET()
                .uri(URI.create(ROOT_NODE_ADDRESS+"/Ch41N/blocks")).build();
        try {
            HttpResponse response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) { //Success, lets replace the node chain
                Gson gson = new GsonBuilder().create();
                Block[] blocks = gson.fromJson(response.body().toString(), Block[].class);
                List<Block> blockList = Arrays.asList(blocks);
                sharedService.blockchain.replaceChain(blockList);
            }
        } catch (ConnectException ce) {
            //do nothing we probably are in the ROOT node
        } catch (Exception e) {
            LOGGER.error("Sync Chain on Connect FAILED: " + e.getMessage());
        }
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOGGER.info("Blockchain network node _" + NetUtil.getHostName() + " stopped.");
    }

}
