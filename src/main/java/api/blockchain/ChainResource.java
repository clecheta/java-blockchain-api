package api.blockchain;

import com.R07r35cInc.Block;
import com.R07r35cInc.Blockchain;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("Ch41N")
public class ChainResource {

    Blockchain blockchain = new Blockchain();

    @GET
    @Produces("application/json")
    @Path("blocks")
    public Response blocks() {
        return Response.ok(blockchain.getChain()).build();
    }

    @POST
    @Produces("application/json")
    @Path("mine")
    public Response mine(Object data) {
        blockchain.addBlock(data);
        return Response.ok(blockchain.getChain()).build();
    }
}