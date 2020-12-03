package doctor.cluster.serder;

import doctor.cluster.Jackson;

public class SerDer {

    public static byte[] serialize(Object o) {
        return Jackson.writeValueAsBytes(o);
    }

    public static <T> T deserialize(byte[] bytes, Class<T> type) {
        return Jackson.readValueFromBytes(bytes, type);
    }


}
