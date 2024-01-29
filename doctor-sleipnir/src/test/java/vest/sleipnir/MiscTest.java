package vest.sleipnir;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import vest.doctor.rx.FlowBuilder;
import vest.doctor.sleipnir.ChannelContext;
import vest.doctor.sleipnir.Configuration;
import vest.doctor.sleipnir.Server;
import vest.doctor.sleipnir.http.FullResponse;
import vest.doctor.sleipnir.http.Header;
import vest.doctor.sleipnir.http.HttpData;
import vest.doctor.sleipnir.http.HttpInitializer;
import vest.doctor.sleipnir.http.ProtocolVersion;
import vest.doctor.sleipnir.http.RequestAggregator;
import vest.doctor.sleipnir.http.Status;
import vest.doctor.sleipnir.http.StatusLine;

import java.util.List;
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
                                    return new FullResponse(new StatusLine(ProtocolVersion.HTTP1_1, Status.OK),
                                            List.of(new Header("Date", HttpData.httpDate()),
                                                    new Header("Server", "sleipnir"),
                                                    new Header("x-thread", Thread.currentThread().getName())),
                                            data.body());
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
}
