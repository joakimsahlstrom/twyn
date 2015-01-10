package se.jsa.twyn.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class TwynUtil {

	// Getter helpers

	private static final Collection<String> GET_PREFIXES = Arrays.asList("get", "is");
	public static String decodeJavaBeanGetName(String name) {
		return resolve(name, GET_PREFIXES);
	}

	/// Setter helpers

	private static final Collection<String> SET_PREFIXES = Arrays.asList("set");
	public static String decodeJavaBeanSetName(String name) {
		return resolve(name, SET_PREFIXES);
	}

	// Other

	private static final Collection<String> ALL_PREFIXES = Stream.of(GET_PREFIXES, SET_PREFIXES).flatMap(Collection::stream).collect(Collectors.toList());
	public static String decodeJavaBeanName(String name) {
		return resolve(name, ALL_PREFIXES);
	}

	// private helper methods

	private static String resolve(String name, Collection<String> prefixes) {
		for (String prefix : prefixes) {
			int prefixLength = prefix.length();
			if (name.startsWith(prefix) && name.length() > prefixLength && Character.isUpperCase(name.charAt(prefixLength))) {
				return name.substring(prefixLength, prefixLength + 1).toLowerCase() + name.substring(prefixLength + 1);
			}
		}
		return name;
	}
}
