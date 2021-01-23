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

import com.fasterxml.jackson.databind.ObjectMapper;
import se.jsa.twyn.internal.*;
import se.jsa.twyn.internal.datamodel.CollectionNode;
import se.jsa.twyn.internal.datamodel.Node;
import se.jsa.twyn.internal.datamodel.NodeProducer;
import se.jsa.twyn.internal.datamodel.json.TwynJsonNodeProducer;
import se.jsa.twyn.internal.proxy.TwynProxyBuilder;
import se.jsa.twyn.internal.proxy.cg.TwynProxyClassBuilder;
import se.jsa.twyn.internal.proxy.reflect.TwynProxyInvocationHandlerBuilder;
import se.jsa.twyn.internal.readmodel.ProxiedInterface;
import se.jsa.twyn.internal.readmodel.reflect.ProxiedInterfaceClass;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Twyn {
	private final TwynContext twynContext;

	private Twyn(TwynContext twynContext) {
		this.twynContext = Objects.requireNonNull(twynContext);
	}

	public <T> T read(InputStream inputStream, Class<T> type) throws IOException {
		return read(twynContext.getNodeProducer().read(inputStream, type), type);
	}

	public <T> T read(byte[] data, Class<T> type) throws IOException {
		return read(twynContext.getNodeProducer().read(data, type), type);
	}

	public <T> T read(File file, Class<T> type) throws IOException {
		return read(twynContext.getNodeProducer().read(file, type), type);
	}

	public <T> T read(Reader reader, Class<T> type) throws IOException {
		return read(twynContext.getNodeProducer().read(reader, type), type);
	}

	public <T> T read(String string, Class<T> type) throws IOException {
		return read(twynContext.getNodeProducer().read(string, type), type);
	}

	public <T> T read(URL url, Class<T> type) throws IOException {
		return read(twynContext.getNodeProducer().read(url, type), type);
	}

	private <T> T read(Node node, Class<T> type) throws IOException {
		try {
			if (type.isArray()) {
				Class<?> componentType = type.getComponentType();
				Require.that(node.isCollection(), ErrorFactory.proxyArrayNodeNotCollectionType("ROOT", componentType.getSimpleName(), node));
				return type.cast(twynContext.proxyArray((CollectionNode) node, validate(componentType)));
			} else {
				return twynContext.proxy(node, validate(type)); 
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new TwynProxyException("Could not read input!", e);
		}
	}

	private <T> Class<T> validate(Class<T> type) {
		ProxiedInterfaceClass proxiedInterface = ProxiedInterface.of(type);
		proxiedInterface.getMethods().stream()
			.filter(MethodType.ILLEGAL_TYPES_FILTER)
			.findAny()
			.ifPresent(m -> { throw ErrorFactory.proxyValidationError(proxiedInterface, m).get(); });
		return type;
	}

	public Node getNode(Object obj) {
		if (obj instanceof NodeSupplier) {
			return ((NodeSupplier) obj).getNode();
		}
		try {
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(obj);
			NodeSupplier twynProxyInvocationHandler = ((NodeSupplier)invocationHandler);
			return twynProxyInvocationHandler.getNode();
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Not a twyn object!", e);
		}
	}

	public static Twyn forTest() {
		return configurer().withJavaProxies().configure();
	}

	// Builder

	public static SelectMethod configurer() {
		return new ConfigurerImpl();
	}

	private static class ConfigurerImpl implements SelectMethod, ClassGenerationConfigurer {
		private Supplier<Cache> cacheSupplier = () -> new Cache.None();
		private TwynProxyBuilder twynProxyBuilder;
		private Set<Class<?>> precompiledTypes = Collections.<Class<?>>emptySet();
		boolean debug = false;
		private NodeProducer nodeProducer = new TwynJsonNodeProducer(new ObjectMapper());

		@Override
		public Configurer withNodeProducer(NodeProducer nodeProducer) {
			return setAndReturn(c -> c.nodeProducer = Objects.requireNonNull(nodeProducer));
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
			return new Twyn(new TwynContext(nodeProducer, twynProxyBuilder, cacheSupplier, debug)
				.precompile(precompiledTypes));
		}

		private ConfigurerImpl setAndReturn(Consumer<ConfigurerImpl> c) {
			c.accept(this);
			return this;
		}
	}

	public interface SelectMethod {
		/**
		 * Faster fi-rst time parsing. Slower afterwards. Suitable for test and in some cases production.
		 */
		Configurer withJavaProxies();

		/**
		 * Slower first time parsing. More performant afterwards. Mostly suitable for production code.
		 */
		ClassGenerationConfigurer withClassGeneration();
	}

	public interface Configurer {
		Configurer withNodeProducer(NodeProducer nodeProducer);
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

	public interface ClassGenerationConfigurer extends Configurer {
		Configurer withPrecompiledClasses(Collection<Class<?>> types);
	}

}
