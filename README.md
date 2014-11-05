Yatomata Camel integration
=============

This project aims to provide the ability to use [Yatomata](https://github.com/camelot-framework/yatomata) along with
[Camel](https://github.com/apache/camel).

## User Guide

### Setup

Add the following dependency to pom.xml of your Camel project:

```xml
    <dependency>
        <groupId>ru.yandex.qatools</groupId>
        <artifactId>yatomata-camel</artifactId>
        <version>1.0</version>
    </dependency>
```

### Basics

Create the FSM class (for more information see [Yatomata](https://github.com/camelot-framework/yatomata) docs):
```java
    @FSM(start = Stopped.class)
    @Transitions({
            @Transit(from = Stopped.class, on = Run.class, to = Running.class),
            @Transit(from = Running.class, on = Stop.class, to = Stopped.class, stop = true),
    })
    public class MyFSM {

    }
```

Add the aggregator bean to your Spring context:
```xml
    <bean id="myFSM" class="ru.yandex.qatools.fsm.camel.YatomataAggregationStrategy">
        <constructor-arg value="com.me.MyFSM"/>
    </bean>
```

Now you can use `myFSM` as an ordinary aggregation strategy:
```xml
    <aggregate strategyRef="myFSM">
        <correlationExpression>
            <simple>${in.body.uuid}</simple>
        </correlationExpression>
        <completionPredicate>
            <method bean="myFSM" method="isCompleted"/>
        </completionPredicate>
        <to uri="seda:queue:done"/>
    </aggregate>
```

You can also use the more declarative processors with the `@Processor` annotation. 
Create the processor class, declaring the separate methods for every body type of your message:
```java
public class MyProcessor {
    @Processor(bodyType = String.class)
    public String process(@Body String body) {
        return body + "processed";
    }
}
```

Add the processor bean to your Spring context:
```xml
    <bean id="myProcessor" class="ru.yandex.qatools.fsm.camel.PluggableProcessor">
        <constructor-arg>
            <bean class="com.me.MyProcessor"/>
        </constructor-arg>
    </bean>
```

Now you can use `myProcessor` as an ordinary processor in your camel routes:
```xml
    <process ref="myProcessor"/>
```

### Camel context injection

You can use `@InjectHeader` and `@InjectHeaders` annotations to inject the current exchange context into your FSM:

```java
    @InjectHeader("headerName")
    String headerValue;

    @InjectHeaders
    Map<String, Object> headers;

```

You can also inject the Camel producer templates using `@Producer` annotation like in the ordinary Camel beans:

```java
    @Produce(uri = "seda:queue:done")
    private ProducerTemplate doneQueue;
```

It is also possible to inject the CamelContext using the CamelContextAware interface:

```java
   @FSM(start = InitialState.class)
   @Transitions
   public class TestStateMachine implements CamelContextAware {
       private CamelContext camelContext;

       @Override
       public void setCamelContext(CamelContext camelContext) {
           this.camelContext = camelContext;
       }

       @Override
       public CamelContext getCamelContext() {
           return camelContext;
       }
   }
```
