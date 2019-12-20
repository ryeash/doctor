package vest.doctor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;
import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class JAXRSServer extends WebSocketServlet implements WebSocketCreator, AutoCloseable {

    private static final List<Class<?>> JAX_RS_TYPES = Arrays.asList(
            ContainerRequestFilter.class,
            ContainerResponseFilter.class,
            ExceptionMapper.class,
            MessageBodyWriter.class,
            MessageBodyReader.class,
            ReaderInterceptor.class,
            WriterInterceptor.class,
            Provider.class,
            Path.class);

    private final ConfigurationFacade configurationFacade;
    private final JaxrsConfiguration jaxrsConfiguration;
    private final Map<String, Object> pathToWebsocket;

    private Server server;

    @Inject
    public JAXRSServer(ConfigurationFacade configurationFacade, JaxrsConfiguration jaxrsConfiguration) {
        this.configurationFacade = configurationFacade;
        this.jaxrsConfiguration = jaxrsConfiguration;
        this.pathToWebsocket = new HashMap<>();
    }

    @EventListener
    public void startupMessage(ApplicationStartedEvent startedEvent) {
        this.server = startServer(startedEvent.beanProvider());
    }

    @Override
    public void close() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    private Server startServer(BeanProvider beanProvider) {
        List<InetSocketAddress> bindAddresses = jaxrsConfiguration.bindAddresses();
        if (bindAddresses.isEmpty()) {
            return null;
        }

        ServletContextHandler context = new ServletContextHandler();
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

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.addProperties((Map) configurationFacade.toProperties());
//        resourceConfig.property("jersey.config.server.tracing.type", "ALL");
//        resourceConfig.property("jersey.config.server.tracing.threshold", "VERBOSE");

        ObjectMapper mapper = beanProvider.getProviderOpt(ObjectMapper.class)
                .map(Provider::get)
                .orElseGet(() -> new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                        .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
                        .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                        .setDefaultMergeable(true)
                        .registerModules(ObjectMapper.findModules()));

        resourceConfig.register(new JacksonJsonProvider(mapper));
        resourceConfig.register(new GZipDecoder());

        JAX_RS_TYPES.stream()
                .flatMap(type -> {
                    if (type.isAnnotation()) {
                        return beanProvider.getProvidersWithAnnotation((Class<? extends Annotation>) type);
                    } else {
                        return beanProvider.getProviders(type);
                    }
                })
                .map(Provider::get)
                .distinct()
                .forEach(resourceConfig::register);

        beanProvider.getProvidersWithAnnotation(WebSocket.class).forEach(this::addWebsocket);

        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(resourceConfig));
        context.addServlet(jerseyServlet, "/*");

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

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.setCreator(this);
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest, ServletUpgradeResponse servletUpgradeResponse) {
        return pathToWebsocket.get(servletUpgradeRequest.getRequestPath());
    }
}
