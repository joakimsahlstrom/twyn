package se.jsa.twyn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Objects;
import java.util.function.Supplier;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import se.jsa.twyn.internal.Cache;
import se.jsa.twyn.internal.MethodType;
import se.jsa.twyn.internal.Methods;
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
		} catch (IOException | IllegalArgumentException e) { // also handles JsonProcessingException
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Could not read input!", e);
		}
	}

	private <T> Class<T> validate(Class<T> type) {
		Methods.stream(type)
			.filter(MethodType.ILLEGAL)
			.findAny()
			.ifPresent(m -> { throw new IllegalArgumentException("Type " + type + " defines method " + m + " which is nondefault and has method arguments. Proxy cannot be created."); });
		return type;
	}

	public static Twyn forTest() {
		return configurer().withJavaProxies().configure();
	}

	// Builder

	public static SelectMethod configurer() {
		return new BuilderImpl();
	}

	private static class BuilderImpl implements SelectMethod, Configurer {
		private ObjectMapper objectMapper = new ObjectMapper();
		private Supplier<Cache> cacheSupplier = () -> new Cache.None();
		private TwynProxyBuilder twynProxyBuilder;

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
		public Configurer withClassGeneration() {
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
		public Twyn configure() {
			return new Twyn(new TwynContext(objectMapper, twynProxyBuilder, cacheSupplier));
		}

	}
	public static interface SelectMethod {
		/**
		 * Faster first time parsing. Slower afterwards. Suitable for test and in some cases production.
		 */
		Configurer withJavaProxies();

		/**
		 * Slower first time parsing. More performant afterwards. Mostly suitable for production code.
		 */
		Configurer withClassGeneration();
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
		Twyn configure();
	}

}
