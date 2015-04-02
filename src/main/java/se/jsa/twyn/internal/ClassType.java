package se.jsa.twyn.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class ClassType {
	static final Collection<Class<?>> COLLECTION_TYPES = new HashSet<>(Arrays.asList(List.class, Map.class, Set.class));
	static final Set<String> COLLECTION_TYPES_QUALIFIED_NAMES = COLLECTION_TYPES.stream().map(c -> c.getName()).collect(Collectors.toSet());

	public static boolean isCollection(Class<?> type) {
		return COLLECTION_TYPES.contains(type);
	}

	public static boolean isCollectionQualifiedName(String name) {
		return COLLECTION_TYPES_QUALIFIED_NAMES.contains(name);
	}
}
