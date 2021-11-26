package api.blockchain;

import api.network.PublishService;
import api.repository.BlockRepository;
import api.repository.model.ChainTypeEnum;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.util.Parse;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
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
    @Path("M1n3")
    public Response mine(Object data) {
        Block addedBlock = blockchain.chain.addBlock(data);
        repository.add(addedBlock, ChainTypeEnum.GENERIC);

        publisher.broadcastChain(blockchain.chain);
        return Response.ok(blockchain.chain.getChain()).build();
    }

}