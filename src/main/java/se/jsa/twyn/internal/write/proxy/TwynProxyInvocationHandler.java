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
package se.jsa.twyn.internal.write.proxy;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

import se.jsa.twyn.TwynProxyException;
import se.jsa.twyn.internal.Cache;
import se.jsa.twyn.internal.ErrorFactory;
import se.jsa.twyn.internal.MethodType;
import se.jsa.twyn.internal.NodeSupplier;
import se.jsa.twyn.internal.Require;
import se.jsa.twyn.internal.TwynContext;
import se.jsa.twyn.internal.read.ImplementedMethod;
import se.jsa.twyn.internal.read.reflect.ImplementedMethodMethod;
import se.jsa.twyn.internal.read.reflect.ProxiedInterfaceClass;
import se.jsa.twyn.internal.write.common.NodeResolver;
import se.jsa.twyn.internal.write.common.TwynUtil;

class TwynProxyInvocationHandler implements InvocationHandler, NodeSupplier {
	private static final Object[] NO_ARGS = new Object[] {};

	private final JsonNode jsonNode;
	private final TwynContext twynContext;
	private final ProxiedInterfaceClass implementedType;
	private final Cache cache;
	private final NodeResolver nodeResolver;
	private final DefaultMethodLookup defaultMethodLookup;

	public TwynProxyInvocationHandler(JsonNode jsonNode, TwynContext twynContext, ProxiedInterfaceClass implementedType) {
		this.jsonNode = jsonNode;
		this.twynContext = Objects.requireNonNull(twynContext);
		this.implementedType = Objects.requireNonNull(implementedType);
		this.cache = Objects.requireNonNull(twynContext.createCache());
		this.nodeResolver = NodeResolver.getResolver(implementedType);
		this.defaultMethodLookup = DefaultMethodLookup.create();
	}

	public static TwynProxyInvocationHandler create(JsonNode jsonNode, TwynContext twynContext, ProxiedInterfaceClass implementedType) throws Exception {
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

		ImplementedMethod implementedMethod = ImplementedMethod.of(method);
        switch (MethodType.getType(implementedMethod)) {
		    case DEFAULT:	return callDefaultMethod(proxy, method, args);
		    case ARRAY:		return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> innerArrayProxy(method));
		    case LIST:		return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> innerCollectionProxy(method, Collectors.toList()));
		    case SET:		return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> innerCollectionProxy(method, Collectors.toSet()));
		    case MAP:		return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> innerMapProxy(method));
		    case INTERFACE:	return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> innerProxy(method));
			case VALUE:		return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> resolveValue(method));
			case OPTIONAL:	return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> resolveOptional(method));
            case SET_VALUE: return setValue(proxy, method, args);
            default:		throw ErrorFactory.proxyValidationError(implementedType, implementedMethod).get();
		}
	}

	private boolean hasSignature(Method method, String name, Class<?>... params) {
		return method.getName().equals(name) && Arrays.deepEquals(method.getParameterTypes(), params);
	}

	private Object callDefaultMethod(Object proxy, Method method, Object[] args) throws InstantiationException, IllegalArgumentException, Throwable {
		Class<?> declaringClass = method.getDeclaringClass();
		return defaultMethodLookup.lookup(declaringClass)
		        .unreflectSpecial(method, declaringClass)
		        .bindTo(proxy)
		        .invokeWithArguments(args);
	}

	@SuppressWarnings("unchecked")
	private <T, A, R> R innerCollectionProxy(Method method, Collector<T, A, R> collector) {
		ImplementedMethod implementedMethod = ImplementedMethod.of(method);
		String returnTypeParameterTypeCanonicalName = implementedMethod.getReturnTypeParameterTypeCanonicalName(0);
		return tryResolveTargetGetNode(method).map(node -> {
			Require.that(node.isArray(), ErrorFactory.proxyCollectionJsonNotArrayType(returnTypeParameterTypeCanonicalName, method, jsonNode));
			return twynContext.proxyCollection((Class<T>)implementedMethod.getReturnTypeParameterType(0),
				node,
				collector);
		}).orElseGet(() -> collector.finisher().apply(collector.supplier().get()));
	}

	private Object innerMapProxy(Method method) {
		ImplementedMethod implementedMethod = ImplementedMethod.of(method);
		Class<?> valueComponentType = implementedMethod.getReturnTypeParameterType(1);
		Class<?> keyType = implementedMethod.getReturnTypeParameterType(0);

		return tryResolveTargetGetNode(method).map(node -> {
			Require.that(node.isContainerNode(), ErrorFactory.innerMapProxyNoMapStructure(method, node));
			return StreamSupport
					.stream(Spliterators.spliteratorUnknownSize(node.fields(), 0), false)
					.collect(Collectors.toMap((entry) -> readKeyType(entry.getKey(), keyType), (entry) -> twynContext.proxy(entry.getValue(), valueComponentType)));
		}).orElseGet(Collections::emptyMap);
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
		return tryResolveTargetGetNode(method).map(node -> {
			Require.that(node.isArray(), ErrorFactory.proxyArrayJsonNotArrayType(componentType, method, jsonNode));
			return twynContext.proxyArray(node, componentType);
		}).orElseGet(() -> Array.newInstance(componentType, 0));
	}

	private Object innerProxy(Method method) {
		return tryResolveTargetGetNode(method).map(node -> {
			Require.that(node.isContainerNode(), ErrorFactory.innerProxyNoStruct(method, node));
			return twynContext.proxy(node, method.getReturnType());
		}).orElse(null);
	}

	private Object setValue(Object proxy, Method method, Object[] args) {
		if (!BasicJsonTypes.isBasicJsonType(args[0].getClass())) {
			nodeResolver.setNode(ImplementedMethod.of(method), jsonNode, twynContext.writeValue(args[0]));
		} else {
			switch (BasicJsonTypes.get(args[0].getClass())) {
				case BIG_DECIMAL: 	nodeResolver.setNode(ImplementedMethod.of(method), jsonNode, DecimalNode.valueOf((BigDecimal)args[0])); break;
				case BOOLEAN:		nodeResolver.setNode(ImplementedMethod.of(method), jsonNode, BooleanNode.valueOf((Boolean)args[0])); break;
				case BYTE_ARRAY:	nodeResolver.setNode(ImplementedMethod.of(method), jsonNode, BinaryNode.valueOf((byte[])args[0])); break;
				case DOUBLE:		nodeResolver.setNode(ImplementedMethod.of(method), jsonNode, DoubleNode.valueOf((Double)args[0])); break;
				case FLOAT:			nodeResolver.setNode(ImplementedMethod.of(method), jsonNode, FloatNode.valueOf((Float)args[0])); break;
				case INTEGER:		nodeResolver.setNode(ImplementedMethod.of(method), jsonNode, IntNode.valueOf((Integer) args[0])); break;
				case LONG:			nodeResolver.setNode(ImplementedMethod.of(method), jsonNode, LongNode.valueOf((Long)args[0])); break;
				case STRING:		nodeResolver.setNode(ImplementedMethod.of(method), jsonNode, TextNode.valueOf((String)args[0])); break;
			}
		}
		cache.clear(TwynUtil.decodeJavaBeanName(method.getName()));
		return proxy;
	}

	private Object resolveValue(Method method) {
		return tryResolveTargetGetNode(method).map(node -> {
			try {
				return (Object) twynContext.readValue(node, method.getReturnType());
			} catch (IOException e) {
				throw new TwynProxyException("Could not resolve value for node " + node + ". Wanted type: " + method.getReturnType(), e);
			}
		}).orElseGet(() -> ImplementedMethod.of(method).returnsArray() ? Array.newInstance(method.getReturnType().getComponentType(), 0) : null);
	}

	private Object resolveOptional(Method method) {
		ImplementedMethod implementedMethod = ImplementedMethod.of(method);
		return implementedMethod.getReturnTypeParameterType(0).isInterface()
				? resolveOptionalInterface(method, implementedMethod)
				: resolveOptionalValue(method, implementedMethod);
	}

	private Object resolveOptionalInterface(Method method, ImplementedMethod implementedMethod) {
		return tryResolveTargetGetNode(method).map(node -> {
			Require.that(node.isContainerNode(), ErrorFactory.innerProxyNoStruct(method.getName(), implementedMethod.getReturnTypeParameterTypeCanonicalName(0), node));
			return Optional.of(twynContext.proxy(node, implementedMethod.getReturnTypeParameterType(0)));
		}).orElse(Optional.empty());
	}

	private Object resolveOptionalValue(Method method, ImplementedMethod implementedMethod) {
		return tryResolveTargetGetNode(method).<Object>map(node -> {
			try {
				return Optional.of(twynContext.readValue(node, implementedMethod.getReturnTypeParameterType(0)));
			} catch (IOException e) {
				throw new TwynProxyException("Could not resolve value for node " + node + ". Wanted type: " + implementedMethod.getReturnTypeParameterType(0), e);
			}
		}).orElse(Optional.empty());
	}

	private Optional<JsonNode> tryResolveTargetGetNode(Method method) {
		return Optional.ofNullable(nodeResolver.resolveNode(ImplementedMethod.of(method), jsonNode));
	}

	@Override
	public JsonNode getJsonNode() {
		return jsonNode;
	}

	@Override
	public String toString() {
		return "TwynProxyInvocationHandler<" + implementedType.getSimpleName() + "> [" +
				twynContext.getIdentityMethods().getIdentityMethods(implementedType)
				.map((m) -> {
					try {
						return m.getName() + "()=" + this.invoke(null, getMethod(m), NO_ARGS);
					} catch (Throwable e) {
						throw new TwynProxyException("Could not call method " + m + " for toString calculation.", e);
					}
				})
				.reduce(null, (s1, s2) -> { return (s1 == null ? s2 : (s2 == null ? s1 : s1 + ", " + s2)); })
				+ (twynContext.isDebug() ? ", node=\"" + jsonNode + "\"" : "")
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
		return twynContext.getIdentityMethods().getIdentityMethods(implementedType)
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
		return Objects.hash(twynContext.getIdentityMethods().getIdentityMethods(implementedType)
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