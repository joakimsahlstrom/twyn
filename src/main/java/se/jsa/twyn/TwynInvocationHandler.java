package se.jsa.twyn;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import se.jsa.twyn.Twyn.ObjectMapperFacade;

public class TwynInvocationHandler implements InvocationHandler {

	private final JsonNode tree;
	private ObjectMapper objectMapper;
	private Twyn twyn;
	
	public TwynInvocationHandler(JsonNode tree, ObjectMapper objectMapper, Twyn jsonProxy) {
		this.tree = tree;
		this.objectMapper = Objects.requireNonNull(objectMapper);
		this.twyn = Objects.requireNonNull(jsonProxy);
	}
	
	interface reader {
		JsonNode readWith(ObjectMapper mapper);
	}
	
	public static TwynInvocationHandler create(ObjectMapperFacade reader, Twyn jsonProxy) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		return new TwynInvocationHandler(reader.apply(objectMapper), objectMapper, jsonProxy);
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	    if (method.isDefault()) {
	        return callDefaultMethod(proxy, method, args);
	    } else if (method.getReturnType().isInterface()) {
	    	return innerProxy(method.getReturnType(), tree.get(resolveJavaBeanName(method.getName())));
	    } else if (method.getReturnType().isArray() && method.getReturnType().getComponentType().isInterface()) {
	    	return innerArrayProxy(method.getReturnType().getComponentType(), tree.get(resolveJavaBeanName(method.getName())));
	    } else {
	    	return objectMapper.readValue(tree.get(resolveJavaBeanName(method.getName())), method.getReturnType());
	    }
	}

	private Object callDefaultMethod(Object proxy, Method method, Object[] args) throws InstantiationException, IllegalArgumentException, Throwable {
		Class<?> declaringClass = method.getDeclaringClass();
		return twyn.lookup(declaringClass)
		        .unreflectSpecial(method, declaringClass)
		        .bindTo(proxy)
		        .invokeWithArguments(args);
	}

	private Object innerProxy(Class<?> type, JsonNode jsonNode) {
		return twyn.proxy(type, jsonNode, objectMapper);
	}
	
	private <T> T[] innerArrayProxy(Class<T> componentType, JsonNode jsonNode) {
		@SuppressWarnings("unchecked")
		T[] result = (T[]) Array.newInstance(componentType, jsonNode.size());
		for (int i = 0; i < jsonNode.size(); i++) {
			result[i] = twyn.proxy(componentType, jsonNode.get(i), objectMapper);
		}
		return result;
	}

	private static final String[] PREFIXES = new String[] { "get", "is" };
	private String resolveJavaBeanName(String name) {
		for (String prefix : PREFIXES) {
			int prefixLength = prefix.length();
			if (name.startsWith(prefix) && name.length() > prefixLength && Character.isUpperCase(name.charAt(prefixLength))) {
				return name.substring(prefixLength, prefixLength + 1).toLowerCase() + name.substring(prefixLength + 1);
			}
		}
		return name;
	}
	
	
	
}
