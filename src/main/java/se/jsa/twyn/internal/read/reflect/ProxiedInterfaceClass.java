/*
 * Copyright 2016 Joakim Sahlström
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
package se.jsa.twyn.internal.read.reflect;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import se.jsa.twyn.internal.read.ImplementedMethod;
import se.jsa.twyn.internal.read.ProxiedInterface;

public class ProxiedInterfaceClass implements ProxiedInterface {
	private final Class<?> type;

	public ProxiedInterfaceClass(Class<?> type) {
		this.type = type;
	}

	@Override
	public String getCanonicalName() {
		return type.getCanonicalName();
	}

	@Override
	public String getSimpleName() {
		return type.getSimpleName();
	}

	@Override
	public Collection<ImplementedMethod> getMethods() {
		return Stream.of(type.getMethods())
				.map(m -> new ImplementedMethodMethod(m))
				.collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ProxiedInterfaceClass) {
			return type.equals(((ProxiedInterfaceClass) obj).type);
		} else {
			return super.equals(obj);
		}
	}

	@Override
	public int hashCode() {
		return type.hashCode();
	}

	public boolean isAssignableFrom(Class<? extends Object> otherClass) {
		return type.isAssignableFrom(otherClass);
	}

	public Class<?> getProxiedType() {
		return type;
	}
}