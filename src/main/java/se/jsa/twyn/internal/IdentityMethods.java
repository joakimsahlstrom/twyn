package se.jsa.twyn.internal;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import se.jsa.twyn.TwynId;

class IdentityMethods {
	private final Map<Class<?>, List<Method>> methods = new ConcurrentHashMap<Class<?>, List<Method>>();

	public Stream<Method> getIdentifyMethods(Class<?> implementedType) {
		return methods.computeIfAbsent(implementedType, t -> IdentityMethods.get(t)).stream();
	}

	private static List<Method> get(Class<?> implementedType) {
		List<Method> methods = Methods.stream(implementedType).parallel()
				.filter(m -> !MethodType.DEFAULT.test(m) && m.getParameters().length == 0)
				.collect(Collectors.toList());
		List<Method> idAnnotatedMethods = methods.stream().filter((m) -> { return m.getAnnotation(TwynId.class) != null; }).collect(Collectors.toList());
		return (idAnnotatedMethods.size() > 0) ? idAnnotatedMethods : methods;
	}
}
