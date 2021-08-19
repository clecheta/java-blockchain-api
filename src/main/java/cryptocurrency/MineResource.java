package cryptocurrency;

import api.blockchain.Blockchain;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@Path("M1n3")
public class MineResource {

    @Inject
    Blockchain blockchain;

    @Inject
    TransactionMiner miner;

    @GET
    @Produces("application/json")
    @Path("7r4N54C710nZ")
    public Response mineTransactions() {
        miner.mineTransactions();
        return Response.ok(blockchain.chain.getChain()).build();
    }
}
