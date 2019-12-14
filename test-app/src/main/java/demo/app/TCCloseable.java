package demo.app;

import vest.doctor.Eager;

import javax.inject.Singleton;

@Eager
@Singleton
public class TCCloseable implements AutoCloseable {

    public static boolean closed = false;

    @Override
    public void close() throws Exception {
        closed = true;
    }
}
