package se.jsa.twyn.internal;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codehaus.jackson.JsonNode;

import se.jsa.twyn.TwynIndex;


interface NodeResolver  {

	JsonNode resolveNode(Method method, JsonNode root);
	static Predicate<Method> TWYNINDEX_ANNOTATED = m -> m.getAnnotation(TwynIndex.class) != null;

	static NodeResolver getResolver(Class<?> implementedType) {
		if (isArrayType(implementedType)) {
			return new ArrayInvocationHandlerMethodResolver(implementedType);
		} else {
			return new MethodNameInvocationHandlerMethodResolver();
		}
	}
	static boolean isArrayType(Class<?> implementedType) {
		List<Method> getMethods = Stream.of(implementedType.getMethods())
			.filter(MethodType.GETTER_TYPES_FILTER)
			.collect(Collectors.toList());
		if (getMethods.stream().allMatch(TWYNINDEX_ANNOTATED)) {
			return true;
		} else if (getMethods.stream().noneMatch(TWYNINDEX_ANNOTATED)) {
			return false;
		} else {
			throw new IllegalArgumentException("Type " + implementedType.getCanonicalName() + " has some, but not all getter method annotated with @" + TwynIndex.class.getSimpleName() + ". "
					+ "Either all or none getter methods must be annotated with this annotation.");
		}
	}

	String resolveNodeId(Method method);

	static class MethodNameInvocationHandlerMethodResolver implements NodeResolver {
		@Override
		public JsonNode resolveNode(Method method, JsonNode root) {
			return root.get(TwynUtil.decodeJavaBeanGetName(method.getName()));
		}
		@Override
		public String resolveNodeId(Method method) {
			return "\"" + TwynUtil.decodeJavaBeanGetName(method.getName()) + "\"";
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
		@Override
		public String resolveNodeId(Method method) {
			return fieldOrder.get(TwynUtil.decodeJavaBeanGetName(method.getName())).toString();
		}
	}


}
