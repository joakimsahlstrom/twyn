package se.jsa.twyn;

import java.io.File;
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

import se.jsa.twyn.internal.CachePolicy;
import se.jsa.twyn.internal.CachePolicyFull;
import se.jsa.twyn.internal.CachePolicyNone;
import se.jsa.twyn.internal.TwynContext;
import se.jsa.twyn.internal.TwynProxyBuilder;
import se.jsa.twyn.internal.TwynProxyClassBuilder;
import se.jsa.twyn.internal.TwynProxyInvocationHandlerBuilder;

public class Twyn {

	private final TwynContext twynContext;

	private Twyn(TwynContext twynContext) {
		this.twynContext = Objects.requireNonNull(twynContext);
	}

	public <T> T read(InputStream inputStream, Class<T> type) throws JsonProcessingException {
		return read(() -> twynContext.getObjectMapper().readTree(inputStream), type);
	}

	public <T> T read(byte[] data, Class<T> type) throws JsonProcessingException {
		return read(() -> twynContext.getObjectMapper().readTree(data), type);
	}

	public <T> T read(File file, Class<T> type) throws JsonProcessingException {
		return read(() -> twynContext.getObjectMapper().readTree(file), type);
	}

	public <T> T read(JsonParser parser, Class<T> type) throws JsonProcessingException {
		return read(() -> twynContext.getObjectMapper().readTree(parser), type);
	}

	public <T> T read(Reader reader, Class<T> type) throws JsonProcessingException {
		return read(() -> twynContext.getObjectMapper().readTree(reader), type);
	}

	public <T> T read(String string, Class<T> type) throws JsonProcessingException {
		return read(() -> twynContext.getObjectMapper().readTree(string), type);
	}

	public <T> T read(URL url, Class<T> type) throws JsonProcessingException {
		return read(() -> twynContext.getObjectMapper().readTree(url), type);
	}

	public <T> T read(JsonParser parser, DeserializationConfig deserializationConfig, Class<T> type) throws JsonProcessingException {
		return read(() -> twynContext.getObjectMapper().readTree(parser, deserializationConfig), type);
	}

	@FunctionalInterface
	private interface JsonProducer {
		JsonNode get() throws Exception;
	}

	private <T> T read(JsonProducer jsonProducer, Class<T> type) throws JsonProcessingException {
		try {
			return twynContext.proxy(jsonProducer.get(), type);
		} catch (JsonProcessingException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Could not read input!", e);
		}
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
		private Supplier<CachePolicy> cachePolicySupplier = () -> new CachePolicyNone();
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
			cachePolicySupplier = () -> new CachePolicyFull();
			return this;
		}

		@Override
		public Configurer withNoCaching() {
			cachePolicySupplier = () -> new CachePolicyNone();
			return this;
		}

		@Override
		public Twyn configure() {
			return new Twyn(new TwynContext(objectMapper, twynProxyBuilder, cachePolicySupplier));
		}

	}
	public static interface SelectMethod {
		/**
		 * Faster first time parsing. Slower afterwards. Suitable for test and in some cases production.
		 * @return
		 */
		Configurer withJavaProxies();

		/**
		 * Slower first time parsing. More performant afterwards. Mostly suitable for production code.
		 * Creates classes.
		 */
		Configurer withClassGeneration();
	}
	public static interface Configurer {
		Configurer withObjectMapper(ObjectMapper objectMapper);
		/**
		 * Return values from all calls are cached
		 */
		Configurer withFullCaching();
		/**
		 * Default
		 */
		Configurer withNoCaching();
		Twyn configure();
	}

}
