package se.jsa.twyn.internal;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Spliterators;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import se.jsa.twyn.TwynCollection;

class TwynProxyInvocationHandler implements InvocationHandler, JsonNodeHolder {
	private static final Object[] NO_ARGS = new Object[] {};

	private final JsonNode jsonNode;
	private final TwynContext twynContext;
	private final Class<?> implementedType;
	private final Cache cache;
	private final InvocationHandlerNodeResolver invocationHandlerNodeResolver;

	public TwynProxyInvocationHandler(JsonNode jsonNode, TwynContext twynContext, Class<?> implementedType) {
		this.jsonNode = jsonNode;
		this.twynContext = Objects.requireNonNull(twynContext);
		this.implementedType = Objects.requireNonNull(implementedType);
		this.cache = Objects.requireNonNull(twynContext.createCache());
		this.invocationHandlerNodeResolver = InvocationHandlerNodeResolver.getResolver(implementedType, jsonNode);
	}

	public static TwynProxyInvocationHandler create(JsonNode jsonNode, TwynContext twynContext, Class<?> implementedType) throws Exception {
		return new TwynProxyInvocationHandler(jsonNode, twynContext, implementedType);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (hasSignature(method, "toString")) {
	    	return toString();
	    } else if (hasSignature(method, "equals", Object.class)) {
	    	return equals(args[0]);
	    } else if (hasSignature(method, "hashCode")) {
	    	return hashCode();
	    }

		switch (MethodType.getType(method)) {
		case DEFAULT:	return callDefaultMethod(proxy, method, args);
		case ARRAY:		return cache.get(method, () -> innerArrayProxy(method));
		case LIST:		return cache.get(method, () -> innerCollectionProxy(method, Collectors.toList()));
		case SET:		return cache.get(method, () -> innerCollectionProxy(method, Collectors.toSet()));
		case MAP:		return cache.get(method, () -> innerMapProxy(method));
		case INTERFACE:	return cache.get(method, () -> innerProxy(method));
		case SET_VALUE: return setValue(proxy, method, args);

		case VALUE:		return cache.get(method, () -> resolveValue(method));
		default:		throw new RuntimeException("Could not handle methodType=" + MethodType.getType(method));
		}
	}

	private boolean hasSignature(Method method, String name, Class<?>... params) {
		return method.getName().equals(name) && Arrays.deepEquals(method.getParameterTypes(), params);
	}

	private Object callDefaultMethod(Object proxy, Method method, Object[] args) throws InstantiationException, IllegalArgumentException, Throwable {
		Class<?> declaringClass = method.getDeclaringClass();
		return twynContext.lookup(declaringClass)
		        .unreflectSpecial(method, declaringClass)
		        .bindTo(proxy)
		        .invokeWithArguments(args);
	}

	@SuppressWarnings("unchecked")
	private <T, A, R> R innerCollectionProxy(Method method, Collector<T, A, R> collector) {
		TwynCollection annotation = method.getAnnotation(TwynCollection.class);
		return collect((Class<T>)annotation.value(), resolveTargetGetNode(method), annotation.parallel(), collector);
	}

	private Map<?, ?> innerMapProxy(Method method) {
		TwynCollection annotation = method.getAnnotation(TwynCollection.class);
		Class<?> componentType = annotation.value();
		return StreamSupport
				.stream(Spliterators.spliteratorUnknownSize(resolveTargetGetNode(method).getFields(), 0), annotation.parallel())
				.collect(Collectors.toMap(Entry::getKey, (entry) -> twynContext.proxy(entry.getValue(), componentType)));
	}

	@SuppressWarnings("unchecked")
	private <T> Object innerArrayProxy(Method method) {
		Class<T> componentType = (Class<T>) method.getReturnType().getComponentType();
		TwynCollection annotation = method.getAnnotation(TwynCollection.class);
		List<T> result = collect(componentType, resolveTargetGetNode(method), annotation != null ? annotation.parallel() : false, Collectors.toList());
		return result.toArray((T[]) Array.newInstance(componentType, result.size()));
	}

	private Object innerProxy(Method method) {
		return twynContext.proxy(resolveTargetGetNode(method), method.getReturnType());
	}

	private Object setValue(Object proxy, Method method, Object[] args) {
		if (!BasicJsonTypes.isBasicJsonType(args[0].getClass())) {
			((ObjectNode) jsonNode).put(TwynUtil.decodeJavaBeanSetName(method.getName()), twynContext.writeValue(args[0]));
		} else {
			switch (BasicJsonTypes.get(args[0].getClass())) {
			case BIG_DECIMAL:	((ObjectNode) jsonNode).put(TwynUtil.decodeJavaBeanSetName(method.getName()), (BigDecimal)args[0]); break;
			case BOOLEAN:		((ObjectNode) jsonNode).put(TwynUtil.decodeJavaBeanSetName(method.getName()), (Boolean)args[0]); break;
			case BYTE_ARRAY:	((ObjectNode) jsonNode).put(TwynUtil.decodeJavaBeanSetName(method.getName()), (byte[])args[0]); break;
			case DOUBLE:		((ObjectNode) jsonNode).put(TwynUtil.decodeJavaBeanSetName(method.getName()), (Double)args[0]); break;
			case FLOAT:			((ObjectNode) jsonNode).put(TwynUtil.decodeJavaBeanSetName(method.getName()), (Float)args[0]); break;
			case INTEGER:		((ObjectNode) jsonNode).put(TwynUtil.decodeJavaBeanSetName(method.getName()), (Integer)args[0]); break;
			case LONG:			((ObjectNode) jsonNode).put(TwynUtil.decodeJavaBeanSetName(method.getName()), (Long)args[0]); break;
			case STRING:		((ObjectNode) jsonNode).put(TwynUtil.decodeJavaBeanSetName(method.getName()), (String)args[0]); break;
			}
		}
		cache.clear();
		return proxy;
	}

	private Object resolveValue(Method method) {
		try {
			return twynContext.readValue(resolveTargetGetNode(method), method.getReturnType());
		} catch (IOException e) {
			throw new RuntimeException("Could not resolve value for node " + resolveTargetGetNode(method) + ". Wanted type: " + method.getReturnType(), e);
		}
	}

	private <T, A, R> R collect(Class<T> componentType, JsonNode jsonNode, boolean parallel, Collector<T, A, R> collector) {
		return StreamSupport.stream(jsonNode.spliterator(), parallel)
			.map((n) -> {
				try {
					return componentType.isInterface() ? twynContext.proxy(n, componentType) : twynContext.readValue(n, componentType);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} )
			.collect(collector);
	}

	private JsonNode resolveTargetGetNode(Method method) {
		return invocationHandlerNodeResolver.resolveNode(implementedType, method, jsonNode);
	}

	@Override
	public JsonNode getJsonNode() {
		return jsonNode;
	}

	@Override
	public String toString() {
		return "TwynInvocationHandler<" + implementedType.getSimpleName() + "> [" +
				twynContext.getIdentityMethods(implementedType)
				.map((m) -> {
					try {
						return m.getName() + "()=" + this.invoke(null, m, NO_ARGS).toString();
					} catch (Throwable e) {
						throw new RuntimeException("Could not call method " + m + " for toString calculation.", e);
					}
				})
				.reduce(null, (s1, s2) -> { return (s1 == null ? s2 : (s2 == null ? s1 : s1 + ", " + s2)); })
				+ (twynContext.isDebug() ? ", node=\"" + jsonNode + "\"": "")
				+ "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!implementedType.isAssignableFrom(obj.getClass())) {
			return false;
		}
		return twynContext.getIdentityMethods(implementedType)
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
		return Objects.hash(twynContext.getIdentityMethods(implementedType)
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
