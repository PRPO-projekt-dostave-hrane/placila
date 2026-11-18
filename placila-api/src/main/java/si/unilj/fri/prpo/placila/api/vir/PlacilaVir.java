package si.unilj.fri.prpo.placila.api.vir;

import java.net.URI;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.enterprise.context.ApplicationScoped;

import si.unilj.fri.prpo.placila.api.model.Cena;
import si.unilj.fri.prpo.placila.api.model.NovoPlacilo;
import si.unilj.fri.prpo.placila.api.model.Placilo;
import si.unilj.fri.prpo.placila.api.PlacilaApi;

@ApplicationScoped
public class PlacilaVir implements PlacilaApi {
    private final Map<String, Placilo> placila = new ConcurrentHashMap<>();
    private final Map<UUID, String> kljuci = new ConcurrentHashMap<>();

    public PlacilaVir() {
        dodaj("p-3", "n-3", "10.00", "tx-0003");
        dodaj("p-4", "n-4", "28.50", "tx-0004");
        dodaj("p-5", "n-5", "9.50", "tx-0005");
        dodaj("p-6", "n-6", "24.00", "tx-0006");
    }

    private void dodaj(String id, String narociloId, String znesek, String transakcijaId) {
        placila.put(id, new Placilo()
        .id(id)
        .narociloId(narociloId)
        .znesek(new Cena().znesek(znesek).valuta(Cena.ValutaEnum.EUR))
        .status(Placilo.StatusEnum.USPESNO)
        .transakcijaId(transakcijaId));
    }
    
    @Override 
    public synchronized Response izvediPlacilo(UUID idempotencyKey, NovoPlacilo novoPlacilo) {
        String obstojeciId = kljuci.get(idempotencyKey);
        if (obstojeciId != null) {
            Placilo prejsnePlacilo = placila.get(obstojeciId);
            if (prejsnePlacilo.getNarociloId().equals(novoPlacilo.getNarociloId())) {
                URI lokacija = URI.create("/v1/placila/" + obstojeciId);
                return Response.created(lokacija).entity(prejsnePlacilo).build();
            }
            else {
                throw new BadRequestException("Idempotency-Key je že uporabljen za drugo naročilo.");
            }
        }

        String id = "p-" + UUID.randomUUID();
        String transakcijaId = "tx-" + UUID.randomUUID();
        URI lokacija = URI.create("/v1/placila/" + id);
        Placilo placilo = new Placilo()
        .id(id)
        .narociloId(novoPlacilo.getNarociloId())
        .znesek(novoPlacilo.getZnesek())
        .status(Placilo.StatusEnum.USPESNO)
        .transakcijaId(transakcijaId);

        placila.put(id, placilo);
        kljuci.put(idempotencyKey, id);
        return Response.created(lokacija).entity(placilo).build();
    }

    @Override 
    public Response podrobnostiPlacila(String placiloId) {
        Placilo placilo = placila.get(placiloId);
        if (placilo == null) {
            throw new NotFoundException("Plačilo ni bilo najdeno.");
        }
        return Response.ok(placilo).build();
    }


}