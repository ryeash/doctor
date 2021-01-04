package demo.app;

import vest.doctor.DestroyMethod;
import vest.doctor.Eager;

import javax.inject.Singleton;

@Eager
@Singleton
@DestroyMethod("destroy")
public class TCCustomCloseable {

    public static boolean closed = false;

    public void destroy() {
        closed = true;
    }
}
