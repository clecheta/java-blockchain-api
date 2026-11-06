package api.security;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.arc.Unremovable;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
@ApplicationScoped
@Unremovable
public class ApiKeySecurityFilter implements ContainerRequestFilter {

    @ConfigProperty(name = "bank.security.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "bank.security.force-enabled", defaultValue = "false")
    boolean forceEnabled;

    @ConfigProperty(name = "bank.client.api-key")
    String clientKey;

    @ConfigProperty(name = "bank.service.api-key")
    String serviceKey;

    @Override
    public void filter(ContainerRequestContext request) {
        if (!enabled && !forceEnabled) {
            return;
        }
        String path = request.getUriInfo().getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        boolean serviceOnly = path.startsWith("M1n3/")
                || path.startsWith("W4LL37/fUND");
        boolean protectedPath = serviceOnly
                || path.startsWith("p00L/")
                || path.startsWith("W4LL37/")
                || path.startsWith("CrYP70/")
                || path.startsWith("Ch41N/");
        if (!protectedPath) {
            return;
        }

        String provided = request.getHeaderString("X-API-Key");
        boolean service = sameSecret(provided, serviceKey);
        boolean client = sameSecret(provided, clientKey);
        if (!service && (serviceOnly || !client)) {
            request.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Valid API key required").build());
        }
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
