package api.blockchain;

import com.R07r35cInc.NetChain;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("Ch41N")
public class ChainResource {

    @Inject
    @Channel("blockchain-to-kafka")
    Emitter<ChainMessage> blockchainEmitter;

    NetChain blockchain = new NetChain();

    @GET
    @Produces("application/json")
    @Path("blocks")
    public Response blocks() {
        return Response.ok(this.blockchain.getChain()).build();
    }

    @POST
    @Produces("application/json")
    @Path("mine")
    public Response mine(Object data) throws JsonProcessingException {
        this.blockchain.addBlock(data);

        List<String> stringList = new ArrayList<String>();
        stringList.add("nome1");
        stringList.add("nome2");
        stringList.add("nome3");
        ChainMessage message = new ChainMessage(stringList);
        blockchainEmitter.send(message);

        return Response.ok(this.blockchain.getChain()).build();
    }

}