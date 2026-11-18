package si.unilj.fri.prpo.placila.api.vir;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class PlacilaVirTest {

    private static final String PLACILA = "/v1/placila";

    private static String telo(String narociloId, String znesek) {
        return """
                {"narociloId": "%s", "znesek": {"znesek": "%s", "valuta": "EUR"}}
                """.formatted(narociloId, znesek);
    }

    private static String novKljuc() {
        return UUID.randomUUID().toString();
    }

    // POST /v1/placila

    @Test
    void izvedbaVrne201InLocation() {
        given().contentType(ContentType.JSON).header("Idempotency-Key", novKljuc())
                .body(telo("n-1", "19.00"))
                .when().post(PLACILA)
                .then().statusCode(201)
                .header("Location", containsString(PLACILA + "/p-"))
                .body("narociloId", equalTo("n-1"))
                .body("znesek.znesek", equalTo("19.00"))
                .body("status", equalTo("USPESNO"))
                .body("transakcijaId", startsWith("tx-"));
    }

    @Test
    void novoPlaciloJeShranjeno() {
        String id = given().contentType(ContentType.JSON).header("Idempotency-Key", novKljuc())
                .body(telo("n-1", "19.00"))
                .when().post(PLACILA)
                .then().statusCode(201)
                .extract().path("id");

        given()
                .when().get(PLACILA + "/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id));
    }

    @Test
    void ponovitevZIstimKljucemVrneIstoPlacilo() {
        String kljuc = novKljuc();

        String prviId = given().contentType(ContentType.JSON).header("Idempotency-Key", kljuc)
                .body(telo("n-1", "19.00"))
                .when().post(PLACILA)
                .then().statusCode(201)
                .extract().path("id");

        given().contentType(ContentType.JSON).header("Idempotency-Key", kljuc)
                .body(telo("n-1", "19.00"))
                .when().post(PLACILA)
                .then().statusCode(201)
                .body("id", equalTo(prviId));
    }

    @Test
    void istiKljucZaDrugoNarociloVrne400() {
        String kljuc = novKljuc();

        given().contentType(ContentType.JSON).header("Idempotency-Key", kljuc)
                .body(telo("n-1", "19.00"))
                .when().post(PLACILA)
                .then().statusCode(201);

        given().contentType(ContentType.JSON).header("Idempotency-Key", kljuc)
                .body(telo("n-2", "14.50"))
                .when().post(PLACILA)
                .then().statusCode(400)
                .contentType("application/problem+json");
    }

    @Test
    void istiKljucZDrugimZneskomVrne400() {
        String kljuc = novKljuc();

        given().contentType(ContentType.JSON).header("Idempotency-Key", kljuc)
                .body(telo("n-1", "19.00"))
                .when().post(PLACILA)
                .then().statusCode(201);

        given().contentType(ContentType.JSON).header("Idempotency-Key", kljuc)
                .body(telo("n-1", "9.50"))
                .when().post(PLACILA)
                .then().statusCode(400);
    }

    @Test
    void brezKljucaVrne400() {
        given().contentType(ContentType.JSON)
                .body(telo("n-1", "19.00"))
                .when().post(PLACILA)
                .then().statusCode(400);
    }

    @Test
    void neveljavenKljucVrne400() {
        given().contentType(ContentType.JSON).header("Idempotency-Key", "abc")
                .body(telo("n-1", "19.00"))
                .when().post(PLACILA)
                .then().statusCode(400)
                .body("type", endsWith("zahteva"));
    }

    @Test
    void neveljavenJsonVrne400() {
        given().contentType(ContentType.JSON).header("Idempotency-Key", novKljuc())
                .body("{ to ni json")
                .when().post(PLACILA)
                .then().statusCode(400);
    }

    @Test
    void manjkajoceNarociloVrne422() {
        given().contentType(ContentType.JSON).header("Idempotency-Key", novKljuc())
                .body("""
                        {"znesek": {"znesek": "19.00", "valuta": "EUR"}}
                        """)
                .when().post(PLACILA)
                .then().statusCode(422)
                .contentType("application/problem+json")
                .body("type", endsWith("vnos"))
                .body("errors[0].pointer", equalTo("#/narociloId"));
    }

    @Test
    void neveljavenZnesekVrne422() {
        given().contentType(ContentType.JSON).header("Idempotency-Key", novKljuc())
                .body(telo("n-1", "devet"))
                .when().post(PLACILA)
                .then().statusCode(422)
                .body("errors[0].pointer", equalTo("#/znesek/znesek"));
    }

    // GET /v1/placila/{placiloId}

    @Test
    void podrobnostiPlacila() {
        given()
                .when().get(PLACILA + "/p-3")
                .then().statusCode(200)
                .body("narociloId", equalTo("n-3"))
                .body("transakcijaId", equalTo("tx-0003"));
    }

    @Test
    void neobstojecePlaciloVrne404() {
        given()
                .when().get(PLACILA + "/p-99")
                .then().statusCode(404)
                .contentType("application/problem+json")
                .body("type", endsWith("ni-najden"));
    }
}
