package ru.yandex.qatools.fsm.camel;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.qatools.fsm.Yatomata;

import java.lang.reflect.InvocationTargetException;

import static java.lang.String.format;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 */
public class YatomataAggregationStrategy<T> extends BasicStrategy implements AggregationStrategy{

    public static final String FINISHED_EXCHANGE = "YatomataFinishedExchange";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final YatomataCamelFSMBuilder<T> fsmEngineBuilder;
    private final Class<T> fsmClass;

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
            logger.error(format("Failed to process event %s with FSM %s!",
                    event.getIn().getBody(), fsmClass), e);
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
}
