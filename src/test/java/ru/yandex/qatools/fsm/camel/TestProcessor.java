package ru.yandex.qatools.fsm.camel;

import org.apache.camel.Body;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import ru.yandex.qatools.fsm.camel.annotations.Processor;

public class TestProcessor implements CamelContextAware {
    private CamelContext camelContext;


    @Processor(bodyType = String.class)
    public String process(@Body String body) {
        return body + "processed";
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
