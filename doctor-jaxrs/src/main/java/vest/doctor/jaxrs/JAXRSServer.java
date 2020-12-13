package vest.doctor.jaxrs;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import vest.doctor.ProviderRegistry;

import javax.ws.rs.Path;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class JAXRSServer extends WebSocketServlet implements WebSocketCreator, AutoCloseable {
    private final JaxrsConfiguration jaxrsConfiguration;
    private final Map<String, Object> pathToWebsocket;

    private final Server server;

    static ProviderRegistry providerRegistry;

    public JAXRSServer(ProviderRegistry providerRegistry) {
        JAXRSServer.providerRegistry = providerRegistry;
        this.jaxrsConfiguration = new JaxrsConfiguration(providerRegistry.configuration());
        this.pathToWebsocket = new HashMap<>();
        this.server = startServer(providerRegistry);
    }

    public boolean isStarted() {
        return server != null && server.isStarted();
    }

    @Override
    public void close() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.setCreator(this);
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest, ServletUpgradeResponse servletUpgradeResponse) {
        return pathToWebsocket.get(servletUpgradeRequest.getRequestPath());
    }

    private Server startServer(ProviderRegistry providerRegistry) {
        List<InetSocketAddress> bindAddresses = jaxrsConfiguration.bindAddresses();
        if (bindAddresses.isEmpty()) {
            // don't start the server is no addresses are listed
            return null;
        }

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath(jaxrsConfiguration.rootPath());

        QueuedThreadPool qtp = new QueuedThreadPool(jaxrsConfiguration.maxRequestThreads(), jaxrsConfiguration.minRequestThreads());
        qtp.setName(jaxrsConfiguration.requestThreadPrefix());
        qtp.setIdleTimeout(jaxrsConfiguration.requestThreadIdleTimeout());
        qtp.setDaemon(false);

        Server server = new Server(qtp);

        GzipHandler gzip = new GzipHandler();
        gzip.setMinGzipSize(jaxrsConfiguration.minGzipSize());
        gzip.setIncludedMethods("GET", "PUT", "POST", "DELETE");
        gzip.setExcludedMethods();
        gzip.setHandler(context);
        server.setHandler(gzip);

        for (InetSocketAddress address : jaxrsConfiguration.bindAddresses()) {
            HttpConfiguration httpConfig = new HttpConfiguration();
            httpConfig.setRequestHeaderSize(jaxrsConfiguration.maxRequestHeaderSize());
            httpConfig.setResponseHeaderSize(jaxrsConfiguration.maxResponseHeaderSize());
            httpConfig.setIdleTimeout(jaxrsConfiguration.socketIdleTimeout());
            httpConfig.setSendDateHeader(true);
            httpConfig.setSendXPoweredBy(false);
            httpConfig.setSendServerVersion(false);

            ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
            connector.setHost(address.getHostName());
            connector.setPort(address.getPort());
            connector.setAcceptQueueSize(jaxrsConfiguration.socketBacklog());
            connector.setIdleTimeout(jaxrsConfiguration.socketIdleTimeout());
            server.addConnector(connector);
        }
        providerRegistry.getProvidersWithAnnotation(WebSocket.class).forEach(this::addWebsocket);

        ServletHolder restEasyServlet = context.addServlet(HttpServletDispatcher.class, "/");
        restEasyServlet.setInitParameter("javax.ws.rs.Application", RESTEasyAppConfig.class.getCanonicalName());
        context.addServlet(restEasyServlet, "/*");

        ServletHolder websocketServlet = new ServletHolder(this);
        context.addServlet(websocketServlet, jaxrsConfiguration.websocketRootPath() + "/*");

        context.setMaxFormContentSize(jaxrsConfiguration.maxRequestBodySize());
        context.setDisplayName("doctor");
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException("error starting jetty", e);
        }
        return server;
    }

    private void addWebsocket(Object ws) {
        Path path = ws.getClass().getAnnotation(Path.class);
        if (path == null) {
            throw new IllegalStateException("websocket classes must have a declared @Path: missing on " + ws.getClass());
        }
        StringBuilder spec = new StringBuilder();
        if (!path.value().startsWith(jaxrsConfiguration.websocketRootPath())) {
            spec.append(jaxrsConfiguration.websocketRootPath()).append('/');
        }
        spec.append(path.value());
        String pathSpec = JaxrsConfiguration.squeeze(spec.toString(), '/');
        pathToWebsocket.put(pathSpec, ws);
    }
}
