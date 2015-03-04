package ru.yandex.qatools.fsm.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import ru.yandex.qatools.fsm.annotations.*;
import ru.yandex.qatools.fsm.camel.annotations.InjectHeader;
import ru.yandex.qatools.fsm.camel.annotations.OnTimer;

import java.io.Serializable;
import java.util.HashMap;

@FSM(start = TestStateMachine.UndefinedState.class)
@Transitions({
        @Transit(from = TestStateMachine.UndefinedState.class, to = TestStateMachine.ProgressState.class, on = TestStateMachine.TStartProgress.class),
        @Transit(from = TestStateMachine.ProgressState.class, to = TestStateMachine.FinishedState.class, on = TestStateMachine.TFinishProgress.class, stop = true),
})
public class TestStateMachine implements CamelContextAware {

    public static final String HEADER_KEY = "uuid";

    @Produce(uri = "seda:queue:done")
    private ProducerTemplate doneQueue;

    @InjectHeader(HEADER_KEY)
    String uuid;

    private CamelContext camelContext;

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    public static abstract class TState implements Serializable {
        TEvent event;
    }

    public static class UndefinedState extends TState {

    }

    public static class ProgressState extends TState {

    }

    public static class FinishedState extends TState {

    }

    public static abstract class TEvent implements Serializable {
        String uuid;

        protected TEvent(String uuid) {
            this.uuid = uuid;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class TStartProgress extends TEvent {
        public TStartProgress(String uuid) {
            super(uuid);
        }
    }

    public static class TFinishProgress extends TEvent {
        public TFinishProgress(String uuid) {
            super(uuid);
        }
    }

    @NewState
    public TState initState(Class<? extends TState> stateClass, TEvent event) throws Exception {
        TState res = stateClass.newInstance();
        res.event = event;
        return res;
    }

    @OnTransit
    public void entryFinishedState(FinishedState newState, TFinishProgress event) {
        if (uuid == null) {
            throw new RuntimeException("UUID must not be null!");
        }
        if (camelContext == null) {
            throw new RuntimeException("CamelContext must not be null!");
        }
        doneQueue.sendBody(uuid);
    }

    @OnTimer
    public void onTimer(final int index, final int total, final long timeout) {
        if (uuid == null) {
            throw new RuntimeException("UUID must not be null!");
        }
        if (camelContext == null) {
            throw new RuntimeException("CamelContext must not be null!");
        }
        doneQueue.sendBodyAndHeaders(uuid, new HashMap<String, Object>() {{
            put("index", index);
            put("total", total);
            put("timeout", timeout);
        }});
    }
}
