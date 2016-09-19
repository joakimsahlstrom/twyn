/*
 * Copyright 2015 Joakim Sahlström
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.jsa.twyn.internal.write.cg;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.abstractmeta.toolbox.compilation.compiler.JavaSourceCompiler;
import org.abstractmeta.toolbox.compilation.compiler.impl.JavaSourceCompilerImpl;

import se.jsa.twyn.TwynProxyException;
import se.jsa.twyn.internal.TwynContext;
import se.jsa.twyn.internal.read.ProxiedInterface;
import se.jsa.twyn.internal.write.TwynProxyBuilder;

import com.fasterxml.jackson.databind.JsonNode;

public class TwynProxyClassBuilder implements TwynProxyBuilder {
	private static final Logger LOGGER = Logger.getLogger(TwynProxyClassBuilder.class.getName());

	private final JavaSourceCompiler javaSourceCompiler = new JavaSourceCompilerImpl();
	private final Map<Class<?>, Class<?>> implementations = new ConcurrentHashMap<Class<?>, Class<?>>();
	private final TwynProxyClassJavaTemplates templates;

	public TwynProxyClassBuilder() {
		try {
			templates = TwynProxyClassJavaTemplates.create();
		} catch (IOException | URISyntaxException e) {
			throw new TwynProxyException("Internal error, could not read code template files.", e);
		}
	}

	public void precompile(Collection<Class<?>> types, TwynContext twyn) {
		types.stream().parallel().forEach(((t) -> getImplementingClass(t, twyn)));
	}

	@Override
	public <T> T buildProxy(Class<T> type, TwynContext twyn, JsonNode jsonNode) {
	    return type.cast(instantiate(getImplementingClass(type, twyn), twyn, jsonNode));
	}

	private Class<?> getImplementingClass(Class<?> type, TwynContext twyn) {
		if (!implementations.containsKey(type)) {
			Class<?> createdClass = loadOrCreateClass(type, twyn);
			return implementations.computeIfAbsent(type, t -> createdClass);
		}
		return implementations.get(type);
	}

	private Class<?> loadOrCreateClass(Class<?> type, TwynContext twynContext) {
		try {
			String className = TwynProxyClassJavaFile.generateClassName(ProxiedInterface.of(type));
			Class<?> prebuiltClass = Thread.currentThread().getContextClassLoader().loadClass(className);
			LOGGER.log(Level.INFO, "Prebuilt class found for type " + type + "! prebuiltClass=" + prebuiltClass.getName());
			return prebuiltClass;
		} catch (ClassNotFoundException e1) {
			LOGGER.log(Level.FINEST, "No prebuilt class found for type " + type);
		}

		TwynProxyClassJavaFile twynProxyJavaFile = null;
		try {
			twynProxyJavaFile = TwynProxyClassJavaFile.create(ProxiedInterface.of(type), templates, twynContext.getIdentityMethod(), twynContext.isDebug());
			return javaSourceCompiler
					.compile(twynProxyJavaFile.setupCompilationUnit(javaSourceCompiler))
					.loadClass(twynProxyJavaFile.getCanonicalClassName());
		} catch (IllegalStateException e) {
			throw new TwynProxyException("Could not create class for " + type.getSimpleName() + ". Source:\n" + twynProxyJavaFile != null ? twynProxyJavaFile.getCode() : "n/a", e);
		} catch (ClassNotFoundException | IOException | URISyntaxException e) {
			throw new TwynProxyException("Could not create class for " + type.getSimpleName() + ".", e);
		}
	}

	private Object instantiate(Class<?> typeImpl, TwynContext twyn, JsonNode jsonNode) {
		try {
			return typeImpl.getConstructor(TwynContext.class, JsonNode.class).newInstance(twyn, jsonNode);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new TwynProxyException("Could not instantiate class " + typeImpl.getSimpleName(), e);
		}
	}

	@Override
	public String toString() {
		return "TwynProxyClassBuilder []";
	}

}