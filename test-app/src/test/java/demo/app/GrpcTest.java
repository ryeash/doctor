package demo.app;

import demo.app.grpc.ParrotGrpc;
import demo.app.grpc.StringData;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.testng.annotations.Test;
import vest.doctor.grpc.GrpcUtils;

public class GrpcTest extends AbstractTestAppTest {

    @Test
    public void parrot() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 43011).usePlaintext().build();
        try {
            String test = "polly want a cracker";
            ParrotGrpc.ParrotBlockingStub client = ParrotGrpc.newBlockingStub(channel).withWaitForReady();
            StringData response = client.speak(StringData.newBuilder().setStr(test).build());
            assertEquals(response.getStr(), test);

            ParrotGrpc.ParrotFutureStub future = ParrotGrpc.newFutureStub(channel).withWaitForReady();
            response = GrpcUtils.listen(future.speak(StringData.newBuilder().setStr(test).build())).join();
            assertEquals(response.getStr(), test);
        } finally {
            channel.shutdownNow();
        }
    }
}
