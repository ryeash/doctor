package demo.app.grpc;

import io.grpc.stub.StreamObserver;
import jakarta.inject.Singleton;

@Singleton
public class ParrotImpl extends ParrotGrpc.ParrotImplBase {

    @Override
    public void speak(StringData request, StreamObserver<StringData> responseObserver) {
        String message = request.getStr();
        responseObserver.onNext(StringData.newBuilder()
                .setStr(message)
                .build());
        responseObserver.onCompleted();
    }
}
