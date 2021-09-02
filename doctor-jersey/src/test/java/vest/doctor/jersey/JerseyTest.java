package vest.doctor.jersey;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import vest.doctor.runtime.Doctor;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.is;

public class JerseyTest {

    private Doctor doctor;

    @BeforeClass(alwaysRun = true)
    public void setup() {
        doctor = Doctor.load();
    }

    @AfterClass(alwaysRun = true)
    public void shutdown() {
        doctor.close();
    }

    private RequestSpecification req() {
        RestAssured.baseURI = "http://localhost:9998/";
        return RestAssured.given()
                .accept("application/json")
                .contentType("application/json");
    }

    @Test
    public void init() {
        req().get("/jaxrs/get")
                .then()
                .statusCode(200)
                .body(is("ok"));
    }

    @Test
    public void halt() {
        req().queryParam("halt", "true")
                .get("/jaxrs/get")
                .then()
                .statusCode(503);
    }

    @Test(invocationCount = 3)
    public void throughput() {
        long start = System.nanoTime();
        IntStream.range(0, 1000)
                .parallel()
                .forEach(i -> req()
                        .body(randomBytes())
                        .post("/jaxrs/")
                        .then()
                        .statusCode(200));
        System.out.println(TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + "ms");
    }

    private static byte[] randomBytes() {
        int size = ThreadLocalRandom.current().nextInt(1024, 1024 * 20);
        byte[] b = new byte[size];
        ThreadLocalRandom.current().nextBytes(b);
        return b;
    }
}
