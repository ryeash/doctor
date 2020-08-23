package vest.doctor.netty;

import io.netty.handler.ssl.SslContext;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

/**
 * Configuration for the netty server.
 */
public class NettyConfiguration {
    private int tcpManagementThreads = 1;
    private String tcpThreadPrefix = "netty-tcp";
    private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;
    private String workerThreadPrefix = "netty-worker";
    private int socketBacklog = 1024;
    private List<InetSocketAddress> bindAddresses;
    private SslContext sslContext;
    private int maxInitialLineLength = 8192;
    private int maxHeaderSize = 8192;
    private int maxChunkSize = 8192;
    private boolean validateHeaders = false;
    private int initialBufferSize = 8192;
    private int maxContentLength = 8388608;
    private boolean caseInsensitiveMatching = true;

    public int getTcpManagementThreads() {
        return tcpManagementThreads;
    }

    public void setTcpManagementThreads(int tcpManagementThreads) {
        this.tcpManagementThreads = tcpManagementThreads;
    }

    public String getTcpThreadPrefix() {
        return tcpThreadPrefix;
    }

    public void setTcpThreadPrefix(String tcpThreadPrefix) {
        this.tcpThreadPrefix = tcpThreadPrefix;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public String getWorkerThreadPrefix() {
        return workerThreadPrefix;
    }

    public void setWorkerThreadPrefix(String workerThreadPrefix) {
        this.workerThreadPrefix = workerThreadPrefix;
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

    public boolean getCaseInsensitiveMatching() {
        return caseInsensitiveMatching;
    }

    public void setCaseInsensitiveMatching(boolean caseInsensitiveMatching) {
        this.caseInsensitiveMatching = caseInsensitiveMatching;
    }
}
