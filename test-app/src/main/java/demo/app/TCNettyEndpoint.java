package demo.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import vest.doctor.flow.Flo;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.http.server.rest.Endpoint;
import vest.doctor.http.server.rest.HttpMethod;
import vest.doctor.http.server.rest.Param;
import vest.doctor.http.server.rest.Path;
import vest.doctor.http.server.rest.R;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static vest.doctor.http.server.rest.Param.Type.Attribute;
import static vest.doctor.http.server.rest.Param.Type.Bean;
import static vest.doctor.http.server.rest.Param.Type.Body;
import static vest.doctor.http.server.rest.Param.Type.Header;
import static vest.doctor.http.server.rest.Param.Type.Provided;
import static vest.doctor.http.server.rest.Param.Type.Query;

@Singleton
@Path("netty")
public class TCNettyEndpoint {
    private static final Logger log = LoggerFactory.getLogger(TCNettyEndpoint.class);

    @Endpoint(method = HttpMethod.GET, path = "/hello")
    public String basic(@Param(type = Query) Optional<String> q,
                        @Param(type = Query, name = "number") int num,
                        @Param(type = Query, name = "number") Optional<Integer> optNum,
                        @Param(type = Attribute, name = "list") List<InputStream> streams,
                        @Param(type = Bean) NettyBeanParam<?> beanParam) {
        Assert.assertNull(streams);
        Assert.assertEquals(q.get(), beanParam.getQ().get());
        Assert.assertEquals(num, beanParam.getNum());
        Assert.assertEquals(beanParam.getNumberViaMethod(), num);
        return "ok " + q.orElse(null) + " " + num + " " + optNum.orElse(-1);
    }

    @Endpoint(method = HttpMethod.GET, path = "/hello2")
    public byte[] hello2() {
        Locale.forLanguageTag("");
        return "bytes".getBytes();
    }

    @Endpoint(method = {HttpMethod.GET, HttpMethod.POST}, path = "/goodbye")
    public CompletableFuture<String> goodbye(@Param(type = Body) String s) {
        return CompletableFuture.completedFuture("goodbye");
    }

    @Endpoint(method = {HttpMethod.GET, HttpMethod.POST}, path = "/usingr")
    public R usingR() {
        return R.ok()
                .header("used-r", true)
                .body("R");
    }

    @Endpoint(method = HttpMethod.POST, path = "/pojolist")
    public CompletableFuture<String> pojolist(@Param(type = Body) CompletableFuture<List<Person>> people) throws JsonProcessingException {
        return people.thenApply(l -> {
            try {
                return new ObjectMapper().writeValueAsString(l);
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Endpoint(method = HttpMethod.POST, path = "/pojo")
    public CompletableFuture<String> pojo(@Param(type = Body) CompletableFuture<Person> person) {
        log.info("pojo endpoint");
        return person.thenApply(p -> {
            try {
                return new ObjectMapper().writeValueAsString(p);
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Endpoint(method = HttpMethod.GET, path = "/headerparam")
    public R headerParam(@Param(type = Header, name = "x-param") String param) {
        return R.ok(param);
    }

    @Endpoint(method = HttpMethod.GET, path = "/void")
    public void returnsVoid() {
    }

    @Endpoint(method = HttpMethod.GET, path = "/file/*")
    public R staticFiles(Request ctx, @Param(type = Param.Type.Path, name = "*") String file) {
        log.info("{}", ctx.uri());
        return R.file(ctx, "./", file);
    }

    @Endpoint(method = HttpMethod.GET, path = "/paramtest/{normal}/{custom:\\d+}")
    public String paramtest(@Param(type = Param.Type.Path, name = "normal") String normal,
                            @Param(type = Param.Type.Path) int custom,
                            @Param(type = Provided) @Named("pourOver") CoffeeMaker pourOver) {
        return normal + " " + custom + " " + pourOver.brew();
    }

    @Endpoint(method = HttpMethod.GET, path = "/attribute")
    public String attribute(@Param(type = Attribute) String attr) {
        return attr;
    }

    @Endpoint(method = HttpMethod.ANY, path = "/anything")
    public String any(Request request) {
        return request.method().toString();
    }

    @Endpoint(method = HttpMethod.GET, path = "/locale")
    public String locale(@Param(type = Header, name = "Accept-Language") Locale locale) {
        return locale.toString();
    }

    @Endpoint(method = HttpMethod.GET, path = "/fullresponse")
    public CompletableFuture<Response> responder(Response response,
                                                 @Param(type = Body) CompletableFuture<String> body) {
        return body
                .thenApply(ResponseBody::of)
                .thenApplyAsync(response::body);
    }

    @Endpoint(method = HttpMethod.POST, path = "/flo")
    public Flo<?, R> floResponse(Request request) {
        return request.body()
                .asString()
                .map(R::ok);
    }
}
