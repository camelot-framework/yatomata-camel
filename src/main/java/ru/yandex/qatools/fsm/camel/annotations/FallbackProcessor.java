package ru.yandex.qatools.fsm.camel.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a method of a plugin will be used when all other processor methods did not match the body's type.
 * Fallback processor typically should receive the <code>Object</code> type.
 * @author: Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FallbackProcessor {
    public Class baseType() default Object.class;
}
