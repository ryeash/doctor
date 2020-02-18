package vest.doctor.netty;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import vest.doctor.ConfigurationFacade;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NettyConfiguration {
    private final ConfigurationFacade configurationFacade;

    public NettyConfiguration(ConfigurationFacade configurationFacade) {
        this.configurationFacade = configurationFacade;
    }

    public int getTcpManagementThreads() {
        return configurationFacade.get("doctor.netty.tcp.threads", 1, Integer::valueOf);
    }

    public String getTcpThreadPrefix() {
        return configurationFacade.get("doctor.netty.tcp.threadPrefix", "netty-tcp");
    }

    public int getWorkerThreads() {
        return configurationFacade.get("doctor.netty.worker.threads", 16, Integer::valueOf);
    }

    public String getWorkerThreadPrefix() {
        return configurationFacade.get("doctor.netty.worker.threadPrefix", "netty-worker");
    }

    public int getSocketBacklog() {
        return configurationFacade.get("doctor.netty.tcp.socketBacklog", 1024, Integer::valueOf);
    }

    public List<? extends InetSocketAddress> getListenAddresses() {
        return configurationFacade.getSplit("doctor.netty.bind", Function.identity())
                .map(s -> s.split(":"))
                .map(hp -> new InetSocketAddress(hp[0].trim(), Integer.parseInt(hp[1].trim())))
                .collect(Collectors.toList());
    }

    public SslContext getSslContext() {
        try {
            if (configurationFacade.get("doctor.netty.ssl.selfSigned", false, Boolean::valueOf)) {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            }

            String keyCertChainFile = configurationFacade.get("doctor.netty.ssl.keyCertChainFile");
            if (keyCertChainFile != null && !keyCertChainFile.isEmpty()) {
                String keyFile = Objects.requireNonNull(configurationFacade.get("doctor.netty.ssl.keyFile"), "missing ssl key file configuration");
                String keyPassword = configurationFacade.get("doctor.netty.ssl.keyPassword");
                return SslContextBuilder.forServer(new File(keyCertChainFile), new File(keyFile), keyPassword).build();
            }

            return null;
        } catch (Throwable t) {
            throw new RuntimeException("error configuring ssl", t);
        }
    }

    public int getMaxInitialLineLength() {
        return configurationFacade.get("doctor.netty.http.maxInitialLineLength", 8192, Integer::valueOf);
    }

    public int getMaxHeaderSize() {
        return configurationFacade.get("doctor.netty.http.maxHeaderSize", 8192, Integer::valueOf);
    }

    public int getMaxChunkSize() {
        return configurationFacade.get("doctor.netty.http.maxChunkSize", 8192, Integer::valueOf);
    }

    public boolean isValidateHeaders() {
        return configurationFacade.get("doctor.netty.http.validateHeaders", false, Boolean::valueOf);
    }

    public int getInitialBufferSize() {
        return configurationFacade.get("doctor.netty.http.initialBufferSize", 8192, Integer::valueOf);
    }

    public int getMaxContentLength() {
        return configurationFacade.get("doctor.netty.http.maxContentLength", 8388608, Integer::valueOf);
    }

    public boolean debugRequestRouting() {
        return configurationFacade.get("doctor.netty.http.debugRequestRouting", false, Boolean::valueOf);
    }
}
