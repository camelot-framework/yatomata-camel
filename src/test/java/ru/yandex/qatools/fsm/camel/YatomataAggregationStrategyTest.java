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

    private static final String HEADER_VALUE = "Hello:)";

    @EndpointInject(uri = "mock:seda:queue:done")
    protected MockEndpoint endpointDone;

    @EndpointInject(uri = "mock:direct:events.stop")
    protected MockEndpoint endpointStop;

    @Produce(uri = "seda:queue:test")
    protected ProducerTemplate testAggregator;

    @Produce(uri = "seda:queue:test-timeout")
    protected ProducerTemplate testAggregatorWithTimeout;

    @Before
    public void setUp() throws Exception {
        endpointDone.reset();
        endpointStop.reset();
    }

    @Test
    public void testFsm() throws Exception {
        endpointDone.expectedMessageCount(1);
        endpointDone.expectedBodiesReceived(HEADER_VALUE);

        endpointStop.expectedMessageCount(2);
        endpointStop.message(0).body().isEqualTo(HEADER_VALUE);
        endpointStop.message(1).body().isInstanceOf(FinishedState.class);
        endpointStop.message(1).header(HEADER_KEY).isEqualTo(HEADER_VALUE);

        testAggregator.sendBody(new TStartProgress("test"));
        testAggregator.sendBodyAndHeader(new TFinishProgress("test"), HEADER_KEY, HEADER_VALUE);

        endpointDone.assertIsSatisfied(2000);
        endpointStop.assertIsSatisfied(2000);
    }

    @Test
    public void testFsmWithTimeout() throws Exception {
        endpointDone.expectedMessageCount(1);
        endpointDone.expectedBodyReceived().body().isInstanceOf(ProgressState.class);

        endpointStop.expectedMessageCount(2);
        endpointStop.message(0).body().isEqualTo(HEADER_VALUE);
        endpointStop.message(0).header("ontimeout.params.state").isInstanceOf(ProgressState.class);
        endpointStop.message(0).header("ontimeout.params.index").isEqualTo(-1);
        endpointStop.message(0).header("ontimeout.params.total").isEqualTo(-1);
        endpointStop.message(0).header("ontimeout.params.timeout").isEqualTo(500);
        endpointStop.message(1).body().isInstanceOf(ProgressState.class);
        endpointStop.message(1).header(HEADER_KEY).isEqualTo(HEADER_VALUE);

        testAggregatorWithTimeout.sendBodyAndHeader(new TStartProgress("test"), HEADER_KEY, HEADER_VALUE);

        endpointDone.assertIsSatisfied(2000);
        endpointStop.assertIsSatisfied(2000);
    }
}
