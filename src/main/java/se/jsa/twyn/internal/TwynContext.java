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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import se.jsa.twyn.BadJsonNodeTypeException;
import se.jsa.twyn.TwynProxyException;
import se.jsa.twyn.internal.ProxiedInterface.ImplementedMethod;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TwynContext {

	private final ObjectMapper objectMapper;
	private final TwynProxyBuilder proxyBuilder;
	private final Supplier<Cache> cacheSupplier;
	private final IdentityMethods identityMethods = new IdentityMethods();
	private final Constructor<MethodHandles.Lookup> methodHandleLookupConstructor;
	private final boolean debug;

	public TwynContext(ObjectMapper objectMapper, TwynProxyBuilder proxyBuilder, Supplier<Cache> cacheSupplier, boolean debug) {
		this.objectMapper = Objects.requireNonNull(objectMapper);
		this.proxyBuilder = Objects.requireNonNull(proxyBuilder);
		this.cacheSupplier = Objects.requireNonNull(cacheSupplier);
		this.debug = debug;

		try {
			this.methodHandleLookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
			if (!methodHandleLookupConstructor.isAccessible()) {
				methodHandleLookupConstructor.setAccessible(true);
			}
		} catch (NoSuchMethodException | SecurityException e) {
			throw new TwynProxyException("Unexpected internal error!");
		}
	}

	public Lookup lookup(Object declaringClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return methodHandleLookupConstructor.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE);
	}

	public <T> T proxy(JsonNode jsonNode, Class<T> type) {
		try {
			return proxyBuilder.buildProxy(type, this, jsonNode);
		} catch (Exception e) {
			throw new TwynProxyException("Could not create Twyn proxy", e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> Object proxyArray(JsonNode node, Class<T> componentType, boolean parallel) {
		List<T> result = proxyCollection(componentType, node, parallel, Collectors.toList());
		return result.toArray((T[]) Array.newInstance(componentType, result.size()));
	}

	public <T, A, R> R proxyCollection(Class<T> componentType, JsonNode jsonNode, boolean parallel, Collector<T, A, R> collector) {
		Require.that(jsonNode.isArray(), ErrorFactory.proxyCollectionJsonNotArrayType(componentType, jsonNode)); 
		return StreamSupport.stream(jsonNode.spliterator(), parallel)
				.map((n) -> {
					try {
						return componentType.isInterface() ? proxy(n, componentType) : readValue(n, componentType);
					} catch (Exception e) {
						throw new TwynProxyException("Could not proxy collection " + n + " of type " + componentType, e);
					}
				} )
				.collect(collector);
	}

	public <T> T readValue(JsonNode resolveTargetNode, Class<T> valueType) throws JsonParseException, JsonMappingException, IOException {
		return objectMapper.treeToValue(resolveTargetNode, valueType);
	}

	public JsonNode writeValue(Object object) {
		return getObjectMapper().valueToTree(object);
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public Cache createCache() {
		return cacheSupplier.get();
	}

	Stream<ImplementedMethod> getIdentityMethods(ProxiedInterface implementedType) {
		return identityMethods.getIdentityMethods(implementedType);
	}

	IdentityMethods getIdentityMethod() {
		return identityMethods;
	}

	public boolean isDebug() {
		return debug;
	}

	@Override
	public String toString() {
		return "TwynContext [objectMapper=" + objectMapper + ", proxyBuilder=" + proxyBuilder + "]";
	}

	public TwynContext precompile(Set<Class<?>> precompiledTypes) {
		if (proxyBuilder instanceof TwynProxyClassBuilder) {
			((TwynProxyClassBuilder) proxyBuilder).precompile(precompiledTypes, this);
		}
		return this;
	}

}
