package vest.doctor;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class JaxrsConfiguration {
    private final ConfigurationFacade configurationFacade;

    @Inject
    public JaxrsConfiguration(ConfigurationFacade configurationFacade) {
        this.configurationFacade = configurationFacade;
    }

    public String rootPath() {
        return configurationFacade.get("jaxrs.rootPath", "/");
    }

    public int maxRequestThreads() {
        return configurationFacade.get("jaxrs.maxRequestThreads", 16, Integer::valueOf);
    }

    public int minRequestThreads() {
        return configurationFacade.get("jaxrs.minRequestThreads", 1, Integer::valueOf);
    }

    public String requestThreadPrefix() {
        return configurationFacade.get("jaxrs.threadPrefix", "request");
    }

    public int requestThreadIdleTimeout() {
        return configurationFacade.get("jaxrs.threadIdleTimeout", 120000, Integer::valueOf);
    }

    public int minGzipSize() {
        return configurationFacade.get("jaxrs.minGzipSize", 814, Integer::valueOf);
    }

    public List<InetSocketAddress> bindAddresses() {
        List<String> list = configurationFacade.getList("jaxrs.bind", String::valueOf);
        return list.stream()
                .map(s -> s.split(":"))
                .map(hp -> new InetSocketAddress(hp[0].trim(), Integer.parseInt(hp[1].trim())))
                .collect(Collectors.toList());
    }

    public int maxRequestHeaderSize() {
        return configurationFacade.get("jaxrs.maxRequestHeaderSize", 8192, Integer::valueOf);
    }

    public int maxResponseHeaderSize() {
        return configurationFacade.get("jaxrs.maxResponseHeaderSize", 8192, Integer::valueOf);
    }

    public int maxRequestBodySize() {
        return configurationFacade.get("jaxrs.maxRequestBodySize", 2097152, Integer::valueOf);
    }

    public int socketIdleTimeout() {
        return configurationFacade.get("jaxrs.socketIdleTimeout", 60000, Integer::valueOf);
    }

    public int socketBacklog() {
        return configurationFacade.get("jaxrs.socketBacklog", 1024, Integer::valueOf);
    }

    public String websocketRootPath() {
        return configurationFacade.get("jaxrs.websocketRootPath", "/ws");
    }

    public static String squeeze(String s, char c) {
        char[] a = s.toCharArray();
        int n = 1;
        for (int i = 1; i < a.length; i++) {
            a[n] = a[i];
            if (a[n] != c) {
                n++;
            } else if (a[n - 1] != c) {
                n++;
            }
        }
        return new String(a, 0, n);
    }
}
