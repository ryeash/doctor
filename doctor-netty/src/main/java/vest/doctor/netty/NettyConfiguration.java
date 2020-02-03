package vest.doctor.netty;

import io.netty.handler.ssl.SslContext;
import vest.doctor.ConfigurationFacade;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

class NettyConfiguration {
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
        // TODO
        return null;
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
        return configurationFacade.get("doctor.netty.http.initialBufferSize", 8388608, Integer::valueOf);
    }
}
