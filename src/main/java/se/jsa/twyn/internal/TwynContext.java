package se.jsa.twyn.internal;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

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
			throw new RuntimeException("Unexpected internal error!");
		}
	}

	public Lookup lookup(Object declaringClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return methodHandleLookupConstructor.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE);
	}

	public <T> T proxy(JsonNode jsonNode, Class<T> type) {
		try {
			return proxyBuilder.buildProxy(type, this, jsonNode);
		} catch (Exception e) {
			throw new RuntimeException("Could not create Twyn proxy", e);
		}
	}

	public <T> T readValue(JsonNode resolveTargetNode, Class<T> valueType) throws JsonParseException, JsonMappingException, IOException {
		return objectMapper.readValue(resolveTargetNode, valueType);
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

	Stream<Method> getIdentityMethods(Class<?> implementedType) {
		return identityMethods.getIdentifyMethods(implementedType);
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
