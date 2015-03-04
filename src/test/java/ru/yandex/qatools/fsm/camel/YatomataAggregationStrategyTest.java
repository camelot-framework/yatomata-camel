package ru.yandex.qatools.fsm.camel;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringJUnit4ClassRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import static ru.yandex.qatools.fsm.camel.TestStateMachine.*;

@RunWith(CamelSpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
@DirtiesContext
@MockEndpoints("*")
public class YatomataAggregationStrategyTest {

    @EndpointInject(uri = "mock:seda:queue:done")
    protected MockEndpoint endpoint;

    @EndpointInject(uri = "mock:direct:events.stop")
    protected MockEndpoint endpointStop;

    @Produce(uri = "seda:queue:test")
    protected ProducerTemplate execute;

    @Before
    public void setUp() throws Exception {
        endpoint.reset();
    }

    @Test
    public void testFSM() throws Exception {
        endpoint.expectedMessageCount(1);
        endpoint.expectedBodyReceived().body().isInstanceOf(FinishedState.class);
        endpointStop.expectedMinimumMessageCount(2);
        execute.sendBody(new TStartProgress("test"));
        execute.sendBodyAndHeader(new TFinishProgress("test"), "uuid", "Hello:)");
        endpoint.assertIsSatisfied(2000);
        endpointStop.assertIsSatisfied(2000);
    }
}
