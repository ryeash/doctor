package vest.doctor.http.server.impl;

import io.netty.buffer.ByteBuf;
import vest.doctor.http.server.Part;

public record PartImpl(String type, String name, ByteBuf data, boolean last) implements Part {
}
