package api.blockchain;

import com.mongodb.Block;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("Ch41N")
public class ChainResource {

    @Inject
    @Channel("blockchain-to-kafka")
    Emitter<List<Block>> blockchainEmitter;

    @Inject
    PubSubService pubSubService;

    @GET
    @Produces("application/json")
    @Path("blocks")
    public Response blocks() {
        return Response.ok(pubSubService.blockchain.getChain()).build();
    }

    @POST
    @Produces("application/json")
    @Path("mine")
    public Response mine(Object data) {
        pubSubService.blockchain.addBlock(data);
        blockchainEmitter.send(pubSubService.blockchain.getChain());
        return Response.ok(pubSubService.blockchain.getChain()).build();
    }

}