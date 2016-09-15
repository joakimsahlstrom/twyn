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
package se.jsa.twyn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.jsa.twyn.internal.Cache;
import se.jsa.twyn.internal.ErrorFactory;
import se.jsa.twyn.internal.MethodType;
import se.jsa.twyn.internal.NodeSupplier;
import se.jsa.twyn.internal.ProxiedInterface;
import se.jsa.twyn.internal.ProxiedInterface.ProxiedElementClass;
import se.jsa.twyn.internal.TwynContext;
import se.jsa.twyn.internal.TwynProxyBuilder;
import se.jsa.twyn.internal.TwynProxyClassBuilder;
import se.jsa.twyn.internal.TwynProxyInvocationHandlerBuilder;

public class Twyn {
	private final TwynContext twynContext;

	private Twyn(TwynContext twynContext) {
		this.twynContext = Objects.requireNonNull(twynContext);
	}

	public <T> T read(InputStream inputStream, Class<T> type) throws JsonProcessingException, IOException {
		return read(() -> twynContext.getObjectMapper().readTree(inputStream), type);
	}

	public <T> T read(byte[] data, Class<T> type) throws JsonProcessingException, IOException {
		return read(() -> twynContext.getObjectMapper().readTree(data), type);
	}

	public <T> T read(File file, Class<T> type) throws JsonProcessingException, IOException {
		return read(() -> twynContext.getObjectMapper().readTree(file), type);
	}

	public <T> T read(JsonParser parser, Class<T> type) throws JsonProcessingException, IOException {
		return read(() -> twynContext.getObjectMapper().readTree(parser), type);
	}

	public <T> T read(Reader reader, Class<T> type) throws JsonProcessingException, IOException {
		return read(() -> twynContext.getObjectMapper().readTree(reader), type);
	}

	public <T> T read(String string, Class<T> type) throws JsonProcessingException, IOException {
		return read(() -> twynContext.getObjectMapper().readTree(string), type);
	}

	public <T> T read(URL url, Class<T> type) throws JsonProcessingException, IOException {
		return read(() -> twynContext.getObjectMapper().readTree(url), type);
	}

	@FunctionalInterface
	private interface JsonProducer {
		JsonNode get() throws Exception;
	}

	private <T> T read(JsonProducer jsonProducer, Class<T> type) throws JsonProcessingException, IOException {
		try {
			JsonNode node = jsonProducer.get();
			return type.isArray()
					? type.cast(twynContext.proxyArray(node, validate(type.getComponentType()), false))
					: twynContext.proxy(node, validate(type));
		} catch (IOException | RuntimeException e) { // also handles JsonProcessingException
			throw e;
		} catch (Exception e) {
			throw new TwynProxyException("Could not read input!", e);
		}
	}

	private <T> Class<T> validate(Class<T> type) {
		ProxiedElementClass proxiedInterface = ProxiedInterface.of(type);
		proxiedInterface.getMethods().stream()
			.filter(MethodType.ILLEGAL_TYPES_FILTER)
			.findAny()
			.ifPresent(m -> { throw ErrorFactory.proxyValidationError(proxiedInterface, m).get(); });
		return type;
	}

	public JsonNode getJsonNode(Object obj) {
		if (obj instanceof NodeSupplier) {
			return ((NodeSupplier) obj).getJsonNode();
		}
		try {
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(obj);
			NodeSupplier twynProxyInvocationHandler = ((NodeSupplier)invocationHandler);
			return twynProxyInvocationHandler.getJsonNode();
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Not a twyn object!", e);
		}
	}

	ObjectMapper getObjectMapper() {
		return twynContext.getObjectMapper();
	}

	public static Twyn forTest() {
		return configurer().withJavaProxies().configure();
	}

	// Builder

	public static SelectMethod configurer() {
		return new ConfigurerImpl();
	}

	private static class ConfigurerImpl implements SelectMethod, ClassGenerationConfigurer {
		private ObjectMapper objectMapper = new ObjectMapper();
		private Supplier<Cache> cacheSupplier = () -> new Cache.None();
		private TwynProxyBuilder twynProxyBuilder;
		private Set<Class<?>> precompiledTypes = Collections.<Class<?>>emptySet();
		boolean debug = false;

		@Override
		public Configurer withObjectMapper(ObjectMapper objectMapper) {
			return setAndReturn(c -> c.objectMapper = Objects.requireNonNull(objectMapper));
		}

		@Override
		public Configurer withJavaProxies() {
			return setAndReturn(c -> c.twynProxyBuilder = new TwynProxyInvocationHandlerBuilder());
		}

		@Override
		public ClassGenerationConfigurer withClassGeneration() {
			return setAndReturn(c -> c.twynProxyBuilder = new TwynProxyClassBuilder());
		}

		@Override
		public Configurer withFullCaching() {
			return setAndReturn(c -> c.cacheSupplier = () -> new Cache.Full());
		}

		@Override
		public Configurer withFullConcurrentCaching() {
			return setAndReturn(c -> c.cacheSupplier = () -> new Cache.FullConcurrent());
		}

		@Override
		public Configurer withNoCaching() {
			return setAndReturn(c -> c.cacheSupplier = () -> new Cache.None());
		}

		@Override
		public Configurer withPrecompiledClasses(Collection<Class<?>> types) {
			return setAndReturn(c -> c.precompiledTypes = new HashSet<Class<?>>(types));
		}

		@Override
		public Configurer withDebugMode() {
			return setAndReturn(c -> c.debug = true);
		}

		@Override
		public Twyn configure() {
			return new Twyn(new TwynContext(objectMapper, twynProxyBuilder, cacheSupplier, debug)
				.precompile(precompiledTypes));
		}

		private ConfigurerImpl setAndReturn(Consumer<ConfigurerImpl> c) {
			c.accept(this);
			return this;
		}
	}

	public static interface SelectMethod {
		/**
		 * Faster fi-rst time parsing. Slower afterwards. Suitable for test and in some cases production.
		 */
		Configurer withJavaProxies();

		/**
		 * Slower first time parsing. More performant afterwards. Mostly suitable for production code.
		 */
		ClassGenerationConfigurer withClassGeneration();
	}

	public static interface Configurer {
		Configurer withObjectMapper(ObjectMapper objectMapper);
		/**
		 * Return values from all calls are cached. Not thread safe
		 */
		Configurer withFullCaching();
		/**
		 * Return values from all calls are cached. Thread safe
		 */
		Configurer withFullConcurrentCaching();
		/**
		 * Default
		 */
		Configurer withNoCaching();
		/**
		 * toString of nodes includes underlying json node, currently only supported with JavaProxies
		 */
		Configurer withDebugMode();
		Twyn configure();
	}

	public static interface ClassGenerationConfigurer extends Configurer {
		Configurer withPrecompiledClasses(Collection<Class<?>> types);
	}

}
