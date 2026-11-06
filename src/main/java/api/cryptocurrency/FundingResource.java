package api.cryptocurrency;

import api.cryptocurrency.model.FundingCommand;
import api.repository.FundingRepository;
import api.repository.WalletRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.micrometer.core.annotation.Timed;

import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Path("W4LL37")
public class FundingResource {

    @Inject
    WalletRepository wallets;

    @Inject
    FundingRepository funding;

    @ConfigProperty(name = "bank.service.api-key")
    String serviceApiKey;

    @POST
    @Path("fUND")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Timed(value = "jblock.funding", histogram = true)
    public Response fund(@HeaderParam("X-API-Key") String apiKey, FundingCommand command) {
        if (!sameSecret(apiKey, serviceApiKey)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        if (command == null || command.uid == null || command.idempotencyKey == null
                || command.idempotencyKey.isBlank() || command.amount == null
                || command.amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("uid, positive amount, and idempotencyKey are required").build();
        }
        if (!funding.reserve(command)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Duplicate idempotency key").build();
        }
        BigDecimal balance = wallets.fund(command.uid, command.amount);
        funding.complete(command.idempotencyKey);
        return Response.ok(Map.of(
                "uid", command.uid,
                "credited", command.amount.toPlainString(),
                "balance", balance.toPlainString(),
                "idempotencyKey", command.idempotencyKey)).build();
    }

    private boolean sameSecret(String provided, String expected) {
        if (provided == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
