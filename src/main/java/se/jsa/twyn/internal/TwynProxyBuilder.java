package se.jsa.twyn.internal;

import com.fasterxml.jackson.databind.JsonNode;

public interface TwynProxyBuilder {
	<T> T buildProxy(Class<T> type, TwynContext twynContext, JsonNode jsonNode);
}
