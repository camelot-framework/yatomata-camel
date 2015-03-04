package ru.yandex.qatools.fsm.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelBeanPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import ru.yandex.qatools.fsm.camel.annotations.InjectHeader;
import ru.yandex.qatools.fsm.camel.annotations.InjectHeaders;

import java.lang.reflect.Field;

import static ru.yandex.qatools.fsm.camel.util.ReflectionUtil.getAnnotationValue;
import static ru.yandex.qatools.fsm.camel.util.ReflectionUtil.getFieldsInClassHierarchy;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 */
public abstract class BasicStrategy implements CamelContextAware, ApplicationContextAware {

    private static final AutowiredAnnotationBeanPostProcessor BEAN_PROCESSOR
            = new AutowiredAnnotationBeanPostProcessor();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private CamelContext camelContext;
    private ApplicationContext applicationContext;

    protected void injectFields(Object procInstance, Exchange exchange) {
        try {
            DefaultCamelBeanPostProcessor processor = new DefaultCamelBeanPostProcessor(camelContext);
            processor.postProcessBeforeInitialization(procInstance, null);
            if (procInstance instanceof CamelContextAware) {
                ((CamelContextAware) procInstance).setCamelContext(camelContext);
            }
        } catch (Exception e) {
            logger.error("Could not autowire Camel context fields: ", e);
        }
        try {
            if (applicationContext != null) {
                BEAN_PROCESSOR.setBeanFactory(applicationContext.getAutowireCapableBeanFactory());
                BEAN_PROCESSOR.processInjection(procInstance);
                if (procInstance instanceof ApplicationContextAware) {
                    ((ApplicationContextAware) procInstance).setApplicationContext(applicationContext);
                }
            }
        } catch (Exception e) {
            logger.error("Could not autowire Camel context fields: ", e);
        }
        for (Field field : getFieldsInClassHierarchy(procInstance.getClass())) {
            try {
                boolean oldAccessible = field.isAccessible();
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                if (field.isAnnotationPresent(InjectHeader.class)) {
                    String headerName = (String) getAnnotationValue(field, InjectHeader.class, "value");
                    field.set(procInstance, exchange.getIn().getHeader(headerName));
                }
                if (field.isAnnotationPresent(InjectHeaders.class)) {
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @SuppressWarnings("UnusedDeclaration")
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
