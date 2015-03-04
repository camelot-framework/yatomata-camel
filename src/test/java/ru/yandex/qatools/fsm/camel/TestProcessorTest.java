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

@RunWith(CamelSpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
@DirtiesContext
@MockEndpoints("*")
public class TestProcessorTest {

    @EndpointInject(uri = "mock:seda:queue:testprocdone")
    protected MockEndpoint endpoint;

    @EndpointInject(uri = "mock:direct:events.stop")
    protected MockEndpoint endpointStop;

    @Produce(uri = "seda:queue:testproc")
    protected ProducerTemplate execute;

    @Before
    public void setUp() throws Exception {
        endpoint.reset();
    }

    @Test
    public void testFSM() throws Exception {
        endpoint.expectedMessageCount(1);
        endpoint.expectedBodyReceived().body().isInstanceOf(String.class);
        endpoint.expectedBodiesReceived("testprocessed");
        endpointStop.expectedMinimumMessageCount(1);
        execute.sendBody("test");
        endpoint.assertIsSatisfied(2000);
        endpointStop.assertIsSatisfied(2000);
    }
}
