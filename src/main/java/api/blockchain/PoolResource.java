package api.blockchain;

import api.blockchain.model.TransactCommand;
import api.blockchain.pubsub.PublishService;
import com.fasterxml.jackson.core.JsonProcessingException;
import cryptocurrency.Transaction;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@Path("p00L")
public class PoolResource {

    private static final Logger LOGGER = Logger.getLogger("PoolResource");

    @Inject
    SharedService sharedService;

    @Inject
    PublishService publisher;

    @POST
    @Produces("application/json")
    @Path("tRaN5ACt")
    /** creates a new transaction and add it to the transaction pool */
    public Response transact(TransactCommand cmd) {
        Transaction transaction = sharedService.wallet.createTransaction(cmd.recipient, cmd.amount);
        sharedService.transactionPool.setTransaction(transaction);

        LOGGER.info(sharedService.transactionPool);
        return Response.ok(transaction).build();
    }

}