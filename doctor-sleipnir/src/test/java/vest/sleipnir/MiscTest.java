package vest.sleipnir;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import vest.doctor.rx.FlowBuilder;
import vest.doctor.sleipnir.BufferUtils;
import vest.doctor.sleipnir.ChannelContext;
import vest.doctor.sleipnir.Configuration;
import vest.doctor.sleipnir.Server;
import vest.doctor.sleipnir.http.FullRequest;
import vest.doctor.sleipnir.http.FullResponse;
import vest.doctor.sleipnir.http.HttpData;
import vest.doctor.sleipnir.http.HttpException;
import vest.doctor.sleipnir.http.HttpInitializer;
import vest.doctor.sleipnir.http.RequestAggregator;
import vest.doctor.sleipnir.http.Status;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;

public class MiscTest {

    private Server server;

    @BeforeClass(alwaysRun = true)
    public void start() {
        this.server = Server.start(new Configuration(
                "0.0.0.0",
                32123,
                17,
                13,
                false,
                new HttpInitializer(1024, 8 * 1024 * 1024) {
                    @Override
                    protected void handleData(ChannelContext channelContext, Flow.Publisher<HttpData> dataInput, Flow.Subscriber<HttpData> dataOutput) {
                        FlowBuilder.start(dataInput)
                                .chain(new RequestAggregator(channelContext))
                                .parallel(Executors.newFixedThreadPool(7))
                                .onNext(data -> {
                                    if (data instanceof FullRequest fullRequest) {
                                        FullResponse response = new FullResponse();
                                        response.status(Status.OK);
                                        response.headers().set("Date", HttpData.httpDate());
                                        response.headers().set("Server", "sleipnir");
                                        response.headers().set("x-thread", Thread.currentThread().getName());
                                        response.body(fullRequest.body());
                                    } else {
                                        throw new UnsupportedOperationException();
                                    }
                                })
                                .chain(dataOutput);
                    }
                }
        ));
    }

    @AfterClass(alwaysRun = true)
    public void stop() {
        server.stop();
    }

    private RequestSpecification req() {
        RestAssuredConfig config = RestAssured.config();
        config.getDecoderConfig().useNoWrapForInflateDecoding(true);
        return RestAssured.given()
                .config(config)
                .baseUri("http://localhost:32123");
    }

    @Test
    public void foo() {
        req().body("goodbye world, I'd like to be done now, and I'm taking matters into my own hands")
                .post("/hello")
                .prettyPeek();
    }

    @Test
    public void multipart() {
        req().multiPart("stuff", new File("C:/dev/doctor/doctor-sleipnir/pom.xml"))
                .post("/multipart")
                .prettyPeek();
    }

    @Test
    public void bufferIndex() {
        ByteBuffer test = ByteBuffer.wrap("this is a line\r\nthis is antoher\r\n".getBytes(StandardCharsets.UTF_8));
        System.out.println(BufferUtils.indexOf(test, (byte) '\r', (byte) '\n'));
        ByteBuffer dest = ByteBuffer.allocate(BufferUtils.indexOf(test, (byte) '\r', (byte) '\n'));
        BufferUtils.transfer(test, dest);
        dest.flip();
        System.out.println(BufferUtils.toString(dest));
    }

    @Test
    public void wsSha() {
        try {
            String key = "x3JJHMbDL1EzLkh9GBhXDw==";
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8));
            byte[] digest = crypt.digest();
            String secKeyAccept = Base64.getEncoder().encodeToString(digest);
            System.out.println(secKeyAccept);
        } catch (Throwable e) {
            throw new HttpException(Status.INTERNAL_SERVER_ERROR, "failed to digest key");
        }
    }
}
