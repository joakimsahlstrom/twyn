package se.jsa.twyn.internal;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import se.jsa.twyn.TwynId;

class IdentityMethods {

	public static Stream<Method> get(Class<?> implementedType) {
		List<Method> methods = Arrays.asList(implementedType.getMethods()).stream().parallel()
				.filter(m -> !MethodType.DEFAULT.test(m) && m.getParameters().length == 0)
				.collect(Collectors.toList());
		List<Method> idAnnotatedMethods = methods.stream().filter((m) -> { return m.getAnnotation(TwynId.class) != null; }).collect(Collectors.toList());
		if (idAnnotatedMethods.size() > 0) {
			return idAnnotatedMethods.stream();
		} else {
	 		return methods.stream();
		}
	}

}
