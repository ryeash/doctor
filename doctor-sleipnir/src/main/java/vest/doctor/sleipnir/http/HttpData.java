package vest.doctor.sleipnir.http;

import java.nio.ByteBuffer;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public interface HttpData {
    byte CR = 13;
    byte LF = 10;
    byte[] CR_LF = new byte[]{CR, LF};
    byte COLON = ':';
    byte SPACE = ' ';

    static String httpDate() {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("UTC")));
    }

    ByteBuffer serialize();
}
