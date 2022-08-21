package app.ext.sub;

import app.ext.Widget;
import vest.doctor.ThreadLocal;

@ThreadLocal
public class RotatingWidget implements Widget {
    @Override
    public String wonk() {
        return "rotate";
    }
}
