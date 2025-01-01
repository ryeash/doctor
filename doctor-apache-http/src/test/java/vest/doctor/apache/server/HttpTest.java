package vest.doctor.apache.server;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;
import org.hamcrest.MatcherAssert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;
import vest.doctor.apache.server.impl.DefaultExceptionHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.is;

public class HttpTest {

    static final ServerContainer serverContainer = ServerContainer.start(new InetSocketAddress("localhost", 43221), Executors.newCachedThreadPool(), new DefaultExceptionHandler(), 1024, (bootstrap, router) -> {
        router.filter(Filter.after((ctx, response) ->
                        response.setHeader("X-After", "True")
                                .setHeader("X-Thread", Thread.currentThread().getName())))
                .get("/hello/{name}", ctx ->
                        ctx.collectBodyToString().whenComplete((body, error) -> {
                            AsyncResponseBuilder response = AsyncResponseBuilder.create(200)
                                    .setHeader("X-Thread", Thread.currentThread().getName())
                                    .setEntity("toast " + ctx.getPathParam("name"));
                            try {
                                ctx.send(response);
                            } catch (HttpException | IOException e) {
                                e.printStackTrace();
                            }
                        }))
                .post("/hello/{name}", ctx -> {
                    ctx.collectBodyToArray().whenComplete((data, error) -> {
                        String name = ctx.getPathParam("name");
                        try {
                            ctx.send(AsyncResponseBuilder.create(200)
                                    .setHeader("X-Thread", Thread.currentThread().getName())
                                    .setHeader("X-Param", name)
                                    .setEntity(data, ContentType.TEXT_PLAIN));
                        } catch (HttpException | IOException e) {
                            e.printStackTrace();
                        }
                    });
                });
    });

    @AfterSuite(alwaysRun = true)
    public void after() {
        serverContainer.close();
    }

    private RequestSpecification req() {
        return RestAssured.with()
                .baseUri("http://localhost:43221/");
    }

    @Test
    public void basic() {
        req().get("/hello/world")
                .prettyPeek()
                .then()
                .statusCode(200)
                .body(is("toast world"));
    }

    @Test
    public void filter() {
        req().get("/hello/world")
                .then()
                .statusCode(200)
                .header("X-After", is("True"));
    }

    @Test
    public void smallRequestBody() {
        req().body("toast: yes")
                .post("/hello/world")
                .prettyPeek()
                .then()
                .statusCode(200)
                .body(is("toast: yes"));
    }

    @Test
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
                                .post("/hello/world")
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

    private static byte[] randomBytes() {
        int size = ThreadLocalRandom.current().nextInt(128, 10024);
        byte[] b = new byte[size];
        ThreadLocalRandom.current().nextBytes(b);
        return b;
    }
}
