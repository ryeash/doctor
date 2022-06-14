package vest.doctor.http.server;

import io.netty.handler.ssl.SslContext;
import vest.doctor.http.server.impl.CompositeExceptionHandler;
import vest.doctor.http.server.impl.Router;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

public class HttpServerConfiguration {

    /**
     * Whether to match routes in {@link Router}
     * using case-insensitive regular expressions.
     */
    private boolean caseInsensitiveMatching = true;

    /**
     * Whether to add debug headers to the response when routing requests.
     */
    private boolean debugRequestRouting = false;

    /**
     * The prefix to prepend to all routes and filters registered with the {@link Router}.
     */
    private String routerPrefix = "";

    /**
     * The exception handler to use.
     */
    private ExceptionHandler exceptionHandler = new CompositeExceptionHandler();

    /**
     * The number of threads used for TCP management. Defaults to 1 and should
     * rarely need any more than that.
     */
    private int tcpManagementThreads = 1;

    /**
     * The thread prefix / pool name to use for the main TCP management event loop group.
     */
    private String tcpThreadFormat = "netty-tcp-%d";

    /**
     * The number of threads used to handle requests.
     */
    private int workerThreads = Runtime.getRuntime().availableProcessors();

    /**
     * The thread prefix / pool name to use for the worker thread event loop group.
     */
    private String workerThreadFormat = "netty-worker-%d";

    /**
     * The maximum queue length for incoming connection indications;
     * i.e. a request to connect. If an attempt to connect comes in
     * and the queue is full, the connection may be rejected, depending
     * on platform.
     */
    private int socketBacklog = 1024;

    /**
     * The list of addresses that the server will listen on.
     */
    private List<InetSocketAddress> bindAddresses;

    /**
     * The {@link SslContext} use to handle encrypted sockets. When non-null,
     * SSL will be enabled on all sockets, otherwise all sockets
     * will be cleartext.
     */
    private SslContext sslContext;

    /**
     * The max size for the initial HTTP request line,
     * which is: METHOD URI HTTP_VERSION.
     */
    private int maxInitialLineLength = 8192;

    /**
     * The max size for a single header, including name and value.
     */
    private int maxHeaderSize = 8192;

    /**
     * The maximum chunk size allowed for requests with Transfer-Encoding: chunked.
     */
    private int maxChunkSize = 8192;

    /**
     * Whether to validate request headers.
     */
    private boolean validateHeaders = false;

    /**
     * The starting size for the initial line parser.
     */
    private int initialBufferSize = 256;

    /**
     * The maximum allowed size for request bodies.
     */
    private int maxContentLength = 8388608;

    /**
     * The minimum response size to enable gzip-ing the response body.
     */
    private int minGzipSize = 812;

    /**
     * The PipelineCustomizers to apply to the netty pipeline.
     */
    private List<PipelineCustomizer> pipelineCustomizers;

    /**
     * The ServerBoostrapCustomizers to apply to the netty http server.
     */
    private List<ServerBootstrapCustomizer> serverBootstrapCustomizers;

    public int getTcpManagementThreads() {
        return tcpManagementThreads;
    }

    public void setTcpManagementThreads(int tcpManagementThreads) {
        this.tcpManagementThreads = tcpManagementThreads;
    }

    public String getTcpThreadFormat() {
        return tcpThreadFormat;
    }

    public void setTcpThreadFormat(String tcpThreadFormat) {
        this.tcpThreadFormat = tcpThreadFormat;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public String getWorkerThreadFormat() {
        return workerThreadFormat;
    }

    public void setWorkerThreadFormat(String workerThreadFormat) {
        this.workerThreadFormat = workerThreadFormat;
    }

    public int getSocketBacklog() {
        return socketBacklog;
    }

    public void setSocketBacklog(int socketBacklog) {
        this.socketBacklog = socketBacklog;
    }

    public List<InetSocketAddress> getBindAddresses() {
        return bindAddresses;
    }

    public void setBindAddresses(List<InetSocketAddress> bindAddresses) {
        this.bindAddresses = bindAddresses;
    }

    public void addBindAddress(InetSocketAddress bind) {
        if (bindAddresses == null) {
            this.bindAddresses = new LinkedList<>();
        }
        this.bindAddresses.add(bind);
    }

    public SslContext getSslContext() {
        return sslContext;
    }

    public void setSslContext(SslContext sslContext) {
        this.sslContext = sslContext;
    }

    public int getMaxInitialLineLength() {
        return maxInitialLineLength;
    }

    public void setMaxInitialLineLength(int maxInitialLineLength) {
        this.maxInitialLineLength = maxInitialLineLength;
    }

    public int getMaxHeaderSize() {
        return maxHeaderSize;
    }

    public void setMaxHeaderSize(int maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
    }

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public void setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    public boolean isValidateHeaders() {
        return validateHeaders;
    }

    public void setValidateHeaders(boolean validateHeaders) {
        this.validateHeaders = validateHeaders;
    }

    public int getInitialBufferSize() {
        return initialBufferSize;
    }

    public void setInitialBufferSize(int initialBufferSize) {
        this.initialBufferSize = initialBufferSize;
    }

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    public int getMinGzipSize() {
        return minGzipSize;
    }

    public void setMinGzipSize(int minGzipSize) {
        this.minGzipSize = minGzipSize;
    }

    public List<PipelineCustomizer> getPipelineCustomizers() {
        return pipelineCustomizers;
    }

    public void setPipelineCustomizers(List<PipelineCustomizer> pipelineCustomizers) {
        this.pipelineCustomizers = pipelineCustomizers;
    }

    public List<ServerBootstrapCustomizer> getServerBootstrapCustomizers() {
        return serverBootstrapCustomizers;
    }

    public void setServerBootstrapCustomizers(List<ServerBootstrapCustomizer> serverBootstrapCustomizers) {
        this.serverBootstrapCustomizers = serverBootstrapCustomizers;
    }

    public boolean getCaseInsensitiveMatching() {
        return caseInsensitiveMatching;
    }

    public void setCaseInsensitiveMatching(boolean caseInsensitiveMatching) {
        this.caseInsensitiveMatching = caseInsensitiveMatching;
    }

    public boolean isDebugRequestRouting() {
        return debugRequestRouting;
    }

    public void setDebugRequestRouting(boolean debugRequestRouting) {
        this.debugRequestRouting = debugRequestRouting;
    }

    public String getRouterPrefix() {
        return routerPrefix;
    }

    public void setRouterPrefix(String routerPrefix) {
        this.routerPrefix = routerPrefix;
    }

    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }
}
