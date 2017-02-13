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
import java.lang.reflect.ParameterizedType;
import java.util.function.Supplier;

import se.jsa.twyn.BadNodeTypeException;
import se.jsa.twyn.NoSuchNodeException;
import se.jsa.twyn.TwynProxyException;
import se.jsa.twyn.internal.datamodel.Node;
import se.jsa.twyn.internal.readmodel.ImplementedMethod;
import se.jsa.twyn.internal.readmodel.ProxiedInterface;

public class ErrorFactory {

	public static Supplier<? extends RuntimeException> innerProxyNoStruct(Method method, Node node) {
		return innerProxyNoStruct(getName(method), getReturnTypeName(method), node);
	}
	public static Supplier<? extends RuntimeException> innerProxyNoStruct(String methodName, String returnTypeName, Node node) {
		return () -> new BadNodeTypeException(
				"Did not find node structure matching type=" + returnTypeName + " for method=" + methodName + "(). Bad node fragment=" + node);
	}

	public static Supplier<? extends RuntimeException> innerMapProxyNoMapStructure(Method method, Node node) {
		ParameterizedType genericReturnType = (ParameterizedType) method.getGenericReturnType();
		return innerMapProxyNoMapStructure(
				getName(method),
				"Map<" + genericReturnType.getActualTypeArguments()[0].getTypeName() + ", " + genericReturnType.getActualTypeArguments()[1].getTypeName() +">",
				node);
	}
	public static Supplier<? extends RuntimeException> innerMapProxyNoMapStructure(String methodName, String returnTypeName, Node node) {
		return () -> new BadNodeTypeException(
				"Did not find node map structure when resolving type=" + returnTypeName + " for method=" + methodName + "(). Bad node fragment=" + node);
	}

	public static Supplier<? extends RuntimeException> proxyArrayNodeNotCollectionType(Class<?> componentType, Method method, Node node) {
		return proxyArrayNodeNotCollectionType(getName(method), componentType.getSimpleName(), node);
	}
	public static Supplier<? extends RuntimeException> proxyArrayNodeNotCollectionType(String methodName, String componentTypeName, Node node) {
		return () -> new BadNodeTypeException(
				"Did not find array of " + componentTypeName + " for method=" + methodName + "(). Bad node fragment=" + node);
	}
	
	public static Supplier<? extends RuntimeException> proxyCollectionNotCollectionType(String componentTypeName, Method method, Node node) {
		return proxyCollectionNotCollectionType(getName(method), componentTypeName, node);
	}
	public static Supplier<? extends RuntimeException> proxyCollectionNotCollectionType(String methodName, String componentTypeName, Node node) {
		return () -> new BadNodeTypeException(
				"Did not find collection of " + componentTypeName + " for method=" + methodName + "(). Bad node fragment=" + node);
	}

	public static Supplier<? extends RuntimeException> couldNotResolveTargetNode(Method method, Node node) {
		return couldNotResolveTargetNode(getName(method), getReturnTypeName(method), node);
	}
	public static Supplier<? extends RuntimeException> couldNotResolveTargetNode(String methodName, String returnTypeName, Node node) {
		return () -> new NoSuchNodeException(
				"Could not resolve node node when resolving type=" + returnTypeName + " for method=" + methodName + "(). Bad node fragment=" + node);
	}

	public static Supplier<? extends RuntimeException> proxyValidationError(ProxiedInterface type, ImplementedMethod m) {
		switch (MethodType.getType(m)) {
			case ILLEGAL_OPTIONAL_WRAPS_ARRAY: 	return ErrorFactory.illegalOptionalWrap(m, "an array");
			case ILLEGAL_OPTIONAL_WRAPS_LIST: 	return ErrorFactory.illegalOptionalWrap(m, "a List");
			case ILLEGAL_OPTIONAL_WRAPS_SET: 	return ErrorFactory.illegalOptionalWrap(m, "a Set");
			case ILLEGAL_OPTIONAL_WRAPS_MAP: 	return ErrorFactory.illegalOptionalWrap(m, "a Map");

			case ILLEGAL_NONDEFAULT_METHOD_MORE_THAN_ONE_ARGUMENT:
				return () -> new IllegalArgumentException("Type " + type + " defines method " + m.getName() + " which is nondefault and has method arguments. Proxy cannot be created.");
			default:
				return () -> new TwynProxyException("Error message not supported for " + MethodType.class.getSimpleName() + " " + MethodType.getType(m));
		}
	}

	public static Supplier<? extends RuntimeException> illegalOptionalWrap(ImplementedMethod method, String wrappedType) {
		return () -> new TwynProxyException("Method " + method + " attempts to wrap " + wrappedType + " in an Optional which is not allowed! Collection types will always be empty if node node is missing, empty or null");
	}
	
	// Helper methods
	
	private static String getReturnTypeName(Method method) {
		return method.getReturnType().getSimpleName();
	}
	
	private static String getName(Method method) {
		return method.getDeclaringClass().getSimpleName() + "." + method.getName();
	}

}
