package ru.yandex.qatools.fsm.camel;

import org.apache.camel.Body;
import org.apache.camel.Header;
import org.apache.camel.Headers;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CamelMethodInvokerTest {

    interface TestCamelClass {
        public Object testNoArgs();

        public Object testBodyArg(@Body Object body);

        public Object testHeadersArg(@Headers Map<String, Object> headers);

        public Object testBodyHeadersArg(@org.apache.camel.Body Object body, @Headers Map<String, Object> headers);

        public Object testHeaderArg(@Header("test") String header);

        public Object testBodyHeaderArg(@Body Object body, @Header("test") String header);

        public Object testBodyHeadersHeaderArg(@Body Object body, @Headers Map<String, Object> headers, @Header("test") String header);
    }

    private TestCamelClass mck;
    private Map<String, Object> headers;
    private Object body;
    private String header = "customHeaderValue";

    @Before
    public void init() {
        mck = mock(TestCamelClass.class);
        body = new Object();
        headers = new HashMap<>();
        headers.put("test", header);
    }

    @Test
    public void testNoArgs() throws Exception {
        caller("testNoArgs").call(body, headers);
        verify(mck).testNoArgs();
    }

    @Test
    public void testHeadersArg() throws Exception {
        caller("testHeadersArg").call(body, headers);
        verify(mck).testHeadersArg(headers);
    }

    @Test
    public void testBodyArg() throws Exception {
        caller("testBodyArg").call(body, headers);
        verify(mck).testBodyArg(body);
    }

    @Test
    public void testBodyHeadersArg() throws Exception {
        caller("testBodyHeadersArg").call(body, headers);
        verify(mck).testBodyHeadersArg(body, headers);
    }

    @Test
    public void testHeaderArg() throws Exception {
        caller("testHeaderArg").call(body, headers);
        verify(mck).testHeaderArg(header);
    }

    @Test
    public void testBodyHeaderArg() throws Exception {
        caller("testBodyHeaderArg").call(body, headers);
        verify(mck).testBodyHeaderArg(body, header);
    }

    @Test
    public void testBodyHeadersHeaderArg() throws Exception {
        caller("testBodyHeadersHeaderArg").call(body, headers);
        verify(mck).testBodyHeadersHeaderArg(body, headers, header);
    }

    private CamelMethodInvoker caller(String method) throws Exception {
        return new CamelMethodInvoker(mck, TestCamelClass.class, method);
    }
}
