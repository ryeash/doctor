package vest.doctor.ssf;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

public class SSFTest {
    @BeforeClass(alwaysRun = true)
    public void start() {
        SSF.setPort(41321);
        SSF.get("/hello/{name}", (ctx, request) -> {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("goodbye world!".getBytes(StandardCharsets.UTF_8)));
            response.headers().add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            return response;
        });
        SSF.start();
    }

    private RequestSpecification req() {
        RestAssured.baseURI = "http://localhost:41321/";
        return RestAssured.given();
    }

    @Test
    public void basic() {
        for (int i = 0; i < 10; i++) {
            long start = System.currentTimeMillis();
            req().get("/hello/dumbo")
                    .prettyPeek();
            System.out.println((System.currentTimeMillis() - start) + "ms");
        }
    }
}
