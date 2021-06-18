package api.blockchain;

import api.blockchain.pubsub.PublishService;
import com.fasterxml.jackson.core.JsonProcessingException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("Ch41N")
public class ChainResource {

    @Inject
    SharedService sharedService;

    @Inject
    PublishService publisher;

    @GET
    @Produces("application/json")
    @Path("blocks")
    public Response blocks() {
        return Response.ok(sharedService.blockchain.getChain()).build();
    }

    @POST
    @Produces("application/json")
    @Path("mine")
    public Response mine(Object data) throws JsonProcessingException {
        sharedService.blockchain.addBlock(data);
        publisher.broadcastChain(sharedService.blockchain.getChain());
        return Response.ok(sharedService.blockchain.getChain()).build();
    }

}