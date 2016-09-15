/*
 * Copyright 2016 Joakim Sahlström
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
package se.jsa.twyn.internal;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;

import se.jsa.twyn.BadJsonNodeTypeException;
import se.jsa.twyn.NoSuchJsonNodeException;
import se.jsa.twyn.TwynCollection;
import se.jsa.twyn.TwynProxyException;
import se.jsa.twyn.internal.ProxiedInterface.ImplementedMethod;

public class ErrorFactory {

	public static Supplier<? extends RuntimeException> innerProxyNoStruct(Method method, JsonNode node) {
		return innerProxyNoStruct(
				method.getDeclaringClass().getSimpleName() + "." + method.getName(),
				method.getReturnType().getSimpleName(),
				node);
	}
	public static Supplier<? extends RuntimeException> innerProxyNoStruct(String methodName, String returnTypeName, JsonNode node) {
		return () -> new BadJsonNodeTypeException(
				"Did not find json structure matching type=" + returnTypeName
				+ " for method=" + methodName + "(). Bad json fragment=" + node);
	}

	public static Supplier<? extends RuntimeException> innerMapProxyNoMapStructure(Method method, JsonNode node) {
		TwynCollection annotation = method.getAnnotation(TwynCollection.class);
		return innerMapProxyNoMapStructure(
				method.getDeclaringClass().getSimpleName() + "." + method.getName(),
				"Map<" + Optional.ofNullable(annotation.keyType().getSimpleName()).orElse("String") + ", " + annotation.value().getSimpleName() +">",
				node);
	}
	public static Supplier<? extends RuntimeException> innerMapProxyNoMapStructure(String methodName, String returnTypeName, JsonNode node) {
		return () -> new BadJsonNodeTypeException(
				"Did not find json map structure when resolving type=" + returnTypeName
				+ " for method=" + methodName + "(). Bad json fragment=" + node);
	}

	public static Supplier<? extends RuntimeException> proxyCollectionJsonNotArrayType(Class<?> componentType, JsonNode node) {
		return proxyCollectionJsonNotArrayType(componentType.getSimpleName(), node);
	}
	public static Supplier<? extends RuntimeException> proxyCollectionJsonNotArrayType(String componentTypeName, JsonNode node) {
		return () -> new BadJsonNodeTypeException(
				"Did not find collection of " + componentTypeName
				+ " where expected. Bad json fragment=" + node);
	}

	public static Supplier<? extends RuntimeException> couldNotResolveTargetNode(Method method, JsonNode node) {
		return couldNotResolveTargetNode(
				method.getDeclaringClass().getSimpleName() + "." + method.getName(),
				method.getReturnType().getSimpleName(),
				node);
	}
	public static Supplier<? extends RuntimeException> couldNotResolveTargetNode(String methodName, String returnTypeName, JsonNode node) {
		return () -> new NoSuchJsonNodeException(
				"Could not resolve json node when resolving type=" + returnTypeName
				+ " for method=" + methodName + "(). Bad json fragment=" + node);
	}

	public static Supplier<? extends RuntimeException> proxyValidationError(ProxiedInterface type, ImplementedMethod m) {
		switch (MethodType.getType(m)) {
		case ILLEGAL_NONDEFAULT_METHOD_MORE_THAN_ONE_ARGUMENT:
			return () -> new IllegalArgumentException("Type " + type + " defines method " + m.getName() + " which is nondefault and has method arguments. Proxy cannot be created.");
		case ILLEGAL_COLLECTION_NO_ANNOTATION:
			return () -> new IllegalArgumentException("Collection method " + m.getName() + " on type " + type.getCanonicalName() + " is missing annotation " + TwynCollection.class.getSimpleName());
		default:
			return () -> new TwynProxyException("Error message not supported for " + MethodType.class.getSimpleName() + " " + MethodType.getType(m));
		}
	}

}
