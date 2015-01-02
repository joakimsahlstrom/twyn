package se.jsa.twyn;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URL;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

public class Twyn {

	private final Constructor<MethodHandles.Lookup> methodHandleLookupConstructor;
	
	public Twyn() {
		try {
			this.methodHandleLookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
			if (!methodHandleLookupConstructor.isAccessible()) {
				methodHandleLookupConstructor.setAccessible(true);
			}
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Unexpected internal error!");
		}
	}
	
	public <T> T read(InputStream inputStream, Class<T> type) throws JsonProcessingException {
		return read(m -> m.readTree(inputStream), type);
	}
	
	public <T> T read(byte[] data, Class<T> type) throws JsonProcessingException {
		return read(m -> m.readTree(data), type);
	}
	
	public <T> T read(File file, Class<T> type) throws JsonProcessingException {
		return read(m -> m.readTree(file), type);
	}
	
	public <T> T read(JsonParser parser, Class<T> type) throws JsonProcessingException {
		return read(m -> m.readTree(parser), type);
	}
	
	public <T> T read(Reader reader, Class<T> type) throws JsonProcessingException {
		return read(m -> m.readTree(reader), type);
	}
	
	public <T> T read(String string, Class<T> type) throws JsonProcessingException {
		return read(m -> m.readTree(string), type);
	}
	
	public <T> T read(URL url, Class<T> type) throws JsonProcessingException {
		return read(m -> m.readTree(url), type);
	}
	
	public <T> T read(JsonParser parser, DeserializationConfig deserializationConfig, Class<T> type) throws JsonProcessingException {
		return read(m -> m.readTree(parser, deserializationConfig), type);
	}
	
	@FunctionalInterface
	/*package*/interface ObjectMapperFacade {
		JsonNode apply(ObjectMapper mapper) throws Exception;
	}
	
	private <T> T read(ObjectMapperFacade reader, Class<T> type) throws JsonProcessingException {
		try {
			return type.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), 
					new Class<?>[] {type},
					TwynInvocationHandler.create(reader, this)));
		} catch (JsonProcessingException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Could not read input!", e);
		}
	}
	
	Lookup lookup(Object declaringClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return methodHandleLookupConstructor.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE);
	}
	
	<T> T proxy(Class<T> type, JsonNode tree, ObjectMapper objectMapper) {
		return type.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), 
				new Class<?>[] {type},
				new TwynInvocationHandler(tree, objectMapper, this)));
	}
	
}
