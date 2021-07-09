package vest.doctor.http.server.impl;

import io.netty.buffer.ByteBuf;
import vest.doctor.http.server.Part;

import java.nio.charset.StandardCharsets;

final class PartImpl implements Part {
    private final String type;
    private final String name;
    private final ByteBuf data;
    private final boolean last;

    public PartImpl(String type, String name, ByteBuf data, boolean last) {
        this.type = type;
        this.name = name;
        this.data = data;
        this.last = last;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public ByteBuf getData() {
        return data;
    }

    public boolean isLast() {
        return last;
    }

    @Override
    public String toString() {
        return "Part{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", data=" + data.toString(StandardCharsets.UTF_8) +
                '}';
    }
}
