package se.jsa.twyn.internal;

import java.lang.reflect.Proxy;

import org.codehaus.jackson.JsonNode;

public class TwynProxyInvocationHandlerBuilder implements TwynProxyBuilder {

	@Override
	public <T> T buildProxy(Class<T> type, TwynContext twynContext, JsonNode jsonNode) throws Exception {
		return type.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), 
				new Class<?>[] { type },
				new TwynProxyInvocationHandler(jsonNode, twynContext)));
	}

	@Override
	public String toString() {
		return "TwynProxyInvocationHandlerBuilder []";
	}
	
}
