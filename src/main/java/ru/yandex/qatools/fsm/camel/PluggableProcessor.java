package ru.yandex.qatools.fsm.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.qatools.fsm.camel.common.DispatchException;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 */
public class PluggableProcessor extends BasicStrategy implements Processor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final PluggableProcessorDispatcher dispatcher;
    private final Object processor;

    public PluggableProcessor(Object processor, Class procClass) {
        this.processor = processor;
        dispatcher = new PluggableProcessorDispatcher(processor, procClass);
    }

    public PluggableProcessor(Object processor) {
        this.processor = processor;
        dispatcher = new PluggableProcessorDispatcher(processor);
    }

    @Override
    public void process(Exchange message) {
        Object result = null;
        try {
            Object event = message.getIn().getBody();
            injectFields(processor, message);
            if (event != null) {
                result = dispatcher.dispatch(event, message.getIn().getHeaders());
            }
        } catch (DispatchException e) {
            logger.error(e.getMessage(), e.getCause());
        } catch (Exception e) {
            logger.error(processor + ": " + e.getMessage(), e);
        }
        message.getIn().setBody(result);
    }
}
