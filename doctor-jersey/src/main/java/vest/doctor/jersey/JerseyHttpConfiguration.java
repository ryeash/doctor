package vest.doctor.jersey;

import io.netty.handler.ssl.SslContext;
import vest.doctor.Properties;
import vest.doctor.Property;

import java.util.List;
import java.util.Optional;

@Properties("doctor.jersey.http.")
public interface JerseyHttpConfiguration {
    /**
     * The number of threads used for TCP management. Defaults to 1 and should
     * rarely need any more than that.
     */
    @Property("tcp.threads")
    Optional<Integer> tcpManagementThreads();

    /**
     * The thread prefix / pool name to use for the main TCP management event loop group.
     */
    @Property("tcp.threadFormat")
    Optional<String> tcpThreadFormat();

    /**
     * The maximum queue length for incoming connection indications;
     * i.e. a request to connect. If an attempt to connect comes in
     * and the queue is full, the connection may be rejected, depending
     * on platform.
     */
    @Property("tcp.socketBacklog")
    Optional<Integer> socketBacklog();

    /**
     * The number of threads used to handle requests.
     */
    @Property("worker.threads")
    Optional<Integer> workerThreads();

    /**
     * The thread prefix / pool name to use for the worker thread event loop group.
     */
    @Property("worker.threadFormat")
    Optional<String> workerThreadFormat();

    /**
     * The list of addresses that the server will listen on.
     */
    @Property("bind")
    List<String> bindAddresses();

    /**
     * The cert file to use to build the {@link SslContext}.
     */
    @Property("ssl.certificate")
    Optional<String> sslCertificate();

    /**
     * The private key file to use to build the {@link SslContext}.
     */
    @Property("ssl.privateKey")
    Optional<String> sslPrivateKey();

    /**
     * When true, programmatically build a self-signed ssl cert and use it to create
     * the {@link SslContext} for the server.
     */
    @Property("ssl.selfSigned")
    Optional<Boolean> sslSelfSigned();

    /**
     * The max size for the initial HTTP request line,
     * which is: METHOD URI HTTP_VERSION.
     */
    @Property("maxInitialLineLength")
    Optional<Integer> maxInitialLineLength();

    /**
     * The max size for a single header, including name and value.
     */
    @Property("maxHeaderSize")
    Optional<Integer> maxHeaderSize();

    /**
     * The maximum chunk size allowed for requests with Transfer-Encoding: chunked.
     */
    @Property("maxChunkSize")
    Optional<Integer> maxChunkSize();

    /**
     * Whether to validate request headers.
     */
    @Property("validateHeaders")
    Optional<Boolean> validateHeaders();

    /**
     * The starting size for the initial line parser.
     */
    @Property("initialBufferSize")
    Optional<Integer> initialBufferSize();

    /**
     * The maximum allowed size for request bodies.
     */
    @Property("maxContentLength")
    Optional<Integer> maxContentLength();

    /**
     * The minimum response size to enable gzip-ing the response body.
     */
    @Property("minGzipSize")
    Optional<Integer> minGzipSize();
}
