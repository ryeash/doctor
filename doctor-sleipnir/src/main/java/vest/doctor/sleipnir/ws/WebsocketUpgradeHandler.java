package vest.doctor.sleipnir.ws;

import vest.doctor.sleipnir.http.FullRequest;
import vest.doctor.sleipnir.http.FullResponse;
import vest.doctor.sleipnir.http.HttpException;
import vest.doctor.sleipnir.http.Status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class WebsocketUpgradeHandler {
    public static FullResponse upgrade(FullRequest request, String... supportedProtocols) {
        FullResponse response = new FullResponse();
        /*
         * GET /chat HTTP/1.1
         * Host: server.example.com
         * Upgrade: websocket
         * Connection: Upgrade
         * Sec-WebSocket-Key: x3JJHMbDL1EzLkh9GBhXDw==
         * Sec-WebSocket-Protocol: chat, superchat
         * Sec-WebSocket-Version: 13
         */
        String connection = request.getHeader("Connection");
        if (!Objects.equals(connection, "Upgrade")) {
            throw new IllegalArgumentException("not an upgrade request");
        }
        String upgrade = request.getHeader("Upgrade");
        if (!Objects.equals(upgrade, "websocket")) {
            throw new IllegalArgumentException("not a websocket upgrade request");
        }
        String key = request.getHeader("Sec-WebSocket-Key");
        if (key != null) {
            byte[] decode = Base64.getDecoder().decode(key);
            if (decode.length != 16) {
                throw new IllegalArgumentException("invalid Sec-WebSocket-Key header");
            }
            try {
                MessageDigest crypt = MessageDigest.getInstance("SHA-1");
                crypt.reset();
                crypt.update((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8));
                byte[] digest = crypt.digest();
                String secKeyAccept = Base64.getEncoder().encodeToString(digest);
                response.headers().set("Sec-WebSocket-Accept", secKeyAccept);
            } catch (Throwable e) {
                throw new HttpException(Status.INTERNAL_SERVER_ERROR, "failed to digest key");
            }
        }

        String version = request.getHeader("Sec-WebSocket-Version");
        if (!Objects.equals(version, "13")) {
            throw new IllegalArgumentException("invalid websocket version: " + version + ", only 13 is supported");
        }

        String protocol = request.getHeader("Sec-WebSocket-Protocol");
        if (protocol != null && supportedProtocols.length > 0) {
            Set<String> protocols = Arrays.stream(protocol.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            Set<String> toSupport = Arrays.stream(supportedProtocols)
                    .filter(protocols::contains)
                    .collect(Collectors.toSet());
            if (toSupport.isEmpty()) {
                throw new IllegalArgumentException("no supported protocols requested: " + protocols + ", supported: " + Arrays.toString(supportedProtocols));
            }
            response.headers().set("Sec-WebSocket-Protocol", String.join(",", toSupport));
        }

        /*
         * HTTP/1.1 101 Switching Protocols
         * Upgrade: websocket
         * Connection: Upgrade
         * Sec-WebSocket-Accept: HSmrc0sMlYUkAGmm5OPpG2HaGWk=
         * Sec-WebSocket-Protocol: chat
         */
        response.status(Status.SWITCHING_PROTOCOLS);
        response.headers().set("Upgrade", "websocket");
        response.headers().set("Connection", "Upgrade");
        return response;
    }
}
