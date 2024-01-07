package vest.sleipnir;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import vest.doctor.rx.FlowBuilder;
import vest.sleipnir.http.AbstractHttpInitializer;
import vest.sleipnir.http.HttpData;
import vest.sleipnir.http.ProtocolVersion;
import vest.sleipnir.http.RequestAggregator;
import vest.sleipnir.http.Status;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class MiscTest {

    private Server server;

    @BeforeClass(alwaysRun = true)
    public void start() {
        ExecutorService executorService = Executors.newFixedThreadPool(16);
        this.server = Server.start(new Configuration(
                "0.0.0.0",
                32123,
                17,
                13,
                false,
                new AbstractHttpInitializer() {
                    @Override
                    protected Flow.Publisher<HttpData> handleData(ChannelContext channelContext, Flow.Publisher<HttpData> dataInput) {
                        return FlowBuilder.start(dataInput)
                                .chain(new RequestAggregator(channelContext))
                                .<HttpData.FullRequest>onNext((data, downstream) -> {
                                    executorService.submit(() -> downstream.onNext(data));
                                })
                                .onNext((data, downstream) -> {
                                    System.out.println(Thread.currentThread().getName());
                                    System.out.println(data);

                                    if (ThreadLocalRandom.current().nextBoolean()) {
                                        downstream.onNext(new HttpData.FullResponse(new HttpData.StatusLine(ProtocolVersion.HTTP1_1, Status.OK), List.of(
                                                new HttpData.Header("Date", HttpData.httpDate()),
                                                new HttpData.Header("Server", "sleipnir"),
                                                new HttpData.Header("Transfer-Encoding", "chunked")
                                        ), BufferUtils.copy(data.body())));
                                    } else {
                                        downstream.onNext(new HttpData.Response(new HttpData.StatusLine(ProtocolVersion.HTTP1_1, Status.OK), List.of(
                                                new HttpData.Header("Date", HttpData.httpDate()),
                                                new HttpData.Header("Server", "sleipnir"),
                                                new HttpData.Header("Transfer-Encoding", "chunked")
                                        )));
                                        downstream.onNext(new HttpData.Body(BufferUtils.copy(data.body()), false));
                                        downstream.onNext(HttpData.Body.LAST_EMPTY);
                                    }
                                });
                    }
                }
        ));
    }

    @AfterClass(alwaysRun = true)
    public void stop() {
        server.stop();
    }

    @Test
    public void foo() throws IOException, InterruptedException {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("localhost", 32123));

            s.getOutputStream().write("hello-----------------------------------------goodbye\n\r".getBytes(StandardCharsets.UTF_8));

            byte[] buf = new byte[256];
            for (int i = 0; i < 5; i++) {
                int read = s.getInputStream().read(buf);
                if (read > 0) {
                    System.out.println(new String(buf, 0, read, StandardCharsets.UTF_8));
                }
                if (read == -1) {
                    s.close();
                    break;
                }
            }
        }
    }

    @Test
    public void http() throws IOException {
        IntStream.range(0, 101)
                .parallel()
                .forEach(i -> {
                    try {
                        HttpURLConnection c = (HttpURLConnection) URI.create("http://localhost:32123/sleipnir/test?param=toast")
                                .toURL()
                                .openConnection();
                        c.setRequestMethod("POST");
                        c.setRequestProperty("X-Cool", "very");
                        c.setDoOutput(true);
                        c.getOutputStream().write((i + " data").getBytes(StandardCharsets.UTF_8));
                        c.getOutputStream().flush();
                        System.out.println(c.getResponseCode());
                        c.getHeaderFields().forEach((k, v) -> {
                            System.out.println(k + ": " + v);
                        });
                        System.out.println(new String(c.getInputStream().readAllBytes(), StandardCharsets.UTF_8));

                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                });
    }
}
