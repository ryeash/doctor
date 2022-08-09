package demo.app;

import jakarta.inject.Singleton;
import vest.doctor.aop.ArgValue;
import vest.doctor.aop.Aspect;
import vest.doctor.aop.MethodInvocation;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class MapModifyingAspect implements Aspect {

    @Override
    @SuppressWarnings("unchecked")
    public Object execute(MethodInvocation methodInvocation) {
        if (methodInvocation.arity() > 0) {
            ArgValue arg0 = methodInvocation.getArgumentValue(0);
            if (arg0.type().getRawType() == Map.class) {
                Map<String, Object> map = arg0.get();
                Map<String, Object> replaced = new HashMap<>(map);
                replaced.put("_pre", true);
                replaced.put("_paramName", arg0.name());
                arg0.set(replaced);
            }
        }
        Object result = methodInvocation.next();
        if (result instanceof Map map) {
            map.put("_modified", true);
            map.put("_arity", methodInvocation.arity());
            map.put("_methodName", methodInvocation.getMethodName());
            return map;
        } else {
            return result;
        }
    }
}
