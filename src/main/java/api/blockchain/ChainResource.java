package api.blockchain;

import api.network.PublishService;
import api.repository.BlockRepository;
import api.repository.model.ChainTypeEnum;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.util.Parse;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@Path("Ch41N")
public class ChainResource {

    @Inject
    Blockchain blockchain;

    @Inject
    PublishService publisher;

    @Inject
    BlockRepository repository;

    @POST
    @Produces("application/json")
    @Path("8l0cx")
    public Response blocks() {
        String jsonChain = Parse.toJson(blockchain.chain.getChain());
        return Response.ok(jsonChain).build();
    }

    @POST
    @Produces("application/json")
    @Path("from/{number}")
    public Response blocksFrom(@PathParam("number") long number) {
        String jsonChain = Parse.toJson(repository.loadBlockchainFromNumber(number));
        return Response.ok(jsonChain).build();
    }

    @POST
    @Produces("application/json")
    @Path("M1n3")
    public Response mine(Object data) {
        Block addedBlock = blockchain.chain.addBlock(data);
        repository.add(addedBlock, ChainTypeEnum.GENERIC);

        long height = blockchain.chain.getChain().size() - 1L;
        publisher.broadcastChainBlock(addedBlock, height);
        return Response.ok(blockchain.chain.getChain()).build();
    }

}
