package api;

import io.quarkus.qute.Template;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Inject
    Template error404;

    @Override
    @Produces(MediaType.TEXT_HTML)
    public Response toResponse(NotFoundException exception) {
        return Response.ok(error404.render()).build();
    }
}