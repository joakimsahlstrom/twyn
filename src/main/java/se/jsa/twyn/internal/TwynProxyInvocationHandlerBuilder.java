package se.jsa.twyn.internal;

import java.lang.reflect.Proxy;

import org.codehaus.jackson.JsonNode;

public class TwynProxyInvocationHandlerBuilder implements TwynProxyBuilder {

	@Override
	public <T> T buildProxy(Class<T> type, TwynContext twynContext, JsonNode jsonNode) {
		return type.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
				new Class<?>[] { type },
				new TwynProxyInvocationHandler(jsonNode, twynContext, type)));
	}

	@Override
	public String toString() {
		return "TwynProxyInvocationHandlerBuilder []";
	}

}
