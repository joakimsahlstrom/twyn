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
import java.lang.reflect.Array;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.jsa.twyn.TwynProxyException;
import se.jsa.twyn.internal.write.TwynProxyBuilder;
import se.jsa.twyn.internal.write.cg.TwynProxyClassBuilder;

public class TwynContext {

	private final ObjectMapper objectMapper;
	private final TwynProxyBuilder proxyBuilder;
	private final Supplier<Cache> cacheSupplier;
	private final boolean debug;
	private final IdentityMethods identityMethods = new IdentityMethods();

	public TwynContext(ObjectMapper objectMapper, TwynProxyBuilder proxyBuilder, Supplier<Cache> cacheSupplier, boolean debug) {
		this.objectMapper = Objects.requireNonNull(objectMapper);
		this.proxyBuilder = Objects.requireNonNull(proxyBuilder);
		this.cacheSupplier = Objects.requireNonNull(cacheSupplier);
		this.debug = debug;
	}

	public <T> T proxy(JsonNode jsonNode, Class<T> type) {
		try {
			return proxyBuilder.buildProxy(type, this, jsonNode);
		} catch (Exception e) {
			throw new TwynProxyException("Could not create Twyn proxy", e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> Object proxyArray(JsonNode node, Class<T> componentType) {
		List<T> result = proxyCollection(componentType, node, Collectors.toList());
		return result.toArray((T[]) Array.newInstance(componentType, result.size()));
	}

	public <T, A, R> R proxyCollection(Class<T> componentType, JsonNode jsonNode, Collector<T, A, R> collector) {
		return StreamSupport.stream(jsonNode.spliterator(), false)
				.map((n) -> {
					try {
						return componentType.isInterface() ? proxy(n, componentType) : readValue(n, componentType);
					} catch (Exception e) {
						throw new TwynProxyException("Could not proxy collection " + n + " of type " + componentType, e);
					}
				} )
				.collect(collector);
	}

	public <T> T readValue(JsonNode resolvedTargetNode, Class<T> valueType) throws JsonParseException, JsonMappingException, IOException {
		return objectMapper.treeToValue(resolvedTargetNode, valueType);
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

	public IdentityMethods getIdentityMethods() {
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
