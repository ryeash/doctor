package demo.app;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hamcrest.MatcherAssert;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;

@Test(invocationCount = 5)
public class HttpServerTest extends AbstractTestAppTest {

    private RequestSpecification req() {
        RestAssured.baseURI = "http://localhost:60222/";
        return RestAssured.given();
    }

    public void helloWorld() {
        req().get("/root/hello")
                .then()
                .statusCode(200)
                .body(is("Hello World!"));
    }

    public void helloWorldHead() {
        req().head("/root/hello")
                .then()
                .statusCode(200)
                .body(is(emptyString()));
    }

    public void paramTypes() {
        req().header("X-Param", "headerValue")
                .cookie("cook", "monster")
                .queryParam("size", 10)
                .get("/root/params/test")
                .then()
                .statusCode(200)
                .body(is("test 10 headerValue monster test 10 headerValue monster"))
                .header("X-After", is("true"));
    }

    public void requiredEndpointAnnotations() {
        req().post("/root")
                .then()
                .statusCode(200)
                .body(is("noPath"));
    }

    public void echo() {
        byte[] sent = randomBytes();
        byte[] returned = req()
                .body(sent)
                .put("/root/echo")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asByteArray();
        MatcherAssert.assertThat(sent, is(returned));
    }

    public void halt() {
        req().queryParam("halt", true)
                .get("/root/hello")
                .then()
                .statusCode(202)
                .body(is("halted"));
    }

    public void futureResponse() {
        req().get("/root/future")
                .then()
                .statusCode(200)
                .body(is("future"));
    }

    public void notFound() {
        req().get("/root/nothingtosehere")
                .then()
                .statusCode(404);
    }

    public void locale() {
        req()
                .header("Accept-Language", "en-US,en;q=0.9")
                .get("/root/locale")
                .then()
                .statusCode(200)
                .body(containsString("en"));
    }

    public void splat() {
        req().get("/root/splat/this/is/the/full/path")
                .then()
                .statusCode(200)
                .body(is("root/splat/this/is/the/full/path this/is/the/full/path"));
    }

    public void errors() {
        req().options("/root/syncError")
                .then()
                .statusCode(500)
                .body(containsString("error"));

        req().options("/root/asyncError")
                .then()
                .statusCode(500)
                .body(containsString("error"));
    }

    public void tooLarge() {
        byte[] b = new byte[2 * 1024 * 1024];
        ThreadLocalRandom.current().nextBytes(b);
        req().body(b)
                .post("/root/throughput")
                .then()
                .statusCode(413);
    }

    public void throughput() {
        long start = System.nanoTime();
        ExecutorService background = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors() * 2);
        CompletableFuture.supplyAsync(() -> {
            IntStream.range(0, 100)
                    .parallel()
                    .forEach(i -> {
                        byte[] sent = randomBytes();
                        byte[] returned = req()
                                .body(sent)
                                .post("/root/throughput")
                                .then()
                                .statusCode(200)
                                .extract()
                                .body()
                                .asByteArray();
                        MatcherAssert.assertThat(sent, is(returned));
                    });
            return null;
        }, background).join();
        System.out.println(TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + "ms");
    }

    public void customMethod() throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpEntityEnclosingRequestBase request = new HttpEntityEnclosingRequestBase() {
                @Override
                public String getMethod() {
                    return "KILL";
                }
            };
            request.setURI(URI.create("http://localhost:60222/root/kill"));
            try (CloseableHttpResponse response = client.execute(request)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.flush();
                String responseString = out.toString(StandardCharsets.UTF_8);
                assertEquals(response.getStatusLine().getStatusCode(), 200);
                assertEquals(responseString, "kill");
            }
        }
    }

    public void json() {
        req().get("/root/json")
                .then()
                .statusCode(200)
                .body("name", is("Emerson"))
                .body("superhero", is(true));
    }

    private static byte[] randomBytes() {
        int size = ThreadLocalRandom.current().nextInt(128, 1024);
        byte[] b = new byte[size];
        ThreadLocalRandom.current().nextBytes(b);
        return b;
    }
}
