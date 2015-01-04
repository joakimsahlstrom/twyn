package se.jsa.twyn.internal;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;

import se.jsa.twyn.TwynCollection;

class TwynProxyInvocationHandler implements InvocationHandler {
	private final JsonNode tree;
	private final TwynContext twyn;
	
	public TwynProxyInvocationHandler(JsonNode tree, TwynContext twynContext) {
		this.tree = tree;
		this.twyn = Objects.requireNonNull(twynContext);
	}
	
	public static TwynProxyInvocationHandler create(JsonNode jsonNode, TwynContext twynContext) throws Exception {
		return new TwynProxyInvocationHandler(jsonNode, twynContext);
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
		TwynCollection annotation = method.getAnnotation(TwynCollection.class);
		return collect(annotation.value(), resolveTargetNode(method), annotation.parallel());
	}
	
	private Map<?, ?> innerMapProxy(Method method) {
		TwynCollection annotation = method.getAnnotation(TwynCollection.class);
		Class<?> componentType = annotation.value();
		return StreamSupport
				.stream(Spliterators.spliteratorUnknownSize(resolveTargetNode(method).getFields(), 0), annotation.parallel())
				.collect(Collectors.toMap(Entry::getKey, (entry) -> twyn.proxy(entry.getValue(), componentType)));
	}

	@SuppressWarnings("unchecked")
	private <T> Object innerArrayProxy(Method method) {
		Class<T> componentType = (Class<T>) method.getReturnType().getComponentType();
		TwynCollection annotation = method.getAnnotation(TwynCollection.class);
		List<T> result = collect(componentType, resolveTargetNode(method), annotation != null ? annotation.parallel() : false);
		return result.toArray((T[]) Array.newInstance(componentType, result.size()));
	}
	
	private Object innerProxy(Method method) {
		return twyn.proxy(resolveTargetNode(method), method.getReturnType());
	}

	private Object resolveValue(Method method) throws IOException, JsonParseException, JsonMappingException {
		return twyn.readValue(resolveTargetNode(method), method.getReturnType());
	}
	
	private <T> List<T> collect(Class<T> componentType, JsonNode jsonNode, boolean parallel) {
		return StreamSupport.stream(jsonNode.spliterator(), parallel)
			.map(n -> twyn.proxy(n, componentType))
			.collect(Collectors.toList());
	}

	private JsonNode resolveTargetNode(Method method) {
		return tree.get(TwynUtil.decodeJavaBeanName(method.getName()));
	}
		
	public String toString() {
		return "TwynInvocationHandler proxy. Node=" + tree;
	}
}
