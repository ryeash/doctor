package demo.app;

import vest.doctor.DestroyMethod;
import vest.doctor.Reloadable;

@Reloadable
@DestroyMethod("destroy")
public class TCReloadable {

    public void destroy() {
    }
}
