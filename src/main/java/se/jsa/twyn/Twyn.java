package se.jsa.twyn;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
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
	
	public <T> T read(InputStream inputStream, Class<T> type) throws JsonProcessingException, IOException {
		return type.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), 
				new Class<?>[] {type},
				TwynInvocationHandler.create(inputStream, this)));
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
