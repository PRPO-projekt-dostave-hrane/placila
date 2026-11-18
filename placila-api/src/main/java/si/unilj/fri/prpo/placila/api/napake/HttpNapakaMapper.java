package si.unilj.fri.prpo.placila.api.napake;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import si.unilj.fri.prpo.placila.api.model.Problem;

@Provider
public class HttpNapakaMapper implements ExceptionMapper<WebApplicationException> {

    static final String OSNOVA = "https://dostava-hrane.example/problemi/";

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException e) {
        Response.StatusType statusInfo = e.getResponse().getStatusInfo();
        int status = statusInfo.getStatusCode();

        Problem problem = new Problem()
        .type(tip(status))
        .title(naslov(status, statusInfo))
        .status(status)
        .detail(e.getMessage())
        .instance(uriInfo.getRequestUri().getPath());

        return Response.status(status)
        .type("application/problem+json")
        .entity(problem)
        .build();
    }

    private static String tip(int status) {
        return switch (status) {
            case 400 -> OSNOVA + "zahteva";
            case 404 -> OSNOVA + "ni-najden";
            default -> "about:blank";
        };
    }

    private static String naslov(int status, Response.StatusType statusInfo) {
        return switch (status) {
            case 400 -> "Napačna zahteva";
            case 404 -> "Vir ne obstaja";
            default -> statusInfo.getReasonPhrase();
        };
    }
}