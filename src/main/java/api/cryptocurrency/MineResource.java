package api.cryptocurrency;

import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Tx;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Map;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@Path("M1n3")
public class MineResource {

    @Inject
    TransactionMiner miner;

    @ConfigProperty(name = "mining.settlement.enabled", defaultValue = "false")
    boolean settlementEnabled;

    @POST
    @Produces("application/json")
    @Path("7r4N54C710nZ")
    @Timed(value = "jblock.settlement", histogram = true)
    public Response mineTransactions() {
        if (!settlementEnabled) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("This node is not the settlement writer").build();
        }
        Block<List<Tx>> block = miner.mineTransactions();
        if (block == null) {
            return Response.noContent().build();
        }
        return Response.ok(Map.of(
                "blockHash", block.getHash(),
                "blockNumber", block.getNumber(),
                "transactionCount", block.getData().size())).build();
    }
}
