package vest.doctor.ssf;

public interface AttributeContainer {
    void attribute(String name, Object value);

    <T> T attribute(String name);
}
