package ru.yandex.qatools.fsm.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelBeanPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.qatools.fsm.camel.annotations.InjectHeader;
import ru.yandex.qatools.fsm.camel.annotations.InjectHeaders;

import java.lang.reflect.Field;

import static ru.yandex.qatools.fsm.camel.util.ReflectionUtil.getAnnotation;
import static ru.yandex.qatools.fsm.camel.util.ReflectionUtil.getAnnotationValue;
import static ru.yandex.qatools.fsm.camel.util.ReflectionUtil.getFieldsInClassHierarchy;

public abstract class BasicStrategy implements CamelContextAware{
    protected static final Logger logger = LoggerFactory.getLogger(YatomataAggregationStrategy.class);
    protected CamelContext camelContext;

    protected void injectFields(Object procInstance, Exchange exchange) {
        try {
            DefaultCamelBeanPostProcessor processor = new DefaultCamelBeanPostProcessor(camelContext);
            processor.postProcessBeforeInitialization(procInstance, null);
            if (procInstance instanceof CamelContextAware) {
                ((CamelContextAware) procInstance).setCamelContext(camelContext);
            }
        } catch (Exception e) {
            logger.error("Could not autowire the Spring or Camel context fields: ", e);
        }
        for (Field field : getFieldsInClassHierarchy(procInstance.getClass())) {
            try {
                boolean oldAccessible = field.isAccessible();
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                if (getAnnotation(field, InjectHeader.class) != null) {
                    String headerName = (String) getAnnotationValue(field, InjectHeader.class, "value");
                    field.set(procInstance, exchange.getIn().getHeader(headerName));
                }
                if (getAnnotation(field, InjectHeaders.class) != null) {
                    field.set(procInstance, exchange.getIn().getHeaders());
                }
                field.setAccessible(oldAccessible);
            } catch (Exception e) {
                logger.error("Inject field " + field.getName() + " of FSM " + procInstance + " error: ", e);
            }
        }
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

}
