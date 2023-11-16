package vest.doctor.ssf.ws;

public record Frame(boolean fin,
                    boolean rsv1,
                    boolean rsv2,
                    boolean rsv3,
                    OpCode opCode,
                    boolean mask,
                    long payloadLength,
                    int maskKey,
                    byte[] payload) {

//    public ByteBuffer toByteBuffer() {
//        ByteBuffer buf = ByteBuffer.allocate(14 + payload.length);
//        byte header = 0x00;
//        if (fin) {
//            header |= (byte) 0b10000000;
//        }
//        header |= opCode.code();
//
//        buf.put(header);
//
//        byte payloadL = 0x00;
//        if (mask) {
//            payloadL |= (byte) 0b10000000;
//        }
//        if (payloadLength <= 125) {
//            payloadL |= (byte) (Byte.MAX_VALUE | (byte) payloadLength);
//        } else if (payloadLength < Short.MAX_VALUE) {
//            payloadL |= (byte) 126;
//            short extendedPayloadLength = (short) (Short.MAX_VALUE | (short) payloadL);
//        } else {
//            payloadL |= (byte) 127;
//            long extendedPayloadLength = payloadLength;
//        }
//    }
}
