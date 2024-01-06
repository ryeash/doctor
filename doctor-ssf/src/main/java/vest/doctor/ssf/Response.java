package vest.doctor.ssf;

import vest.doctor.ssf.impl.ResponseImpl;

import java.nio.channels.ReadableByteChannel;

public interface Response extends HeaderContainer, AttributeContainer {

    Status status();

    void status(Status status);

    ReadableByteChannel body();

    void body(ReadableByteChannel data);

    void body(byte[] body);

    void body(String body);

    static Response ok() {
        return of(Status.OK);
    }

    static Response of(Status status) {
        ResponseImpl impl = new ResponseImpl();
        impl.status(status);
        return impl;
    }
}
