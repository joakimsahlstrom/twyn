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
package se.jsa.twyn.internal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import se.jsa.twyn.IdField;
import se.jsa.twyn.internal.read.ImplementedMethod;
import se.jsa.twyn.internal.read.ProxiedInterface;

public class IdentityMethods {
	private final Map<ProxiedInterface, List<ImplementedMethod>> methods = new ConcurrentHashMap<ProxiedInterface, List<ImplementedMethod>>();

	public Stream<ImplementedMethod> getIdentityMethods(ProxiedInterface implementedType) {
		if (!methods.containsKey(implementedType)) {
			List<ImplementedMethod> implementedMethods = IdentityMethods.get(implementedType);
			return methods.computeIfAbsent(implementedType, t -> implementedMethods).stream();
		}
		return methods.get(implementedType).stream();
	}

	private static List<ImplementedMethod> get(ProxiedInterface implementedType) {
		List<ImplementedMethod> methods = implementedType.getMethods().stream()
				.filter(m -> !MethodType.DEFAULT.test(m) && m.getNumParameters() == 0)
				.collect(Collectors.toList());
		List<ImplementedMethod> idAnnotatedMethods = methods.stream().filter(m ->  m.hasAnnotation(IdField.class)).collect(Collectors.toList());
		return (idAnnotatedMethods.size() > 0) ? idAnnotatedMethods : methods;
	}
}
