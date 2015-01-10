package se.jsa.twyn.internal;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import se.jsa.twyn.TwynCollection;

public enum MethodType implements Predicate<Method> {
	ILLEGAL_NONDEFAULT_METHOD_MORE_THAN_ONE_ARGUMENT(m -> !m.isDefault() && m.getParameters().length > 1),
	ILLEGAL_COLLECTION_NO_ANNOTATION(m -> ClassType.isCollection(m.getReturnType()) && m.getParameters().length == 0 && m.getAnnotation(TwynCollection.class) == null ),

	DEFAULT(m 	-> m.isDefault()),
	ARRAY(m 	-> m.getReturnType().isArray() 			&& m.getParameters().length == 0 && m.getReturnType().getComponentType().isInterface()),
	LIST(m 		-> m.getReturnType().equals(List.class) && m.getParameters().length == 0 && m.getAnnotation(TwynCollection.class) != null),
	MAP(m 		-> m.getReturnType().equals(Map.class) 	&& m.getParameters().length == 0 && m.getAnnotation(TwynCollection.class) != null),
	SET(m 		-> m.getReturnType().equals(Set.class) 	&& m.getParameters().length == 0 && m.getAnnotation(TwynCollection.class) != null),
	INTERFACE(m -> m.getReturnType().isInterface() 		&& m.getParameters().length == 0),

	SET_VALUE(m -> m.getParameters().length == 1),

	VALUE(m -> true);

	private Predicate<Method> predicate;

	private MethodType(Predicate<Method> predicate) {
		this.predicate = Objects.requireNonNull(predicate);
	}

	@Override
	public boolean test(Method m) {
		return predicate.test(m);
	}

	public static MethodType getType(Method m) {
		return Arrays.asList(values()).stream()
				.filter(mt -> mt.test(m))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Could not determine MethodType for " + m));
	}

	public static Predicate<Method> GETTER_TYPES_FILTER = any(ARRAY, LIST, MAP, SET, INTERFACE, VALUE);
	public static Predicate<Method> ILLEGAL_TYPES_FILTER = any(ILLEGAL_NONDEFAULT_METHOD_MORE_THAN_ONE_ARGUMENT, ILLEGAL_COLLECTION_NO_ANNOTATION);

	@SafeVarargs
	private static <T> Predicate<T> any(Predicate<T>... predicates) {
		return Stream.of(predicates).reduce(Predicate::or).orElse(p -> false);
	}

}