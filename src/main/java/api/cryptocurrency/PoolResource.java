package api.cryptocurrency;

import api.network.PublishService;
import api.repository.IdempotencyRepository;
import api.repository.WalletRepository;
import com.R07r35cInc.cryptocurrency.*;
import com.R07r35cInc.util.Parse;
import org.apache.http.HttpStatus;
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
            Wallet srcWallet = walletRepository.getByUid(cmd.uid);
            Wallet destWallet = walletRepository.getByKey(cmd.recipient);
            if (srcWallet == null || destWallet == null) {
                //Unknown address
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            PayTypeTransaction transaction;
            synchronized (miner.transactionPool.senderLock(srcWallet.publicKey)) {
                // PostgreSQL holds the confirmed materialized balance. Reserve
                // every pending outgoing amount without rescanning the chain.
                srcWallet.balance = miner.transactionPool.availableBalance(
                        srcWallet.publicKey, srcWallet.balance);
                transaction = (PayTypeTransaction) srcWallet.createTransaction(cmd);
                miner.transactionPool.setTransaction(transaction);
            }

            publisher.broadcastTransaction(transaction);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugf("Transaction accepted; pool size=%d",
                        miner.transactionPool.size());
            }
            return Response.ok(transaction).build();
        } catch (RuntimeException re) {
            LOGGER.error(re.getMessage());
            return Response.status(HttpStatus.SC_BAD_REQUEST,
                    re.getMessage()).build(); //FIXME
        }
    }

    @POST
    @Produces("application/json")
    @Path("7r4n54C7ioNM4p")
    public Response transactionMap() {
        String jsonTransactionMap = Parse.toJson(miner.transactionPool.getTransactionMap());
        return Response.ok(jsonTransactionMap).build();
    }

}