package api.network;

import api.blockchain.Blockchain;
import api.cryptocurrency.TransactionMiner;
import api.repository.BlockRepository;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Tx;
import com.R07r35cInc.util.Parse;
import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * HTTP bootstrap and runtime catch-up against the root node (or local Mongo on root).
 *
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@ApplicationScoped
public class PeerSyncService {

    private static final Logger LOGGER = Logger.getLogger("PeerSyncService");

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

    @ConfigProperty(name = "root.node.address")
    String rootNodeAddress;

    @ConfigProperty(name = "ledger.node.role", defaultValue = "peer")
    String nodeRole;

    @ConfigProperty(name = "bank.client.api-key")
    String clientApiKey;

    public boolean isRoot() {
        return "settler".equalsIgnoreCase(nodeRole);
    }

    public void syncBlockchainFull() {
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("X-API-Key", clientApiKey)
                .uri(URI.create(rootNodeAddress + "/Ch41N/8l0cx")).build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Parse<List<Block>> parse = new Parse<>();
                List<Block> blockList = parse.fromJson(response.body(),
                        new TypeReference<List<Block>>() {
                        });
                LOGGER.info("Sync blockchain with ROOT");
                blockchain.chain.replaceChain(blockList);
            }
        } catch (ConnectException ce) {
            // root node
        } catch (Exception e) {
            LOGGER.error("Sync blockchain on connect FAILED: " + e.getMessage());
        }
    }

    public void syncCryptochainFull() {
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("X-API-Key", clientApiKey)
                .uri(URI.create(rootNodeAddress + "/CrYP70/Ch41N")).build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Parse<List<Block<List<Tx>>>> parse = new Parse<>();
                List<Block<List<Tx>>> blockList = parse.fromJson(response.body(),
                        new TypeReference<List<Block<List<Tx>>>>() {
                        });
                LOGGER.info("Sync cryptochain with ROOT");
                miner.cryptoChain.replaceChain(blockList);
            }
        } catch (ConnectException ce) {
            // root node
        } catch (Exception e) {
            LOGGER.error("Sync cryptochain on connect FAILED: " + e.getMessage());
        }
    }

    public void syncTransactionPoolMap() {
        HttpRequest request = HttpRequest.newBuilder().GET()
                .header("X-API-Key", clientApiKey)
                .uri(URI.create(rootNodeAddress + "/p00L/7r4n54C7ioNM4p")).build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Parse<Map<String, Tx>> parse = new Parse<>();
                Map<String, Tx> rootTransactionPoolMap = parse.fromJson(response.body(),
                        new TypeReference<Map<String, Tx>>() {
                        });
                LOGGER.info("Sync transaction pool with ROOT");
                miner.transactionPool.setMap(rootTransactionPoolMap);
            }
        } catch (ConnectException ce) {
            // root node
        } catch (Exception e) {
            LOGGER.error("Sync Transaction Poll MAP on Connect FAILED: " + e.getMessage());
        }
    }

    /**
     * Fetch and append crypto blocks with number &gt;= fromInclusive after a Kafka gap.
     */
    public void catchUpCryptochain(long fromInclusive) {
        List<Block<List<Tx>>> missing = fetchCryptochainFrom(fromInclusive);
        for (Block<List<Tx>> block : missing) {
            try {
                miner.cryptoChain.appendValidatedBlock(block);
                miner.transactionPool.clearBlockchainTransactions(Collections.singletonList(block));
            } catch (RuntimeException re) {
                LOGGER.errorf("Catch-up crypto block #%d failed: %s", block.getNumber(), re.getMessage());
                // Fall back to full replace from root
                syncCryptochainFull();
                return;
            }
        }
        LOGGER.infof("Catch-up cryptochain applied %d block(s) from #%d", missing.size(), fromInclusive);
    }

    /**
     * Fetch and append generic blocks with number &gt;= fromInclusive after a Kafka gap.
     */
    public void catchUpBlockchain(long fromInclusive) {
        List<Block> missing = fetchBlockchainFrom(fromInclusive);
        for (Block block : missing) {
            try {
                blockchain.chain.appendValidatedBlock(block);
            } catch (RuntimeException re) {
                LOGGER.errorf("Catch-up chain block #%d failed: %s", block.getNumber(), re.getMessage());
                syncBlockchainFull();
                return;
            }
        }
        LOGGER.infof("Catch-up blockchain applied %d block(s) from #%d", missing.size(), fromInclusive);
    }

    private List<Block<List<Tx>>> fetchCryptochainFrom(long fromInclusive) {
        if (isRoot()) {
            return blockRepository.loadCryptochainFromNumber(fromInclusive);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("X-API-Key", clientApiKey)
                .uri(URI.create(rootNodeAddress + "/CrYP70/Ch41N/from/" + fromInclusive)).build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Parse<List<Block<List<Tx>>>> parse = new Parse<>();
                return parse.fromJson(response.body(),
                        new TypeReference<List<Block<List<Tx>>>>() {
                        });
            }
        } catch (Exception e) {
            LOGGER.error("Fetch cryptochain from ROOT FAILED: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    private List<Block> fetchBlockchainFrom(long fromInclusive) {
        if (isRoot()) {
            return blockRepository.loadBlockchainFromNumber(fromInclusive);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("X-API-Key", clientApiKey)
                .uri(URI.create(rootNodeAddress + "/Ch41N/from/" + fromInclusive)).build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Parse<List<Block>> parse = new Parse<>();
                return parse.fromJson(response.body(),
                        new TypeReference<List<Block>>() {
                        });
            }
        } catch (Exception e) {
            LOGGER.error("Fetch blockchain from ROOT FAILED: " + e.getMessage());
        }
        return Collections.emptyList();
    }

}
