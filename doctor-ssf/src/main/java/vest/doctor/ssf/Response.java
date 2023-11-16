package vest.doctor.ssf;

public interface Response extends BaseMessage{
    Status status();

    void status(Status status);

    byte[] body();

    void body(String response);

    void body(byte[] response);
}
