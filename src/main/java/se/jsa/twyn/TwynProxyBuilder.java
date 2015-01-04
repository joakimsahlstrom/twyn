package se.jsa.twyn;

import org.codehaus.jackson.JsonNode;

public interface TwynProxyBuilder {

	<T> T buildProxy(Class<T> type, Twyn twyn, JsonNode jsonNode) throws Exception;

}
