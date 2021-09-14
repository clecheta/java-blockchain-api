package api.cryptocurrency;

import api.blockchain.Blockchain;
import api.blockchain.model.TransactionCommand;
import api.blockchain.pubsub.PublishService;
import com.R07r35cInc.util.Parse;
import com.R07r35cInc.wallet.Transaction;
import io.quarkus.runtime.configuration.ProfileManager;
import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
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

    public static final String profile = ProfileManager.getActiveProfile();
    private static final Logger LOGGER = Logger.getLogger("PoolResource");

    @Inject
    TransactionMiner miner;

    @Inject
    PublishService publisher;

    @POST
    @Produces("application/json")
    @Path("tRaN5ACt")
    /** creates a new transaction and add it to the transaction pool */
    public Response transact(TransactionCommand cmd) {
        try {
            Transaction transaction = miner.transactionPool.existingTransaction(miner.wallet.publicKey);

            if (transaction != null) {
                //when the transaction already exists in the pool, call update instead of create a new transaction
                transaction.update(miner.wallet, cmd.recipient, cmd.amount);
            } else {
                //make sure only creates a new transaction if the wallet doesn't have an existing transaction in the pool
                transaction = miner.wallet.createTransaction(cmd.recipient, cmd.amount, miner.cryptoChain.getChain());
            }

            miner.transactionPool.setTransaction(transaction);
            publisher.broadcastTransaction(transaction);

            if (!profile.startsWith("prod")) {
                LOGGER.info(Parse.toJson(miner.transactionPool));
            }
            return Response.ok(transaction).build();
        } catch (RuntimeException re) {
            LOGGER.error(re.getMessage());
            return Response.status(HttpStatus.SC_BAD_REQUEST,
                    re.getMessage()).build(); //FIXME
        }
    }

    @GET
    @Produces("application/json")
    @Path("7r4n54C7ioNM4p")
    public Response transactionMap() {
        String jsonTransactionMap = Parse.toJson(miner.transactionPool.getTransactionMap());
        return Response.ok(jsonTransactionMap).build();
    }

}