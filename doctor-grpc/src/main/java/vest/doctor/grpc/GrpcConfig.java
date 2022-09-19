package vest.doctor.grpc;

import vest.doctor.Properties;
import vest.doctor.Property;
import vest.doctor.scheduled.Interval;

import java.util.Optional;

/**
 * Internal use. Encapsulates the configuration for the gRPC server.
 */
@Properties("grpc.")
public interface GrpcConfig {

    @Property("port")
    Optional<Integer> port();

    @Property("handshakeTimeout")
    Optional<Interval> handshakeTimeout();

    @Property("keepAlive")
    Optional<Interval> keepAlive();

    @Property("keepAliveTimeout")
    Optional<Interval> keepAliveTimeout();

    @Property("maxConnectionIdle")
    Optional<Interval> maxConnectionIdle();

    @Property("maxConnectionAge")
    Optional<Interval> maxConnectionAge();

    @Property("maxConnectionAgeGrace")
    Optional<Interval> maxConnectionAgeGrace();

    @Property("permitKeepAliveTime")
    Optional<Interval> permitKeepAliveTime();

    @Property("permitKeepAliveWithoutCalls")
    Optional<Boolean> permitKeepAliveWithoutCalls();

    @Property("maxInboundMessageSize")
    Optional<Integer> maxInboundMessageSize();

    @Property("maxInboundMetadataSize")
    Optional<Integer> maxInboundMetadataSize();

    @Property("ssl.certificate")
    Optional<String> sslCertificate();

    @Property("ssl.privateKey")
    Optional<String> sslPrivateKey();

    @Property("ssl.selfSigned")
    Optional<Boolean> sslSelfSigned();
}
