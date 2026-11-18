package si.unilj.fri.prpo.placila.api.napake;

import java.util.List;

import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import si.unilj.fri.prpo.placila.api.model.ValidacijskiProblem;
import si.unilj.fri.prpo.placila.api.model.NapakaVnosa;

@Provider
public class ValidacijaMapper implements ExceptionMapper<ValidationException> {

    @Context
    UriInfo uriInfo;

    @Override 
    public Response toResponse(ValidationException e) {
        if (!(e instanceof ConstraintViolationException cve)) {
            return Response.serverError().build();
        }

        boolean vTelesu = cve.getConstraintViolations().stream()
                .anyMatch(v -> jeVTelesu(v));
        int status = vTelesu ? 422 : 400;

        List<NapakaVnosa> napake = cve.getConstraintViolations().stream()
        .map(v -> new NapakaVnosa()
            .pointer(kazalec(v))
            .detail(v.getMessage()))
        .toList();

        ValidacijskiProblem problem = new ValidacijskiProblem()
        .type(HttpNapakaMapper.OSNOVA + (vTelesu ? "vnos" : "zahteva"))
        .title(vTelesu ? "Neveljavni podatki" : "Napačna zahteva")
        .status(status)
        .detail(vTelesu ? "Telo zahteve vsebuje neveljavne podatke." : "Parametri zahteve niso veljavni.")
        .instance(uriInfo.getRequestUri().getPath())
        .errors(napake);

        return Response.status(status)
        .type("application/problem+json")
        .entity(problem)
        .build();
    }

    private static boolean jeVTelesu(ConstraintViolation<?> v) {
        for (Path.Node n : v.getPropertyPath()) {
            if (n.getKind() == ElementKind.PROPERTY) {
                return true;
            }
        }
        return false;
    }

    private static String kazalec(ConstraintViolation<?> v) {
        StringBuilder polja = new StringBuilder();
        String parameter = "";
        for (Path.Node n : v.getPropertyPath()) {
            if (n.getKind() == ElementKind.PROPERTY) {
                polja.append('/').append(n.getName());
            } else if (n.getKind() == ElementKind.PARAMETER) {
                parameter = n.getName();
            }
        }
        return polja.isEmpty() ? parameter : "#" + polja;
    }
}