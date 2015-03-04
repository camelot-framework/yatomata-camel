package ru.yandex.qatools.fsm.camel;


import org.apache.camel.Body;
import org.apache.camel.Header;
import org.apache.camel.Headers;
import ru.yandex.qatools.fsm.camel.common.CallException;
import ru.yandex.qatools.fsm.camel.common.MetadataException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ru.yandex.qatools.fsm.camel.util.ReflectionUtil.getAnnotationValue;
import static ru.yandex.qatools.fsm.utils.ReflectUtils.getMethodsInClassHierarchy;


/**
 * This is a custom implementation of bean method scanning. It is required
 * just to be able to check parameters annotations by name, and not by class instance
 * See the original code in BeanInfo#getMethodInfo from Camel sources
 *
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 * @author Innokenty Shuvalov (mailto: innokenty@yandex-team.ru)
 */
public class CamelMethodInvoker {

    private ProcMethodInfo info;
    private final Object instance;

    public CamelMethodInvoker(Object instance, Method method) {
        this.info = getMethodInfo(method);
        this.instance = instance;
    }

    public CamelMethodInvoker(Object instance, Class clazz, String methodName) throws Exception {
        if (instance == null) {
            throw new CallException("Instance cannot not be null!");
        }
        this.instance = instance;
        for (Method method : getMethodsInClassHierarchy(clazz)) {
            if (method.getName().equals(methodName)) {
                this.info = getMethodInfo(method);
                break;
            }
        }
    }

    public CamelMethodInvoker(Object instance, String methodName) throws Exception {
        this(instance, instance.getClass(), methodName);
    }

    public Object call(@Body Object body, @Headers Map<String, Object> headers) throws CallException {
        return invokeMethod(instance, body, headers);
    }

    /**
     * Inner struct for method parameters info
     */
    private static class ProcParamInfo {
        static enum Kind {
            BODY, HEADER, HEADERS
        }

        final Kind kind;
        final Type type;
        final String name;

        private ProcParamInfo(Type type, Kind kind, String name) {
            this.type = type;
            this.kind = kind;
            this.name = name;
        }

        public ProcParamInfo(Type type, Kind kind) {
            this.type = type;
            this.kind = kind;
            this.name = null;
        }
    }

    /**
     * Inner struct for method info
     */
    private static class ProcMethodInfo {
        Method method;
        List<ProcParamInfo> parameters = new ArrayList<>();

        private ProcMethodInfo(Method method) {
            this.method = method;
        }
    }

    private Object invokeMethod(Object instance, Object body, Map<String, Object> headers) throws CallException {
        boolean oldAccessible = info.method.isAccessible();
        info.method.setAccessible(true);
        List<Object> arguments = new ArrayList<>();
        // check the type of each param
        for (ProcParamInfo param : info.parameters) {
            switch (param.kind) {
                case BODY:
                    arguments.add(body);
                    break;
                case HEADERS:
                    arguments.add(headers);
                    break;
                case HEADER:
                    arguments.add(headers.get(param.name));
                    break;
            }
        }
        try {
            return info.method.invoke(instance, arguments.toArray());
        } catch (Exception e) {
            throw new CallException(e);
        } finally {
            info.method.setAccessible(oldAccessible);
        }
    }

    private synchronized ProcMethodInfo getMethodInfo(Method method) {
        ProcMethodInfo mInfo = new ProcMethodInfo(method);
        Class<?>[] parameterTypes = method.getParameterTypes();
        mInfo.parameters = getParameters(method, parameterTypes);
        return mInfo;
    }


    public List<ProcParamInfo> getParameters(Method method, Class<?>[] parameterTypes) {
        Annotation[][] pAnnotations = method.getParameterAnnotations();
        List<ProcParamInfo> parameters = new ArrayList<>(pAnnotations.length);
        for (int i = 0; i < parameterTypes.length; ++i) {
            ProcParamInfo paramInfo = null;
            if (pAnnotations[i].length > 0) {
                for (Annotation pAnnotation : pAnnotations[i]) {
                    if (paramInfo == null) {
                        paramInfo = getProcParamInfo(method, pAnnotation, parameterTypes[i]);
                    }
                }
            }
            if (paramInfo == null) {
                throw new MetadataException(String.format(
                        "Cannot recognize method's '%s' %d parameter with type %s!",
                        method.getName(), i, parameterTypes[i]));
            }
            parameters.add(paramInfo);
        }
        return parameters;
    }

    private ProcParamInfo getProcParamInfo(Method method, Annotation paramAnnotation, Class<?> paramType) {
        if (hasAnnotation(paramAnnotation, Headers.class)
                || hasAnnotation(paramAnnotation, org.apache.camel.Headers.class)) {
            if (!paramType.isAssignableFrom(Map.class)) {
                throw new MetadataException(String.format(
                        "Headers argument of type %s of processor %s must be assignable from the body type %s !",
                        paramType, method.getName(), Map.class));
            }
            return new ProcParamInfo(paramType, ProcParamInfo.Kind.HEADERS);

        } else if (hasAnnotation(paramAnnotation, Header.class)
                || hasAnnotation(paramAnnotation, org.apache.camel.Header.class)) {
            final String name;
            try {
                name = (String) getAnnotationValue(paramAnnotation, "value");
            } catch (Exception e) {
                throw new MetadataException(String.format("Cannot get the value of %s of method %s",
                        paramAnnotation.annotationType().getName(), method.getName()));
            }
            return new ProcParamInfo(paramType, ProcParamInfo.Kind.HEADER, name);

        } else if (hasAnnotation(paramAnnotation, Body.class)
                || hasAnnotation(paramAnnotation, org.apache.camel.Body.class)) {
            return new ProcParamInfo(paramType, ProcParamInfo.Kind.BODY);
        }
        return null;
    }

    private boolean hasAnnotation(Annotation paramAnnotation, Class<? extends Annotation> annotationClass) {
        return paramAnnotation.annotationType().equals(annotationClass);
    }

}
