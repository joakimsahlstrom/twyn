package se.jsa.twyn.internal;

import org.codehaus.jackson.JsonNode;

public interface TwynProxyBuilder {
	<T> T buildProxy(Class<T> type, TwynContext twynContext, JsonNode jsonNode);
}
