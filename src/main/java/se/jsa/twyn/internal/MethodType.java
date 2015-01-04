package se.jsa.twyn.internal;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import se.jsa.twyn.TwynCollection;

enum MethodType implements Predicate<Method> {
	DEFAULT(m -> m.isDefault()),
	ARRAY(m -> m.getReturnType().isArray() && m.getReturnType().getComponentType().isInterface()),
	LIST(m -> m.getReturnType().equals(List.class) && m.getAnnotation(TwynCollection.class) != null),
	MAP(m -> m.getReturnType().equals(Map.class) && m.getAnnotation(TwynCollection.class) != null),
	INTERFACE(m -> m.getReturnType().isInterface()),
	VALUE(m -> true);

	private Predicate<Method> p;

	private MethodType(Predicate<Method> p) {
		this.p = Objects.requireNonNull(p);
	}

	@Override
	public boolean test(Method m) {
		return p.test(m);
	}

	public static MethodType getType(Method m) {
		return Arrays.asList(values()).stream()
				.filter(mt -> mt.test(m))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Could not determine MethodType for " + m));
	}
}