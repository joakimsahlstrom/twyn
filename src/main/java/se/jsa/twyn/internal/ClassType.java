package se.jsa.twyn.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ClassType {
	static Collection<Class<?>> COLLECTIONS = new HashSet<Class<?>>(Arrays.asList(List.class, Map.class, Set.class));

	public static boolean isCollection(Class<?> type) {
		return COLLECTIONS.contains(type);
	}
}
