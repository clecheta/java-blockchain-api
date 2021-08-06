package api.blockchain;

import api.blockchain.model.TransactCommand;
import com.google.gson.GsonBuilder;
import cryptocurrency.Transaction;
import io.quarkus.runtime.configuration.ProfileManager;
import org.apache.http.HttpStatus;
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

    public static final String profile = ProfileManager.getActiveProfile();
    private static final Logger LOGGER = Logger.getLogger("PoolResource");
    @Inject
    SharedService sharedService;

    @POST
    @Produces("application/json")
    @Path("tRaN5ACt")
    /** creates a new transaction and add it to the transaction pool */
    public Response transact(TransactCommand cmd) {
        try {
            Transaction transaction = sharedService.transactionPool.existingTransaction(sharedService.wallet.publicKey);

            if (transaction != null) {
                //when the transaction already exists in the pool, call update instead of create a new transaction
                transaction.update(sharedService.wallet, cmd.recipient, cmd.amount);
            } else {
                //make sure only creates a new transaction if the wallet doesn't have an existing transaction in the pool
                transaction = sharedService.wallet.createTransaction(cmd.recipient, cmd.amount);
            }

            sharedService.transactionPool.setTransaction(transaction);

            if (!profile.startsWith("prod")) {
                String serializedTransactionPool = new GsonBuilder().setPrettyPrinting()
                        .create().toJson(sharedService.transactionPool);
                LOGGER.info(serializedTransactionPool);
            }
            return Response.ok(transaction).build();
        } catch (RuntimeException re) {
            LOGGER.error(re.getMessage());
            return Response.status(HttpStatus.SC_BAD_REQUEST,
                    re.getMessage()).build(); //FIXME
        }
    }

}