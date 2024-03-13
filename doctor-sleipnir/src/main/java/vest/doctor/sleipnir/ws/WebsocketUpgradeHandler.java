package vest.doctor.sleipnir.ws;

import vest.doctor.sleipnir.ChannelContext;
import vest.doctor.sleipnir.http.FullRequest;
import vest.doctor.sleipnir.http.FullResponse;
import vest.doctor.sleipnir.http.HTTPDecoder;
import vest.doctor.sleipnir.http.Headers;
import vest.doctor.sleipnir.http.HttpData;
import vest.doctor.sleipnir.http.HttpException;
import vest.doctor.sleipnir.http.Status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class WebsocketUpgradeHandler {

    private static final String WS_SECRET_HASH_KEY = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public HttpData upgrade(ChannelContext channelContext, HttpData httpData) {
        if (!(httpData instanceof FullRequest request)) {
            throw new UnsupportedOperationException("only full requests are supported");
        }
        FullResponse response = new FullResponse();

        String connection = request.getHeader(Headers.CONNECTION);
        if (!Objects.equals(connection, Headers.UPGRADE)) {
            throw new IllegalArgumentException("not an upgrade request");
        }
        String upgrade = request.getHeader(Headers.UPGRADE);
        if (!Objects.equals(upgrade, Headers.WEBSOCKET)) {
            throw new IllegalArgumentException("not a websocket upgrade request");
        }
        String key = request.getHeader(Headers.SEC_WEBSOCKET_KEY);
        if (key != null) {
            byte[] decode = Base64.getDecoder().decode(key);
            if (decode.length != 16) {
                throw new IllegalArgumentException("invalid Sec-WebSocket-Key header");
            }
            try {
                MessageDigest crypt = MessageDigest.getInstance("SHA-1");
                crypt.reset();
                crypt.update((key + WS_SECRET_HASH_KEY).getBytes(StandardCharsets.UTF_8));
                byte[] digest = crypt.digest();
                String secKeyAccept = Base64.getEncoder().encodeToString(digest);
                response.headers().set(Headers.SEC_WEBSOCKET_ACCEPT, secKeyAccept);
            } catch (Throwable e) {
                throw new HttpException(Status.INTERNAL_SERVER_ERROR, "failed to digest key");
            }
        }

        String version = request.getHeader(Headers.SEC_WEBSOCKET_VERSION);
        Set<String> protocol = Optional.ofNullable(request.getHeader(Headers.SEC_WEBSOCKET_PROTOCOL))
                .map(p -> p.split(","))
                .stream()
                .flatMap(Arrays::stream)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        upgrade(channelContext, request, response, version, protocol);

        response.status(Status.SWITCHING_PROTOCOLS);
        response.headers().set(Headers.UPGRADE, Headers.WEBSOCKET);
        response.headers().set(Headers.CONNECTION, Headers.UPGRADE);
        channelContext.attributes().put(HTTPDecoder.WS_UPGRADED, true);
        return response;
    }

    protected void upgrade(ChannelContext channelContext, FullRequest request, FullResponse response, String version, Set<String> protocols) {
        // no-op
    }
}
