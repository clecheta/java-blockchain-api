package api.cryptocurrency;

import api.network.PublishService;
import api.repository.IdempotencyRepository;
import api.repository.PendingTransactionRepository;
import api.repository.WalletRepository;
import com.R07r35cInc.cryptocurrency.*;
import com.R07r35cInc.util.Parse;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.micrometer.core.annotation.Timed;

import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@Path("p00L")
public class PoolResource {

    private static final Logger LOGGER = Logger.getLogger("PoolResource");


    @Inject
    TransactionMiner miner;

    @Inject
    PublishService publisher;

    @Inject
    WalletRepository walletRepository;

    @Inject
    IdempotencyRepository idempotency;

    @Inject
    PendingTransactionRepository pendingTransactions;

    @ConfigProperty(name = "bank.idempotency.required", defaultValue = "true")
    boolean idempotencyRequired;

    @ConfigProperty(name = "bank.velocity.max-transfers-per-minute", defaultValue = "1000")
    long maxTransfersPerMinute;

    @POST
    @Produces("application/json")
    @Path("tRaN5ACt")
    @Timed(value = "jblock.transfer.accept", histogram = true)
    /** creates a new transaction and add it to the transaction pool */
    public Response transact(@HeaderParam("Idempotency-Key") String idempotencyKey,
                             TransactionCommand cmd) {
        try {
            /* if an invalid value is informed in type it backs to the caller
             *  with: not one of the values accepted for Enum
             * class: [MONEY, CREDIT, TRANSFER, DEBIT, PIX, WALLET, INTERCHANGE] */
            if (cmd.type == null || cmd.uid == null || cmd.recipient == null) {
                throw new RuntimeException("wrong usage");
            }
            if (idempotencyRequired && (idempotencyKey == null || idempotencyKey.isBlank())) {
                return Response.status(428).entity("Idempotency-Key is required").build();
            }
            if (idempotency.recentCount("TRANSFER", cmd.uid, 1) >= maxTransfersPerMinute) {
                return Response.status(429).entity("Transfer velocity limit exceeded").build();
            }
            if (idempotencyKey != null && !idempotencyKey.isBlank()
                    && !idempotency.reserve(idempotencyKey, "TRANSFER", cmd.uid, Parse.toJson(cmd))) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("Duplicate idempotency key").build();
            }

            //TODO ideally we should use a cmd token from logged user and get the wallet of the logged user by then comparing uid.
            if (!walletRepository.existsByPublicKey(cmd.recipient)) {
                //Unknown address
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // PostgreSQL serializes accepts for the same wallet across every
            // API node and durably journals the signed transaction.
            PayTypeTransaction transaction = pendingTransactions.reserveAndCreate(cmd);
            miner.transactionPool.setTransaction(transaction);

            publisher.broadcastTransaction(transaction);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugf("Transaction accepted; pool size=%d",
                        miner.transactionPool.size());
            }
            return Response.ok(transaction).build();
        } catch (RuntimeException re) {
            String message = rootMessage(re);
            LOGGER.error(message, re);
            if (isAcquisitionTimeout(message)) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity(message)
                        .build();
            }
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(message == null ? "bad request" : message)
                    .build();
        }
    }

    static boolean isAcquisitionTimeout(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("acquisition timeout")
                || lower.contains("unable to acquire jdbc connection")
                || lower.contains("sorry, acquisition timeout");
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        String message = current.getMessage();
        while (current.getCause() != null) {
            current = current.getCause();
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getMessage();
            }
        }
        return message;
    }

    @POST
    @Produces("application/json")
    @Path("7r4n54C7ioNM4p")
    public Response transactionMap() {
        String jsonTransactionMap = Parse.toJson(miner.transactionPool.getTransactionMap());
        return Response.ok(jsonTransactionMap).build();
    }

}
