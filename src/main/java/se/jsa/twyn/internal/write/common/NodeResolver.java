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
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import se.jsa.twyn.Resolve;
import se.jsa.twyn.ArrayIndex;
import se.jsa.twyn.internal.MethodType;
import se.jsa.twyn.internal.read.ImplementedMethod;
import se.jsa.twyn.internal.read.ProxiedInterface;

public interface NodeResolver  {
	JsonNode resolveNode(ImplementedMethod method, JsonNode root);
	String resolveNodeId(ImplementedMethod method);
	String resolveSetNodeId(ImplementedMethod method);

	void setNode(ImplementedMethod method, JsonNode root, JsonNode value);

	static Predicate<ImplementedMethod> WITH_TWYNINDEX = m -> m.hasAnnotation(ArrayIndex.class);

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
					+ " has some, but not all getter method annotated with @" + ArrayIndex.class.getSimpleName() + ". "
					+ "Either all or none getter methods must be annotated with this annotation.");
		}
	}

	static class MethodNameInvocationHandlerMethodResolver implements NodeResolver {
		private static final BinaryOperator<JsonNode> NO_BINARY_OP = (n1, n2) -> {
			throw new RuntimeException("NO BINARY OP!");
		};

		@Override
		public JsonNode resolveNode(ImplementedMethod method, JsonNode root) {
			return Stream.of(readPath(method, TwynUtil::decodeJavaBeanGetName)).reduce(root, (n, p) -> n.get(p), NO_BINARY_OP);
		}

		@SuppressWarnings("deprecation")
		@Override
		public void setNode(ImplementedMethod method, JsonNode root, JsonNode value) {
			String[] path = readPath(method, TwynUtil::decodeJavaBeanSetName);
			((ObjectNode) Stream.of(path).limit(path.length - 1).reduce(root, (n, p) -> n.get(p), NO_BINARY_OP)).put(path[path.length - 1], value);
		}

		@Override
		public String resolveNodeId(ImplementedMethod method) {
			return "\"" + Optional.ofNullable(method.getAnnotation(Resolve.class))
					.map(Resolve::value)
					.orElseGet(() -> TwynUtil.decodeJavaBeanGetName(method.getName())) + "\"";
		}

		@Override
		public String resolveSetNodeId(ImplementedMethod method) {
			return "\"" + Optional.ofNullable(method.getAnnotation(Resolve.class))
					.map(Resolve::value)
					.orElseGet(() -> TwynUtil.decodeJavaBeanSetName(method.getName())) + "\"";
		}

		private String[] readPath(ImplementedMethod method, Function<String, String> nameDecoder) {
			return (String[]) Optional.ofNullable(method.getAnnotation(Resolve.class))
                            .map(Resolve::value)
                            .map((resolve) -> resolve.split("\\."))
                            .orElseGet(() -> new String[] { nameDecoder.apply(method.getName()) });
		}
	}

	static class ArrayInvocationHandlerMethodResolver implements NodeResolver {
		private final Map<String, Integer> fieldOrder = new HashMap<>();

		public ArrayInvocationHandlerMethodResolver(ProxiedInterface implementedType) {
			implementedType.getMethods().stream()
				.filter(MethodType.GETTER_TYPES_FILTER)
				.forEachOrdered(m -> fieldOrder.put(
					TwynUtil.decodeJavaBeanGetName(m.getName()),
					Optional.ofNullable(m.getAnnotation(ArrayIndex.class))
						.orElseThrow(() -> new IllegalArgumentException(
								"Attempts to map method '" + m.getName() + "' of type '" + implementedType + "'"
								+ " to an array without providing a @" + ArrayIndex.class.getSimpleName() + " annotation."))
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

		@Override
		public String resolveSetNodeId(ImplementedMethod method) {
			return fieldOrder.get(TwynUtil.decodeJavaBeanGetName(method.getName())).toString();
		}

		@Override
		public void setNode(ImplementedMethod method, JsonNode root, JsonNode value) {
			((ArrayNode) root).set(fieldOrder.get(TwynUtil.decodeJavaBeanGetName(method.getName())), value);
		}
	}

}