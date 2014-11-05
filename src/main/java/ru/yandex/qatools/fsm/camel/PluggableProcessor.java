package ru.yandex.qatools.fsm.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 */
public class PluggableProcessor extends BasicStrategy implements Processor {

    final protected Logger logger = LoggerFactory.getLogger(getClass());
    final private PluggableProcessorDispatcher dispatcher;
    final private Object processor;

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
        } catch (Exception e) {
            logger.error(processor + ": " + e.getMessage(), e);
        }
        message.getIn().setBody(result);
    }
}
