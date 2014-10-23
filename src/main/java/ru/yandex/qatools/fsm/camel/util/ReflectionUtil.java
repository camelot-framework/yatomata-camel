package ru.yandex.qatools.fsm.camel.util;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

public class ReflectionUtil {


    /**
     * Searches for all fields within class hierarchy
     *
     * @return
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
    public static Object getAnnotationValue(Object aObj, String aValue) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return aObj.getClass().getMethod(aValue).invoke(aObj);
    }

    /**
     * Get annotation value of annotation object via reflection
     */
    public static Object getAnnotationValue(AnnotatedElement aobj, Class aClass, String aValue) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return getAnnotationValue(getAnnotation(aobj, aClass), aValue);
    }

    /**
     * Get annotation of an object via reflection
     */
    public static Object getAnnotation(AnnotatedElement aobj, Class aClass) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        for (Object a : aobj.getAnnotations()) {
            if (isAnnotationInstance(aClass, a)) return a;
        }
        return null;
    }

    /**
     * Get annotation within hierarchy
     */
    public static <A extends Annotation> Object getAnnotationWithinHierarchy(Class<?> fsmClass, Class<A> aggregateClass) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        while (fsmClass != null) {
            if (getAnnotation(fsmClass, aggregateClass) != null) {
                return getAnnotation(fsmClass, aggregateClass);
            }
            fsmClass = fsmClass.getSuperclass();
        }
        return null;
    }

    private static boolean isAnnotationInstance(Class aClass, Object a) {
        if (Proxy.isProxyClass(a.getClass())) {
            for (Class aInterface : a.getClass().getInterfaces()) {
                if (aInterface.getName().equals(aClass.getName())) {
                    return true;
                }
            }
        }
        return aClass.isInstance(a);
    }

}