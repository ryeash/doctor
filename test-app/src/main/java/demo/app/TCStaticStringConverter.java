package demo.app;

public class TCStaticStringConverter {
    public static TCStaticStringConverter valueOf(String str) {
        return new TCStaticStringConverter(str, str.length());
    }

    private TCStaticStringConverter(String value, int length) {
        this.value = value;
    }

    private final String value;

    public String getValue() {
        return value;
    }
}
