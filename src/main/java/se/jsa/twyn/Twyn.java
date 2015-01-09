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
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import se.jsa.twyn.internal.Cache;
import se.jsa.twyn.internal.JsonNodeHolder;
import se.jsa.twyn.internal.MethodType;
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

	public <T> T read(JsonParser parser, DeserializationConfig deserializationConfig, Class<T> type) throws JsonProcessingException, IOException {
		return read(() -> twynContext.getObjectMapper().readTree(parser, deserializationConfig), type);
	}

	@FunctionalInterface
	private interface JsonProducer {
		JsonNode get() throws Exception;
	}

	private <T> T read(JsonProducer jsonProducer, Class<T> type) throws JsonProcessingException, IOException {
		try {
			return twynContext.proxy(jsonProducer.get(), validate(type));
		} catch (IOException | RuntimeException e) { // also handles JsonProcessingException
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Could not read input!", e);
		}
	}

	private <T> Class<T> validate(Class<T> type) {
		Stream.of(type.getMethods())
			.filter(MethodType.ILLEGAL)
			.findAny()
			.ifPresent(m -> { throw new IllegalArgumentException("Type " + type + " defines method " + m + " which is nondefault and has method arguments. Proxy cannot be created."); });
		return type;
	}

	public JsonNode getJsonNode(Object obj) {
		if (obj instanceof JsonNodeHolder) {
			return ((JsonNodeHolder) obj).getJsonNode();
		}
		try {
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(obj);
			JsonNodeHolder twynProxyInvocationHandler = ((JsonNodeHolder)invocationHandler);
			return twynProxyInvocationHandler.getJsonNode();
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
		private ObjectMapper objectMapper = new ObjectMapper();
		private Supplier<Cache> cacheSupplier = () -> new Cache.None();
		private TwynProxyBuilder twynProxyBuilder;
		private Set<Class<?>> precompiledTypes = Collections.<Class<?>>emptySet();
		boolean debug = false;

		@Override
		public Configurer withObjectMapper(ObjectMapper objectMapper) {
			this.objectMapper = Objects.requireNonNull(objectMapper);
			return this;
		}

		@Override
		public Configurer withJavaProxies() {
			this.twynProxyBuilder = new TwynProxyInvocationHandlerBuilder();
			return this;
		}

		@Override
		public ClassGenerationConfigurer withClassGeneration() {
			this.twynProxyBuilder = new TwynProxyClassBuilder();
			return this;
		}

		@Override
		public Configurer withFullCaching() {
			cacheSupplier = () -> new Cache.Full();
			return this;
		}

		@Override
		public Configurer withFullConcurrentCaching() {
			cacheSupplier = () -> new Cache.FullConcurrent();
			return this;
		}

		@Override
		public Configurer withNoCaching() {
			cacheSupplier = () -> new Cache.None();
			return this;
		}

		@Override
		public Configurer withPrecompiledClasses(Collection<Class<?>> types) {
			this.precompiledTypes = new HashSet<Class<?>>(types);
			return this;
		}

		@Override
		public Configurer withDebugMode() {
			this.debug = true;
			return this;
		}

		@Override
		public Twyn configure() {
			return new Twyn(new TwynContext(objectMapper, twynProxyBuilder, cacheSupplier, debug)
				.precompile(precompiledTypes));
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
