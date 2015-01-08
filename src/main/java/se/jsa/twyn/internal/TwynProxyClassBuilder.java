package se.jsa.twyn.internal;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.abstractmeta.toolbox.compilation.compiler.JavaSourceCompiler;
import org.abstractmeta.toolbox.compilation.compiler.impl.JavaSourceCompilerImpl;
import org.codehaus.jackson.JsonNode;

public class TwynProxyClassBuilder implements TwynProxyBuilder {

	private final JavaSourceCompiler javaSourceCompiler = new JavaSourceCompilerImpl();
	private final Map<Class<?>, Class<?>> implementations = new ConcurrentHashMap<Class<?>, Class<?>>();
	private final TwynProxyClassTemplates templates;

	public TwynProxyClassBuilder() {
		try {
			templates = TwynProxyClassTemplates.create();
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException("Internal error, could not read code template files.", e);
		}
	}

	public void precompile(Collection<Class<?>> types, TwynContext twyn) {
		types.stream().forEach(((t) -> getImplementingClass(t, twyn)));
	}

	@Override
	public <T> T buildProxy(Class<T> type, TwynContext twyn, JsonNode jsonNode) {
	    return type.cast(instantiate(getImplementingClass(type, twyn), twyn, jsonNode));
	}

	private Class<?> getImplementingClass(Class<?> type, TwynContext twyn) {
		return implementations.computeIfAbsent(type, t -> createClass(t, twyn));
	}

	private Class<?> createClass(Class<?> type, TwynContext twynContext) {
		TwynProxyJavaFile twynProxyJavaFile = null;
		try {
			twynProxyJavaFile = TwynProxyJavaFile.create(type, templates, twynContext);
			return javaSourceCompiler
					.compile(twynProxyJavaFile.setupCompilationUnit(javaSourceCompiler))
					.loadClass(twynProxyJavaFile.getCanonicalClassName());
		} catch (IllegalStateException e) {
			throw new RuntimeException("Could not create class for " + type.getSimpleName() + ". Source:\n" + twynProxyJavaFile != null ? twynProxyJavaFile.getCode() : "n/a", e);
		} catch (ClassNotFoundException | IOException | URISyntaxException e) {
			throw new RuntimeException("Could not create class for " + type.getSimpleName() + ".", e);
		}
	}

	private Object instantiate(Class<?> typeImpl, TwynContext twyn, JsonNode jsonNode) {
		try {
			return typeImpl.getConstructor(TwynContext.class, JsonNode.class).newInstance(twyn, jsonNode);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Could not instantiate class " + typeImpl.getSimpleName(), e);
		}
	}

	@Override
	public String toString() {
		return "TwynProxyClassBuilder []";
	}

}