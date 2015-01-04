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
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public <T> T buildProxy(Class<T> type, TwynContext twyn, JsonNode jsonNode) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException, URISyntaxException {
	    Class<?> typeImpl = getImplementingClass(type);
	    return type.cast(build(typeImpl, twyn, jsonNode));
	}

	private Class<?> getImplementingClass(Class<?> type) throws IOException, URISyntaxException, ClassNotFoundException {
		if (!implementations.containsKey(type)) {
			implementations.put(type, createClass(type));
		}
		return implementations.get(type);
	}

	private Class<?> createClass(Class<?> type) throws IOException, URISyntaxException, ClassNotFoundException {
		String typeImplementationName = type.getSimpleName() + "TwynImpl";
		JavaSourceCompiler.CompilationUnit compilationUnit = javaSourceCompiler.createCompilationUnit();
		TwynProxyJavaFile twynProxyClassFile = TwynProxyJavaFile.create(typeImplementationName, type, templates);
	    
	    compilationUnit.addJavaSource(twynProxyClassFile.getClassName(), twynProxyClassFile.getCode());
	    ClassLoader classLoader = javaSourceCompiler.compile(compilationUnit);
	    return classLoader.loadClass(twynProxyClassFile.getClassName());
	}
	
	private Object build(Class<?> fooClass, TwynContext twyn, JsonNode jsonNode) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return fooClass.getConstructor(TwynContext.class, JsonNode.class).newInstance(twyn, jsonNode);
	}

	@Override
	public String toString() {
		return "TwynProxyClassBuilder []";
	}
	
}
