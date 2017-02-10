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
package se.jsa.twyn.internal.write.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import se.jsa.twyn.TwynIndex;
import se.jsa.twyn.internal.MethodType;
import se.jsa.twyn.internal.read.ImplementedMethod;
import se.jsa.twyn.internal.read.ProxiedInterface;

public interface NodeResolver  {

	JsonNode resolveNode(ImplementedMethod method, JsonNode root);
	static Predicate<ImplementedMethod> WITH_TWYNINDEX = m -> m.hasAnnotation(TwynIndex.class);

	static NodeResolver getResolver(ProxiedInterface implementedType) {
		return isArrayType(implementedType)
				? new ArrayInvocationHandlerMethodResolver(implementedType)
				: new MethodNameInvocationHandlerMethodResolver();
	}

	static boolean isArrayType(ProxiedInterface implementedType) {
		List<ImplementedMethod> getMethods = implementedType.getMethods().stream()
			.filter(MethodType.GETTER_TYPES_FILTER)
			.collect(Collectors.toList());
		if (getMethods.stream().allMatch(WITH_TWYNINDEX)) {
			return true;
		} else if (getMethods.stream().noneMatch(WITH_TWYNINDEX)) {
			return false;
		} else {
			throw new IllegalArgumentException("Type " + implementedType.getCanonicalName()
					+ " has some, but not all getter method annotated with @" + TwynIndex.class.getSimpleName() + ". "
					+ "Either all or none getter methods must be annotated with this annotation.");
		}
	}

	String resolveNodeId(ImplementedMethod method);

	static class MethodNameInvocationHandlerMethodResolver implements NodeResolver {
		@Override
		public JsonNode resolveNode(ImplementedMethod method, JsonNode root) {
			return root.get(TwynUtil.decodeJavaBeanGetName(method.getName()));
		}
		@Override
		public String resolveNodeId(ImplementedMethod method) {
			return "\"" + TwynUtil.decodeJavaBeanGetName(method.getName()) + "\"";
		}
	}

	static class ArrayInvocationHandlerMethodResolver implements NodeResolver {
		private final Map<String, Integer> fieldOrder = new HashMap<>();

		public ArrayInvocationHandlerMethodResolver(ProxiedInterface implementedType) {
			implementedType.getMethods().stream()
				.filter(MethodType.GETTER_TYPES_FILTER)
				.forEachOrdered(m -> fieldOrder.put(
					TwynUtil.decodeJavaBeanGetName(m.getName()),
					Optional.ofNullable(m.getAnnotation(TwynIndex.class))
						.orElseThrow(() -> new IllegalArgumentException(
								"Attempts to map method '" + m.getName() + "' of type '" + implementedType + "'"
								+ " to an array without providing a @" + TwynIndex.class.getSimpleName() + " annotation."))
						.value()));
		}

		@Override
		public JsonNode resolveNode(ImplementedMethod method, JsonNode root) {
			return root.get(fieldOrder.get(TwynUtil.decodeJavaBeanGetName(method.getName())));
		}
		@Override
		public String resolveNodeId(ImplementedMethod method) {
			return fieldOrder.get(TwynUtil.decodeJavaBeanGetName(method.getName())).toString();
		}
	}

}