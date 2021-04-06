package demo.app;

import jakarta.inject.Singleton;
import vest.doctor.DestroyMethod;
import vest.doctor.Eager;

@Eager
@Singleton
@DestroyMethod("destroy")
public class TCCustomCloseable {

    public static boolean closed = false;

    public void destroy() {
        closed = true;
    }
}
