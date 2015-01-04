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
	private static final Object[] NO_ARGS = new Object[] {};

	private final JsonNode jsonNode;
	private final TwynContext twyn;
	private final Class<?> implementedType;

	public TwynProxyInvocationHandler(JsonNode jsonNode, TwynContext twynContext, Class<?> implementedType) {
		this.jsonNode = jsonNode;
		this.twyn = Objects.requireNonNull(twynContext);
		this.implementedType = Objects.requireNonNull(implementedType);
	}

	public static TwynProxyInvocationHandler create(JsonNode jsonNode, TwynContext twynContext, Class<?> implementedType) throws Exception {
		return new TwynProxyInvocationHandler(jsonNode, twynContext, implementedType);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getName().equals("toString") && method.getParameterTypes().length == 0) {
	    	return toString();
	    } else if (method.getName().equals("equals") && method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Object.class) {
	    	return equals(args[0]);
	    } else if (method.getName().equals("hashCode") && method.getParameterTypes().length == 0) {
	    	return hashCode();
	    }

		switch (MethodType.getType(method)) {
		case DEFAULT:	return callDefaultMethod(proxy, method, args);
		case ARRAY:		return innerArrayProxy(method);
		case LIST:		return innerListProxy(method);
		case MAP:		return innerMapProxy(method);
		case INTERFACE:	return innerProxy(method);
		case VALUE:		return resolveValue(method);
		default:		throw new RuntimeException("Could not handle methodType=" + MethodType.getType(method));
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
		return jsonNode.get(TwynUtil.decodeJavaBeanName(method.getName()));
	}

	@Override
	public String toString() {
		return "TwynInvocationHandler proxy. Node=" + jsonNode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!implementedType.isAssignableFrom(obj.getClass())) {
			return false;
		}
		return IdentityMethods.get(implementedType)
			.allMatch((m) -> {
				try {
					return Objects.equals(m.invoke(obj), this.invoke(null, m, NO_ARGS));
				} catch (Throwable e) {
					throw new RuntimeException("Could not call method " + m + " for equals comparison..", e);
				}
			});
	}

	@Override
	public int hashCode() {
		return Objects.hash(IdentityMethods.get(implementedType)
				.map((m) -> {
					try {
						return this.invoke(null, m, NO_ARGS);
					} catch (Throwable e) {
						throw new RuntimeException("Could not call method " + m + " for hashCode calculation.", e);
					}
				})
				.toArray());
	}

}
