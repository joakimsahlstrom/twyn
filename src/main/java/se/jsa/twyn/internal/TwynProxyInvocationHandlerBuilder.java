package se.jsa.twyn.internal;

import java.lang.reflect.Proxy;

import org.codehaus.jackson.JsonNode;

import se.jsa.twyn.Twyn;
import se.jsa.twyn.TwynProxyBuilder;

public class TwynProxyInvocationHandlerBuilder implements TwynProxyBuilder {

	@Override
	public <T> T buildProxy(Class<T> type, Twyn twyn, JsonNode jsonNode) throws Exception {
		return type.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), 
				new Class<?>[] { type },
				new TwynProxyInvocationHandler(jsonNode, twyn)));
	}

}
