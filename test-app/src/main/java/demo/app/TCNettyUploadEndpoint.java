//package demo.app;
//
//import jakarta.inject.Singleton;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import vest.doctor.http.server.MultiPartData;
//import vest.doctor.http.server.Request;
//import vest.doctor.http.server.rest.Endpoint;
//import vest.doctor.http.server.rest.HttpMethod;
//import vest.doctor.http.server.rest.Param;
//import vest.doctor.http.server.rest.Path;
//
//import java.util.concurrent.CompletableFuture;
//
//import static vest.doctor.http.server.rest.Param.Type.Body;
//
//@Singleton
//@Path("netty")
//public class TCNettyUploadEndpoint {
//    private static final Logger log = LoggerFactory.getLogger(TCNettyUploadEndpoint.class);
//
//    @Endpoint(method = HttpMethod.POST, path = "/upload")
//    public CompletableFuture<?> upload(Request request,
//                                       @Param(type = Body) MultiPartData body) {
//        log.info("{}", request);
//        CompletableFuture<?> f = new CompletableFuture<>();
//        body.parts()
//                .observe(p -> {
//                    if (p.last()) {
//                        f.complete(null);
//                    }
//                })
//                .subscribe();
//        return f;
//    }
//
//}
