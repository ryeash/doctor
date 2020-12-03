package doctor.cluster.model;

public enum Direction {
    REQUEST(0),
    RESPONSE(1);

    private final byte id;

    Direction(int id) {
        this.id = (byte) id;
    }

    public byte id() {
        return id;
    }

    public static Direction from(byte id) {
        for (Direction value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        throw new IllegalArgumentException("unknown direction: " + id);
    }
}
