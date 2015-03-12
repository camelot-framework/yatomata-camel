package ru.yandex.qatools.fsm.camel;

import ru.yandex.qatools.fsm.camel.annotations.FallbackProcessor;
import ru.yandex.qatools.fsm.camel.annotations.Processor;
import ru.yandex.qatools.fsm.camel.common.CallException;
import ru.yandex.qatools.fsm.camel.common.DispatchException;
import ru.yandex.qatools.fsm.camel.common.MetadataException;
import ru.yandex.qatools.fsm.camel.util.ReflectionUtil.AnnotatedMethodHandler;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static ru.yandex.qatools.fsm.camel.util.ReflectionUtil.forEachAnnotatedMethod;

/**
 * Dispatcher that allows to dispatch messages to the plugins
 *
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 */
public class PluggableProcessorDispatcher {

    private ProcMethodInfo fallBackProcessor = null;
    final private Object processor;
    final private Class procClass;
    private final Map<Class, ProcMethodInfo> procCallers = new HashMap<>();

    public PluggableProcessorDispatcher(Object processor, Class procClass) {
        this.processor = processor;
        this.procClass = procClass;
        scanPluginProcessors();
    }

    public PluggableProcessorDispatcher(Object processor) {
        this(processor, processor.getClass());
    }

    /**
     * Dispatches the message to the processor and returns the result
     * of the processor's method invocation
     *
     * @param body    body of the received exchange
     * @param headers headers of the received exchange
     * @return result of the processor's method invocation
     * @throws ru.yandex.qatools.fsm.camel.common.DispatchException
     *          when invocation cannot be processed.
     */
    public Object dispatch(Object body, Map<String, Object> headers) throws DispatchException {
        if (body == null) {
            throw new DispatchException("Unable to dispatch the plugin " + processor + " with null body!");
        }

        ProcMethodInfo info = findProcMethodByClass(body.getClass());
        if (info == null) {
            if (fallBackProcessor == null) {
                throw new DispatchException(String.format(
                        "Unable to dispatch the plugin %s with body of type %s: processor not found!",
                        processor, body.getClass()));
            } else {
                if (!fallBackProcessor.bodyType.isAssignableFrom(body.getClass())) {
                    throw new DispatchException(String.format(
                            "Unable to dispatch the plugin %s with body of type %s. %s is not assignable from %s!",
                            processor, body.getClass(), fallBackProcessor.bodyType, body.getClass()));
                }
                info = fallBackProcessor;
            }
        }

        try {
            return info.caller.call(body, headers);
        } catch (CallException e) {
            throw new DispatchException(String.format(
                    "Could not call the method of plugin %s because of exception: ", processor
            ), e);
        }
    }

    /**
     * Search through all the processors to match the incoming body with the proc config
     */
    private ProcMethodInfo findProcMethodByClass(final Class objClazz) {
        Class clazz = objClazz;
        // search through superclasses
        while (clazz != null) {
            if (procCallers.containsKey(clazz)) {
                return procCallers.get(clazz);
            }
            // search through all interfaces
            for (Class iface : clazz.getInterfaces()) {
                if (procCallers.containsKey(iface)) {
                    return procCallers.get(iface);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    /**
     * Inner struct for method info
     */
    private static class ProcMethodInfo {
        CamelMethodInvoker caller;
        Class bodyType;

        private ProcMethodInfo(Class bodyType, CamelMethodInvoker caller) {
            this.bodyType = bodyType;
            this.caller = caller;
        }
    }

    /**
     * Perform the processors scan
     */
    private synchronized void scanPluginProcessors() throws MetadataException {
        forEachAnnotatedMethod(procClass, Processor.class, new AnnotatedMethodHandler<Processor>() {
            @Override
            public void handle(Method method, Processor proc) {
                ProcMethodInfo mInfo = getProcMethodInfo(method, proc.bodyType());
                if (procCallers.containsKey(proc.bodyType())) {
                    throw new MetadataException(String.format(
                            "Plugin %s must not contain more than 1 processor for the body type '%s'!",
                            procClass, proc.bodyType()));
                }
                procCallers.put(proc.bodyType(), mInfo);
            }
        });

        forEachAnnotatedMethod(procClass, FallbackProcessor.class, new AnnotatedMethodHandler<FallbackProcessor>() {
            boolean foundFallBackProcessor;
            @Override
            public void handle(Method method, FallbackProcessor annotation) {
                if (foundFallBackProcessor) {
                    throw new MetadataException(String.format(
                            "Plugin %s must not contain more than 1 fallback processor!",
                            procClass));
                }
                foundFallBackProcessor = true;
                fallBackProcessor = getProcMethodInfo(method, annotation.baseType());
            }
        });
    }

    private synchronized ProcMethodInfo getProcMethodInfo(Method method, Class bodyType) {
        ProcMethodInfo mInfo = new ProcMethodInfo(bodyType, new CamelMethodInvoker(processor, method));
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length < 1) {
            throw new MetadataException("Processor method must have at least one argument!");
        }
        return mInfo;
    }
}
