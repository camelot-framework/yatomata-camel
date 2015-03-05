package ru.yandex.qatools.fsm.camel.annotations;

import org.apache.camel.processor.aggregate.TimeoutAwareAggregationStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Indicates that Camel will invoke this timeout method when the timeout occurs.
 * The method should have the following parameters as described in interface method
 * {@link TimeoutAwareAggregationStrategy#timeout} or otherwise an exception would be thrown:
 * <pre>{@code YourState state, int index, int total, long timeout}</pre>
 * If the real current state of the fsm can not be cast to the state parameter type
 * (which is YourState in the example) then an exception will be thrown.
 *
 * @see TimeoutAwareAggregationStrategy
 * @author Innokenty Shuvalov (mailto: innokenty@yandex-team.ru)
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OnTimeout {
}
