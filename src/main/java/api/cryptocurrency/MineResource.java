package api.cryptocurrency;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@Path("M1n3")
public class MineResource {

    @Inject
    TransactionMiner miner;

    @POST
    @Produces("application/json")
    @Path("7r4N54C710nZ")
    public Response mineTransactions() {
        miner.mineTransactions();
        return Response.ok(miner.cryptoChain.getChain()).build();
    }
}
