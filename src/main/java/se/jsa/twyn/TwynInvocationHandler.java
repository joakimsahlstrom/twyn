package se.jsa.twyn;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Spliterator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;

import se.jsa.twyn.Twyn.JsonProducer;

class TwynInvocationHandler implements InvocationHandler {
	private final JsonNode tree;
	private final Twyn twyn;
	
	public TwynInvocationHandler(JsonNode tree, Twyn twyn) {
		this.tree = tree;
		this.twyn = Objects.requireNonNull(twyn);
	}
	
	public static TwynInvocationHandler create(JsonProducer jsonProducer, Twyn twyn) throws Exception {
		return new TwynInvocationHandler(jsonProducer.get(), twyn);
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	    if (method.isDefault()) {
	        return callDefaultMethod(proxy, method, args);
	    } else if (method.getReturnType().isArray() && method.getReturnType().getComponentType().isInterface()) {
	    	return innerArrayProxy(method);
	    } else if (method.getReturnType().equals(List.class) && method.getAnnotation(TwynCollection.class) != null) {
	    	return innerListProxy(method);
	    } else if (method.getReturnType().equals(Map.class) && method.getAnnotation(TwynCollection.class) != null) {
	    	return innerMapProxy(method);
	    } else if (method.getReturnType().isInterface()) {
	    	return innerProxy(method);
	    } else if (method.getName().equals("toString") && method.getParameterTypes().length == 0) {
	    	return toString();
	    } else {
	    	return resolveValue(method);
	    }
	}

	private Object callDefaultMethod(Object proxy, Method method, Object[] args) throws InstantiationException, IllegalArgumentException, Throwable {
		Class<?> declaringClass = method.getDeclaringClass();
		return twyn.lookup(declaringClass)
		        .unreflectSpecial(method, declaringClass)
		        .bindTo(proxy)
		        .invokeWithArguments(args);
	}
	
	private List<?> innerListProxy(Method method) {
		return collect(method.getAnnotation(TwynCollection.class).value(), resolveTargetNode(method), Collectors.toList());
	}
	
	private Map<?, ?> innerMapProxy(Method method) {
		Class<?> componentType = method.getAnnotation(TwynCollection.class).value();
		Iterator<Entry<String, JsonNode>> fields = resolveTargetNode(method).getFields();

		Map<Object, Object> result = new HashMap<>();
		while (fields.hasNext()) {
			Entry<String, JsonNode> entry = fields.next();
			result.put(entry.getKey(), twyn.proxy(componentType, entry.getValue()));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private <T> Object innerArrayProxy(Method method) {
		Class<T> componentType = (Class<T>) method.getReturnType().getComponentType();
		List<T> result = collect(componentType, resolveTargetNode(method), Collectors.toList());
		return result.toArray((T[]) Array.newInstance(componentType, result.size()));
	}

	private Object innerProxy(Method method) {
		return twyn.proxy(method.getReturnType(), resolveTargetNode(method));
	}

	private Object resolveValue(Method method) throws IOException, JsonParseException, JsonMappingException {
		return twyn.readValue(resolveTargetNode(method), method.getReturnType());
	}
	
	private <R, T, A> R collect(Class<T> componentType, JsonNode jsonNode, Collector<T, ? super A, R> collector) {
		return StreamSupport.stream((Spliterator<JsonNode>)jsonNode.spliterator(), false)
			.flatMap(n -> Stream.of((T)twyn.proxy(componentType, n)))
			.collect(collector);
	}

	private JsonNode resolveTargetNode(Method method) {
		return tree.get(decodeJavaBeanName(method.getName()));
	}

	private static final String[] PREFIXES = new String[] { "get", "is" };
	private String decodeJavaBeanName(String name) {
		for (String prefix : PREFIXES) {
			int prefixLength = prefix.length();
			if (name.startsWith(prefix) && name.length() > prefixLength && Character.isUpperCase(name.charAt(prefixLength))) {
				return name.substring(prefixLength, prefixLength + 1).toLowerCase() + name.substring(prefixLength + 1);
			}
		}
		return name;
	}
	
	public String toString() {
		return "TwynInvocationHandler proxy. Node=" + tree;
	}
}
