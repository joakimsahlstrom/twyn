package se.jsa.twyn;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import se.jsa.twyn.Twyn.JsonProducer;

class TwynInvocationHandler implements InvocationHandler {

	private final JsonNode tree;
	private final ObjectMapper objectMapper;
	private final Twyn twyn;
	
	public TwynInvocationHandler(JsonNode tree, ObjectMapper objectMapper, Twyn twyn) {
		this.tree = tree;
		this.objectMapper = Objects.requireNonNull(objectMapper);
		this.twyn = Objects.requireNonNull(twyn);
	}
	
	public static TwynInvocationHandler create(JsonProducer jsonProducer, Twyn twyn) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		return new TwynInvocationHandler(jsonProducer.get(objectMapper), objectMapper, twyn);
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	    if (method.isDefault()) {
	        return callDefaultMethod(proxy, method, args);
	    } else if (method.getReturnType().isArray() && method.getReturnType().getComponentType().isInterface()) {
	    	return innerArrayProxy(method.getReturnType().getComponentType(), tree.get(resolveJavaBeanName(method.getName())));
	    } else if (method.getReturnType().equals(List.class)) {
	    	return collect(method.getAnnotation(TwynCollection.class).value(), tree.get(resolveJavaBeanName(method.getName())), Collectors.toList());
	    } else if (method.getReturnType().isInterface()) {
	    	return innerProxy(method.getReturnType(), tree.get(resolveJavaBeanName(method.getName())));
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
	
	@SuppressWarnings("unchecked")
	private <T> T[] innerArrayProxy(Class<T> componentType, JsonNode jsonNode) {
		List<T> result = collect(componentType, jsonNode, Collectors.toList());
		return result.toArray((T[])Array.newInstance(componentType, result.size()));
	}
	
	private <R, T, A> R collect(Class<T> componentType, JsonNode jsonNode, Collector<T, ? super A, R> collector) {
		return StreamSupport.stream((Spliterator<JsonNode>)jsonNode.spliterator(), false)
			.flatMap(n -> Stream.of((T)twyn.proxy(componentType, n, objectMapper)))
			.collect(collector);
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
