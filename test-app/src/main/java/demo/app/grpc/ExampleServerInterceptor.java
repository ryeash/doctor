package demo.app.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import jakarta.inject.Singleton;

@Singleton
public class ExampleServerInterceptor implements ServerInterceptor {

    public static final Metadata.Key<String> FILTER_KEY = Metadata.Key.of("demo.app.filterKey", new Metadata.AsciiMarshaller<>() {
        @Override
        public String toAsciiString(String value) {
            return value;
        }

        @Override
        public String parseAsciiString(String serialized) {
            return serialized;
        }
    });

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        headers.put(FILTER_KEY, "filtered");
        System.out.println(headers);
        System.out.println(call);
        return next.startCall(call, headers);
    }
}
