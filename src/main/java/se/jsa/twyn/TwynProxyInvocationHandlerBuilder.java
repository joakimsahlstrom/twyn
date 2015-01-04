package se.jsa.twyn;

import java.lang.reflect.Proxy;

import org.codehaus.jackson.JsonNode;

public class TwynProxyInvocationHandlerBuilder implements TwynProxyBuilder {

	@Override
	public <T> T buildProxy(Class<T> type, Twyn twyn, JsonNode jsonNode) throws Exception {
		return type.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), 
				new Class<?>[] { type },
				new TwynInvocationHandler(jsonNode, twyn)));
	}

}
