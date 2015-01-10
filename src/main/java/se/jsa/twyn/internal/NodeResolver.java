package se.jsa.twyn.internal;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.codehaus.jackson.JsonNode;

import se.jsa.twyn.TwynIndex;


interface NodeResolver  {

	JsonNode resolveNode(Method method, JsonNode root);

	static NodeResolver getResolver(Class<?> implementedType, JsonNode jsonNode) {
		if (jsonNode.isArray()) {
			return new ArrayInvocationHandlerMethodResolver(implementedType);
		} else {
			return new MethodNameInvocationHandlerMethodResolver();
		}
	}

	static class MethodNameInvocationHandlerMethodResolver implements NodeResolver {
		@Override
		public JsonNode resolveNode(Method method, JsonNode root) {
			return root.get(TwynUtil.decodeJavaBeanGetName(method.getName()));
		}
	}

	static class ArrayInvocationHandlerMethodResolver implements NodeResolver {
		private final Map<String, Integer> fieldOrder = new HashMap<>();
		public ArrayInvocationHandlerMethodResolver(Class<?> implementedType) {
			Stream.of(implementedType.getMethods())
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
		public JsonNode resolveNode(Method method, JsonNode root) {
			return root.get(fieldOrder.get(TwynUtil.decodeJavaBeanGetName(method.getName())));
		}
	}

}
