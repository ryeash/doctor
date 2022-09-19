doctor-grpc
===

Boiler-plate gRPC support.

Add dependencies:
```xml
<dependency>
    <groupId>doctor</groupId>
    <artifactId>doctor-grpc</artifactId>
    <version>${doctor.version}</version>
</dependency>
<!-- Only required during build -->
<dependency>
    <groupId>org.apache.tomcat</groupId>
    <artifactId>annotations-api</artifactId>
    <version>6.0.53</version>
    <scope>provided</scope>
</dependency>
```

Add gRPC support, e.g. maven:
```xml
<properties>
    <grpc.version>1.49.0</grpc.version>
    <protoc.version>3.21.6</protoc.version>
</properties>

<build>
    <extensions>
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.6.2</version>
        </extension>
    </extensions>
    <plugins>
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.6.1</version>
            <configuration>
                <protocArtifact>com.google.protobuf:protoc:${protoc.version}:exe:${os.detected.classifier}
                </protocArtifact>
                <pluginId>grpc-java</pluginId>
                <pluginArtifact>io.grpc:protoc-gen-grpc-java:${grpc.version}:exe:${os.detected.classifier}
                </pluginArtifact>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>compile-custom</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Add import for doctor-grpc:

```java
@Singleton
@Import({"vest.doctor.grpc"})
public class AppConfig {
    // ...
}
```

Add your .proto and implement your server, ensuring it is marked with a scope:

`src/main/proto/parrot.proto`
```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "demo.app.grpc";

package grpc;

service Parrot {
  rpc Speak (StringData) returns (StringData) {}
}

message StringData {
  string str = 1;
}
```

```java
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
```

Add the necessary grpc configuration so the server starts.
```
grpc.port = 51011
```

Any of the follow types that are provided will be automatically wired into the grpc system as appropriate:
* BindableService.class
* ServerServiceDefinition.class
* ServerInterceptor.class
* ServerTransportFilter.class
* ServerStreamTracer.Factory.class
* HandlerRegistry.class
* BinaryLog.class