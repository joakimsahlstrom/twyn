package se.jsa.twyn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Objects;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

public class Twyn {
	private final Constructor<MethodHandles.Lookup> methodHandleLookupConstructor;
	private final ObjectMapper objectMapper;
	
	public Twyn() {
		this(new ObjectMapper());
	}
	
	public Twyn(ObjectMapper objectMapper) {
		this.objectMapper = Objects.requireNonNull(objectMapper);
		
		try {
			methodHandleLookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
			if (!methodHandleLookupConstructor.isAccessible()) {
				methodHandleLookupConstructor.setAccessible(true);
			}
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Unexpected internal error!");
		}
	}
	
	public <T> T read(InputStream inputStream, Class<T> type) throws JsonProcessingException {
		return read(() -> objectMapper.readTree(inputStream), type);
	}
	
	public <T> T read(byte[] data, Class<T> type) throws JsonProcessingException {
		return read(() -> objectMapper.readTree(data), type);
	}
	
	public <T> T read(File file, Class<T> type) throws JsonProcessingException {
		return read(() -> objectMapper.readTree(file), type);
	}
	
	public <T> T read(JsonParser parser, Class<T> type) throws JsonProcessingException {
		return read(() -> objectMapper.readTree(parser), type);
	}
	
	public <T> T read(Reader reader, Class<T> type) throws JsonProcessingException {
		return read(() -> objectMapper.readTree(reader), type);
	}
	
	public <T> T read(String string, Class<T> type) throws JsonProcessingException {
		return read(() -> objectMapper.readTree(string), type);
	}
	
	public <T> T read(URL url, Class<T> type) throws JsonProcessingException {
		return read(() -> objectMapper.readTree(url), type);
	}
	
	public <T> T read(JsonParser parser, DeserializationConfig deserializationConfig, Class<T> type) throws JsonProcessingException {
		return read(() -> objectMapper.readTree(parser, deserializationConfig), type);
	}
	
	@FunctionalInterface
	/*package*/ interface JsonProducer {
		JsonNode get() throws Exception;
	}
	
	private <T> T read(JsonProducer jsonProducer, Class<T> type) throws JsonProcessingException {
		try {
			return type.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), 
					new Class<?>[] {type},
					TwynInvocationHandler.create(jsonProducer, this)));
		} catch (JsonProcessingException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Could not read input!", e);
		}
	}
	
	Lookup lookup(Object declaringClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return methodHandleLookupConstructor.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE);
	}
	
	<T> T proxy(Class<T> type, JsonNode tree) {
		return type.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), 
				new Class<?>[] {type},
				new TwynInvocationHandler(tree, this)));
	}

	Object readValue(JsonNode resolveTargetNode, Class<?> valueType) throws JsonParseException, JsonMappingException, IOException {
		return objectMapper.readValue(resolveTargetNode, valueType);
	}
	
}
