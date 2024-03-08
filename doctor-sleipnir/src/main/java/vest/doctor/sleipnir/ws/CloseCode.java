package vest.doctor.sleipnir.ws;

public enum CloseCode {
    NORMAL((short) 1000),
    GOING_AWAY((short) 1001),
    PROTOCOL_ERROR((short) 1002),
    UNACCEPTABLE((short) 1003),
    RESERVED((short) 1004),
    NO_STATUS((short) 1005),
    ABNORMAL((short) 1006),
    NOT_CONSISTENT((short) 1007),
    POLICY_VIOLATION((short) 1008),
    MESSAGE_TOO_BIG((short) 1009),
    NOT_NEGOTIATED((short) 1010),
    UNEXPECTED_ERROR((short) 1011),
    TLS_FAILURE((short) 1015),

    UNKNOWN((short) -1);

    private final short code;

    CloseCode(short code) {
        this.code = code;
    }

    public short code() {
        return code;
    }
}
