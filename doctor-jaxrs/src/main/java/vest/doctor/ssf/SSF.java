package vest.doctor.ssf;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.compression.StandardCompressionOptions;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AttributeKey;
import jakarta.ws.rs.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.CustomThreadFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Small, simple, fast http server for programmatic, fluent server building.
 */
public class SSF {

    private static final Logger log = LoggerFactory.getLogger(SSF.class);

    private static int tcpManagementThreads = 1;
    private static String tcpThreadFormat = "netty-tcp-%d";
    private static int workerThreads = 0;
    private static String workerThreadFormat = "netty-worker-%d";
    private static int port = 61013;
    private static int socketBacklog = 1024;

    private static int maxInitialLineLength = 8192;
    private static int maxHeaderSize = 8192;
    private static int maxChunkSize = 8192;
    private static boolean validateHeaders = false;
    private static int initialBufferSize = 8192;
    private static int minGzipSize = 814;
    private static int maxContentSize = 8192;
    private static SslContext sslContext = null;


    public static void setTcpManagementThreads(int tcpManagementThreads) {
        SSF.tcpManagementThreads = tcpManagementThreads;
    }

    public static void setTcpThreadFormat(String tcpThreadFormat) {
        SSF.tcpThreadFormat = tcpThreadFormat;
    }

    public static void setWorkerThreads(int workerThreads) {
        SSF.workerThreads = workerThreads;
    }

    public static void setWorkerThreadFormat(String workerThreadFormat) {
        SSF.workerThreadFormat = workerThreadFormat;
    }

    public static void setPort(int port) {
        SSF.port = port;
    }

    public static void setSocketBacklog(int socketBacklog) {
        SSF.socketBacklog = socketBacklog;
    }

    public static void setMaxInitialLineLength(int maxInitialLineLength) {
        SSF.maxInitialLineLength = maxInitialLineLength;
    }

    public static void setMaxHeaderSize(int maxHeaderSize) {
        SSF.maxHeaderSize = maxHeaderSize;
    }

    public static void setMaxChunkSize(int maxChunkSize) {
        SSF.maxChunkSize = maxChunkSize;
    }

    public static void setValidateHeaders(boolean validateHeaders) {
        SSF.validateHeaders = validateHeaders;
    }

    public static void setInitialBufferSize(int initialBufferSize) {
        SSF.initialBufferSize = initialBufferSize;
    }

    public static void setMinGzipSize(int minGzipSize) {
        SSF.minGzipSize = minGzipSize;
    }

    public static void setMaxContentSize(int maxContentSize) {
        SSF.maxContentSize = maxContentSize;
    }

    public static void setSslContext(SslContext sslContext) {
        SSF.sslContext = sslContext;
    }

    public static void get(String pathSpec, BiFunction<ChannelHandlerContext, FullHttpRequest, FullHttpResponse> handler) {
        HttpRequestHandler.INSTANCE.route(HttpMethod.GET, pathSpec, handler);
    }

    public static void put(String pathSpec, BiFunction<ChannelHandlerContext, FullHttpRequest, FullHttpResponse> handler) {
        HttpRequestHandler.INSTANCE.route(HttpMethod.PUT, pathSpec, handler);
    }

    public static void post(String pathSpec, BiFunction<ChannelHandlerContext, FullHttpRequest, FullHttpResponse> handler) {
        HttpRequestHandler.INSTANCE.route(HttpMethod.POST, pathSpec, handler);
    }

    public static void delete(String pathSpec, BiFunction<ChannelHandlerContext, FullHttpRequest, FullHttpResponse> handler) {
        HttpRequestHandler.INSTANCE.route(HttpMethod.DELETE, pathSpec, handler);
    }

    public static void route(HttpMethod method, String pathSpec, BiFunction<ChannelHandlerContext, FullHttpRequest, FullHttpResponse> handler) {
        HttpRequestHandler.INSTANCE.route(method, pathSpec, handler);
    }

    public static void start() {
        long start = System.currentTimeMillis();
        EventLoopGroup bossGroup = new NioEventLoopGroup(tcpManagementThreads, new CustomThreadFactory(false, tcpThreadFormat, null, null));
        EventLoopGroup workerGroup = new NioEventLoopGroup(workerThreads, new CustomThreadFactory(true, workerThreadFormat, null, null));
        SslContext localSslContext = sslContext;

        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .option(ChannelOption.SO_BACKLOG, socketBacklog)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        if (localSslContext != null) {
                            p.addLast(localSslContext.newHandler(ch.alloc()));
                        }

                        HttpDecoderConfig httpDecoderConfig = new HttpDecoderConfig();
                        httpDecoderConfig.setChunkedSupported(true);
                        httpDecoderConfig.setAllowPartialChunks(true);
                        httpDecoderConfig.setAllowDuplicateContentLengths(false);
                        httpDecoderConfig.setMaxInitialLineLength(maxInitialLineLength);
                        httpDecoderConfig.setMaxHeaderSize(maxHeaderSize);
                        httpDecoderConfig.setMaxChunkSize(maxChunkSize);
                        httpDecoderConfig.setValidateHeaders(validateHeaders);
                        httpDecoderConfig.setInitialBufferSize(initialBufferSize);

                        p.addLast(new HttpServerCodec(httpDecoderConfig));
                        p.addLast(new HttpContentDecompressor());
                        p.addLast(new HttpContentCompressor(minGzipSize, StandardCompressionOptions.gzip()));
                        p.addLast(new HttpServerExpectContinueHandler());
                        p.addLast(new ChunkedWriteHandler());
                        p.addLast(new HttpObjectAggregator(maxContentSize, true));
                        p.addLast(HttpRequestHandler.INSTANCE);
                    }
                });

        try {
            Channel channel = serverBootstrap.bind(port).sync().channel();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    bossGroup.shutdownGracefully().get();
                    workerGroup.shutdownGracefully().get();
                    channel.close();
                } catch (Throwable t) {
                    // ignored
                }
            }, "netty-ssf-shutdown"));
        } catch (InterruptedException e) {
            throw new ProcessingException(e);
        }
        log.info("SSF initialized in {}ms", (System.currentTimeMillis() - start));
    }

    @ChannelHandler.Sharable
    private static final class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        static final HttpRequestHandler INSTANCE = new HttpRequestHandler();
        private static final AttributeKey<Map<String, String>> PATH_PARAMS = AttributeKey.newInstance("doctor.ssf");
        private final Map<HttpMethod, List<Route>> routes = new HashMap<>();

        private HttpRequestHandler() {
        }

        public void route(HttpMethod method, String pathSpec, BiFunction<ChannelHandlerContext, FullHttpRequest, FullHttpResponse> handler) {
            List<Route> list = routes.computeIfAbsent(method, v -> new LinkedList<>());
            list.add(new Route(method, new PathSpec(pathSpec, true), handler));
            Collections.sort(list);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
            if (HttpUtil.is100ContinueExpected(fullHttpRequest)) {
                ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
            }
            FullHttpResponse response = handle(ctx, fullHttpRequest);
            boolean keepAlive = HttpUtil.isKeepAlive(fullHttpRequest) && HttpUtil.isKeepAlive(response);
            ctx.writeAndFlush(response).addListener(keepAlive ? ChannelFutureListener.CLOSE_ON_FAILURE : ChannelFutureListener.CLOSE);
        }

        private FullHttpResponse handle(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
            for (Route route : routes.getOrDefault(fullHttpRequest.method(), List.of())) {
                if (route.pathSpec.matchAndCollect(fullHttpRequest.uri()) instanceof Map<String, String> params) {
                    ctx.channel().attr(PATH_PARAMS).set(params);
                    return route.handler.apply(ctx, fullHttpRequest);
                }
            }
            return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_FOUND, Unpooled.wrappedBuffer(("not found " + fullHttpRequest.method() + " " + fullHttpRequest.uri()).getBytes(StandardCharsets.UTF_8)));
        }
    }

    record Route(HttpMethod method,
                 PathSpec pathSpec,
                 BiFunction<ChannelHandlerContext, FullHttpRequest, FullHttpResponse> handler) implements Comparable<Route> {
        @Override
        public int compareTo(Route o) {
            return pathSpec.compareTo(o.pathSpec);
        }
    }

    // TODO
    record RequestContext(ChannelHandlerContext ctx, FullHttpRequest request) {
        public Map<String, String> pathParams() {
            return ctx.channel().attr(HttpRequestHandler.PATH_PARAMS).get();
        }
    }

    private static final class PathSpec implements Comparable<PathSpec> {
        private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{.*?}");
        private static final Pattern SPLAT_PARAM_PATTERN = Pattern.compile("/\\*");
        private static final String DEFAULT_PARAM_REGEX = "[^/]+?";

        private final String path;
        private final List<String> paramNames;
        private final Pattern pattern;

        public PathSpec(String path, boolean caseInsensitive) {
            if (path.isEmpty()) {
                throw new IllegalArgumentException("the route path may not be empty");
            }
            Objects.requireNonNull(path, "route path uri may not be null");

            this.path = path;
            this.paramNames = new ArrayList<>(3);

            // build the regex pattern uri and extract parameter names
            // pre-process to handle '*'
            String temp = SPLAT_PARAM_PATTERN.matcher(path).replaceAll("/{*:.*}");

            StringBuilder builder = new StringBuilder("^");
            int i = 0;
            Matcher matcher = PATH_PARAM_PATTERN.matcher(temp);
            while (matcher.find()) {
                builder.append(temp, i, matcher.start());
                String section = matcher.group().trim();
                String name = section.substring(1, section.length() - 1);
                String paramRegex = DEFAULT_PARAM_REGEX;
                int index = name.indexOf(':');
                if (index >= 0) {
                    paramRegex = name.substring(index + 1);
                    name = name.substring(0, index);
                }
                paramNames.add(name);
                builder.append('(').append(paramRegex).append(')');
                i = matcher.end();
            }
            builder.append(temp, i, temp.length());
            this.pattern = Pattern.compile(builder.toString(), caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
        }

        /**
         * Match the request uri to this patch spec, returning a non-null map if
         * there is a match.
         *
         * @param requestUri the request uri to match
         * @return a non-null map if the uri matched this spec, else null
         */
        public Map<String, String> matchAndCollect(String requestUri) {
            Matcher matcher = pattern.matcher(requestUri);
            if (!matcher.matches()) {
                return null;
            }
            if (paramNames.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, String> params = new LinkedHashMap<>(matcher.groupCount(), 1.0F);
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                String name = paramNames.get(i - 1);
                params.put(name, group);
            }
            return params;
        }

        @Override
        public String toString() {
            return path + "  pattern:" + pattern + " " + paramNames;
        }

        @Override
        public int compareTo(PathSpec o) {
            // compare specificity of the path
            for (int i = 0; i < path.length() && i < o.path.length(); i++) {
                int ca = path.charAt(i);
                int cb = o.path.charAt(i);
                if (ca != cb) {
                    return Integer.compare(charSortValue(ca), charSortValue(cb));
                }
            }

            // if all else fails, just do a length comparison
            return Integer.compare(path.length(), o.path.length());
        }

        private static int charSortValue(int c) {
            return switch (c) {
                case '{' -> 10000;
                case '*' -> 100000;
                default -> c; // default to sorting alphabetically
            };
        }
    }
}
