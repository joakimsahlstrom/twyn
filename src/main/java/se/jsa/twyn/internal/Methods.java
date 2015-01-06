package se.jsa.twyn.internal;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

public class Methods {
	public static Stream<Method> stream(Class<?> type) {
		return Arrays.asList(type.getMethods()).stream();
	}
}
