/*
 * Copyright 2015 Joakim Sahlström
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.jsa.twyn.internal.read.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassType {
	static final Collection<Class<?>> COLLECTION_TYPES = new HashSet<>(Arrays.asList(List.class, Map.class, Set.class));
	static final Set<String> COLLECTION_TYPES_QUALIFIED_NAMES = COLLECTION_TYPES.stream().map(c -> c.getName()).collect(Collectors.toSet());

	public static boolean isCollection(Class<?> type) {
		return COLLECTION_TYPES.contains(type);
	}

	public static boolean isCollectionQualifiedName(String name) {
		return COLLECTION_TYPES_QUALIFIED_NAMES.contains(name);
	}
}
