/*
 * Copyright 2015 Joakim Sahlström
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.jsa.twyn.internal;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterators;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import se.jsa.twyn.TwynCollection;
import se.jsa.twyn.TwynProxyException;
import se.jsa.twyn.internal.ProxiedInterface.ImplementedMethod;
import se.jsa.twyn.internal.ProxiedInterface.ImplementedMethodMethod;
import se.jsa.twyn.internal.ProxiedInterface.ProxiedElementClass;

class TwynProxyInvocationHandler implements InvocationHandler, NodeSupplier {
	private static final Object[] NO_ARGS = new Object[] {};

	private final JsonNode jsonNode;
	private final TwynContext twynContext;
	private final ProxiedElementClass implementedType;
	private final Cache cache;
	private final NodeResolver invocationHandlerNodeResolver;

	public TwynProxyInvocationHandler(JsonNode jsonNode, TwynContext twynContext, ProxiedElementClass implementedType) {
		this.jsonNode = jsonNode;
		this.twynContext = Objects.requireNonNull(twynContext);
		this.implementedType = Objects.requireNonNull(implementedType);
		this.cache = Objects.requireNonNull(twynContext.createCache());
		this.invocationHandlerNodeResolver = NodeResolver.getResolver(implementedType);
	}

	public static TwynProxyInvocationHandler create(JsonNode jsonNode, TwynContext twynContext, ProxiedElementClass implementedType) throws Exception {
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

		MethodType methodType = MethodType.getType(ImplementedMethod.of(method));
		switch (methodType) {
		case DEFAULT:	return callDefaultMethod(proxy, method, args);
		case ARRAY:		return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> innerArrayProxy(method));
		case LIST:		return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> innerCollectionProxy(method, Collectors.toList()));
		case SET:		return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> innerCollectionProxy(method, Collectors.toSet()));
		case MAP:		return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> innerMapProxy(method));
		case INTERFACE:	return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> innerProxy(method));
		case SET_VALUE: return setValue(proxy, method, args);

		case VALUE:		return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> resolveValue(method));
		default:		throw new TwynProxyException("Could not handle methodType=" + methodType);
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
		JsonNode node = resolveTargetGetNode(method);
		Require.that(node.isArray(), ErrorFactory.proxyCollectionJsonNotArrayType(annotation.value(), method, jsonNode));
		return twynContext.proxyCollection((Class<T>)annotation.value(), node, annotation.parallel(), collector);
	}

	private Map<?, ?> innerMapProxy(Method method) {
		TwynCollection annotation = method.getAnnotation(TwynCollection.class);
		Class<?> valueComponentType = annotation.value();
		Class<?> keyType = annotation.keyType();

		JsonNode node = resolveTargetGetNode(method);
		Require.that(node.isContainerNode(), ErrorFactory.innerMapProxyNoMapStructure(method, node));
		return StreamSupport
				.stream(Spliterators.spliteratorUnknownSize(node.fields(), 0), annotation.parallel())
				.collect(Collectors.toMap((entry) -> readKeyType(entry.getKey(), keyType), (entry) -> twynContext.proxy(entry.getValue(), valueComponentType)));
	}

	private Object readKeyType(String key, Class<?> keyType) {
		if (keyType.equals(String.class)) {
			return key;
		} else {
			try {
				return keyType.getConstructor(String.class).newInstance(key);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new TwynProxyException("Could not create map keyType=" + keyType + " from key=" + key, e);
			}
		}
	}

	private <T> Object innerArrayProxy(Method method) {
		@SuppressWarnings("unchecked")
		Class<T> componentType = (Class<T>) method.getReturnType().getComponentType();
		TwynCollection annotation = method.getAnnotation(TwynCollection.class);
		JsonNode node = resolveTargetGetNode(method);
		Require.that(node.isArray(), ErrorFactory.proxyArrayJsonNotArrayType(componentType, method, jsonNode));
		return twynContext.proxyArray(node, componentType, annotation != null ? annotation.parallel() : false);
	}

	private Object innerProxy(Method method) {
		JsonNode node = resolveTargetGetNode(method);
		Require.that(node.isContainerNode(), ErrorFactory.innerProxyNoStruct(method, node));
		return twynContext.proxy(node, method.getReturnType());
	}

	private Object setValue(Object proxy, Method method, Object[] args) {
		if (!BasicJsonTypes.isBasicJsonType(args[0].getClass())) {
			((ObjectNode) jsonNode).set(TwynUtil.decodeJavaBeanSetName(method.getName()), twynContext.writeValue(args[0]));
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
		cache.clear(TwynUtil.decodeJavaBeanName(method.getName()));
		return proxy;
	}

	private Object resolveValue(Method method) {
		try {
			return twynContext.readValue(resolveTargetGetNode(method), method.getReturnType());
		} catch (IOException e) {
			throw new TwynProxyException("Could not resolve value for node " + resolveTargetGetNode(method) + ". Wanted type: " + method.getReturnType(), e);
		}
	}

	private JsonNode resolveTargetGetNode(Method method) {
		return Require.notNull(
				invocationHandlerNodeResolver.resolveNode(ImplementedMethod.of(method), jsonNode),
				ErrorFactory.couldNotResolveTargetNode(method, jsonNode));
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
						return m.getName() + "()=" + this.invoke(null, getMethod(m), NO_ARGS).toString();
					} catch (Throwable e) {
						throw new TwynProxyException("Could not call method " + m + " for toString calculation.", e);
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
					return Objects.equals(getMethod(m).invoke(obj), this.invoke(null, getMethod(m), NO_ARGS));
				} catch (Throwable e) {
					throw new TwynProxyException("Could not call method " + m + " for equals comparison..", e);
				}
			});
	}

	@Override
	public int hashCode() {
		return Objects.hash(twynContext.getIdentityMethods(implementedType)
				.map((m) -> {
					try {
						return this.invoke(null, getMethod(m), NO_ARGS);
					} catch (Throwable e) {
						throw new TwynProxyException("Could not call method " + m + " for hashCode calculation.", e);
					}
				})
				.toArray());
	}

	private Method getMethod(ImplementedMethod method) {
		return ((ImplementedMethodMethod) method).getMethod();
	}

}