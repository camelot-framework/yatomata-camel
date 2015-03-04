package ru.yandex.qatools.fsm.camel.util;

import org.apache.commons.lang3.ArrayUtils;
import ru.yandex.qatools.fsm.camel.common.MetadataException;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 * @author Innokenty Shuvalov (mailto: innokenty@yandex-team.ru)
 */
public class ReflectionUtil {

    /**
     * Searches for all fields within class hierarchy
     */
    public static Field[] getFieldsInClassHierarchy(Class<?> clazz) {
        Field[] fields = {};
        while (clazz != null) {
            fields = ArrayUtils.addAll(fields, clazz.getDeclaredFields());
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * Get annotation value of annotation object via reflection
     */
    public static Object getAnnotationValue(Object aObj, String aValue)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return aObj.getClass().getMethod(aValue).invoke(aObj);
    }

    /**
     * Get annotation value of annotation object via reflection
     */
    public static Object getAnnotationValue(AnnotatedElement aobj, Class aClass, String aValue)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return getAnnotationValue(aobj.getAnnotation(aClass), aValue);
    }

    /**
     * Get annotation within hierarchy
     */
    public static <A extends Annotation> A getAnnotationWithinHierarchy(Class<?> fsmClass, Class<A> aggregateClass)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        while (fsmClass != null) {
            if (fsmClass.getAnnotation(aggregateClass) != null) {
                return fsmClass.getAnnotation(aggregateClass);
            }
            fsmClass = fsmClass.getSuperclass();
        }
        return null;
    }

    public static <T extends Annotation> Map<Method, T> getAnnotatedMethods(Class aClass, Class<T> annotationClass) {
        HashMap<Method, T> methods = new HashMap<>();
        for (Method method : aClass.getMethods()) {
            try {
                T proc = method.getAnnotation(annotationClass);
                if (proc != null) {
                    methods.put(method, proc);
                }
            } catch (Exception e) {
                throw new MetadataException(String.format(
                        "Failed to read annotation of method %s of class %s",
                        method.getName(), aClass), e);
            }
        }
        return methods;
    }

    public static <T extends Annotation> void forEachAnnotatedMethod(
            Class aClass, Class<T> annotationClass, AnnotatedMethodHandler<T> handler) {
        for (Map.Entry<Method, T> entry : getAnnotatedMethods(aClass, annotationClass).entrySet()) {
            handler.handle(entry.getKey(), entry.getValue());
        }
    }

    /**
     * @author innokenty
     */
    public static interface AnnotatedMethodHandler<T extends Annotation> {
        void handle(Method method, T annotation);
    }
}
