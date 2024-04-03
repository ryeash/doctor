package vest.doctor.jaxrs;

import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import jakarta.ws.rs.core.MediaType;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import vest.doctor.runtime.Doctor;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.assertEquals;

@Test(invocationCount = 3)
public class JerseyTest {

    static {
        System.setProperty("doctor.jaxrs.bind", "http://10.0.0.0:19998/"); // TODO: must end with a slash? silly
    }

    private Doctor doctor;

    @BeforeClass(alwaysRun = true)
    public void setup() {
        doctor = Doctor.load();
    }

    @AfterClass(alwaysRun = true)
    public void shutdown() {
        if (doctor != null) {
            doctor.close();
        }
    }

    private RequestSpecification req() {
        RestAssured.baseURI = "http://localhost:19998/";
        return RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
    }

    public void basic() {
        req().get("/jaxrs/basic")
                .prettyPeek()
                .then()
                .statusCode(200)
                .body(is("ok"))
                .header("X-After", is("true"))
                .header("TIME", containsString("us"));
    }

    public void halt() {
        req().queryParam("halt", "true")
                .get("/jaxrs/get")
                .then()
                .statusCode(503);
    }

    public void pojo() {
        User user = new User();
        user.setName("user");
        user.setDescription("desc");
        req().body(user)
                .post("/jaxrs/pojo")
                .then()
                .statusCode(200)
                .body("name", is("user"))
                .body("description", is("desc"));
    }

    public void async() {
        req().get("/jaxrs/async")
                .then()
                .statusCode(200)
                .body(is("async"));
    }

    public void params() {
        req().queryParam("queryParam", "query")
                .header("X-Header", "header")
                .cookie("_cookie", "cookie")
                .get("/jaxrs/params/path")
                .then()
                .statusCode(200)
                .body(is("path query header cookie"));
    }

    public void throughput() {
        long start = System.nanoTime();
        IntStream.range(0, 100)
                .parallel()
                .forEach(i -> {
                    byte[] bytes = randomBytes();
                    ValidatableResponse validatableResponse = req()
                            .body(bytes)
                            .post("/jaxrs")
                            .then()
                            .statusCode(200);
                    assertEquals(validatableResponse.extract().body().asByteArray(), bytes);
                });
        System.out.println(TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + "ms");
    }

    private static byte[] randomBytes() {
        int size = ThreadLocalRandom.current().nextInt(128, 1024);
        byte[] b = new byte[size];
        ThreadLocalRandom.current().nextBytes(b);
        return b;
    }
}
