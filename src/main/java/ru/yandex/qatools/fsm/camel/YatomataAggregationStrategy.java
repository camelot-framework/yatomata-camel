package ru.yandex.qatools.fsm.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.Headers;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.qatools.fsm.Yatomata;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import static java.lang.String.format;
import static ru.yandex.qatools.fsm.camel.util.ReflectionUtil.*;

public class YatomataAggregationStrategy<T> implements AggregationStrategy {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final YatomataCamelFSMBuilder<T> fsmEngineBuilder;
    private final Class<T> fsmClass;
    public static final String FINISHED_EXCHANGE = "YatomataFinishedExchange";

    public YatomataAggregationStrategy(Class<T> fsmClass) {
        this.fsmClass = fsmClass;
        this.fsmEngineBuilder = new YatomataCamelFSMBuilder<>(fsmClass);
    }

    @Override
    public Exchange aggregate(Exchange state, Exchange event) {
        Object result = state == null ? null : state.getIn().getBody();
        try {
            T fsm = fsmClass.newInstance();
            injectFields(fsm, event);

            Yatomata<T> fsmEngine;
            if (result != null) {
                fsmEngine = fsmEngineBuilder.build(result, fsm);
            } else {
                fsmEngine = fsmEngineBuilder.build(fsm);
            }
            result = fsmEngine.fire(event.getIn().getBody());
            event.getIn().setHeader(FINISHED_EXCHANGE, fsmEngine.isCompleted());
        } catch (Exception e) {
            logger.error(format("Failed to process event %s with FSM %s!", event.getIn().getBody(), fsmClass), e);
        }
        if (result != null) {
            event.getIn().setBody(result);
        }
        return event;
    }

    public boolean isCompleted(Exchange exchange)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return exchange == null
                || exchange.getIn() == null
                || (boolean) exchange.getIn().removeHeader(FINISHED_EXCHANGE);
    }

    protected void injectFields(Object procInstance, Exchange exchange) {
        for (Field field : getFieldsInClassHierarchy(procInstance.getClass())) {
            try {
                boolean oldAccessible = field.isAccessible();
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                if (getAnnotation(field, Header.class) != null) {
                    String headerName = (String) getAnnotationValue(field, Header.class, "value");
                    field.set(procInstance, exchange.getIn().getHeader(headerName));
                }
                if (getAnnotation(field, Headers.class) != null) {
                    field.set(procInstance, exchange.getIn().getHeaders());
                }
                field.setAccessible(oldAccessible);
            } catch (Exception e) {
                logger.error("Inject field " + field.getName() + " of FSM " + procInstance + " error: ", e);
            }
        }
    }
}
