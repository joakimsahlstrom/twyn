package se.jsa.twyn.internal;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.abstractmeta.toolbox.compilation.compiler.JavaSourceCompiler;
import org.abstractmeta.toolbox.compilation.compiler.impl.JavaSourceCompilerImpl;
import org.codehaus.jackson.JsonNode;

public class TwynProxyClassBuilder implements TwynProxyBuilder {
	
	private JavaSourceCompiler javaSourceCompiler = new JavaSourceCompilerImpl();
	private Map<Class<?>, Class<?>> implementations = new ConcurrentHashMap<Class<?>, Class<?>>(); 
	private TwynProxyClassTemplates templates;
	
	public TwynProxyClassBuilder() {
		try {
			templates = TwynProxyClassTemplates.create();
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException("Internal error, could not read code template files.", e);
		}
	}
	
	@Override
	public <T> T buildProxy(Class<T> type, TwynContext twyn, JsonNode jsonNode) {
	    return type.cast(instantiate(getImplementingClass(type), twyn, jsonNode));
	}

	private Class<?> getImplementingClass(Class<?> type) {
		return implementations.computeIfAbsent(type, t -> createClass(t));
	}

	private Class<?> createClass(Class<?> type) {
		try {
			TwynProxyJavaFile twynProxyJavaFile = TwynProxyJavaFile.create(type, templates);
			return javaSourceCompiler
					.compile(twynProxyJavaFile.setupCompilationUnit(javaSourceCompiler))
					.loadClass(twynProxyJavaFile.getClassName());
		} catch (ClassNotFoundException | IOException | URISyntaxException e) {
			throw new RuntimeException("Could not create class for " + type.getSimpleName(), e);
		}
	}

	private Object instantiate(Class<?> typeImpl, TwynContext twyn, JsonNode jsonNode) {
		try {
			return typeImpl.getConstructor(TwynContext.class, JsonNode.class).newInstance(twyn, jsonNode);
		} catch (InstantiationException 
				| IllegalAccessException
				| IllegalArgumentException 
				| InvocationTargetException
				| NoSuchMethodException 
				| SecurityException e) {
			throw new RuntimeException("Could not instantiate class " + typeImpl.getSimpleName(), e);
		}
	}

	@Override
	public String toString() {
		return "TwynProxyClassBuilder []";
	}
	
}
