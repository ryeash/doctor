package demo.app;

import jakarta.inject.Singleton;
import vest.doctor.Eager;

@Eager
@Singleton
public class TCCloseable implements AutoCloseable {

    public static boolean closed = false;

    @Override
    public void close() {
        closed = true;
    }
}
