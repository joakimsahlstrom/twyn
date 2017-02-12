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
package se.jsa.twyn.internal.readmodel.ap;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import se.jsa.twyn.internal.readmodel.ImplementedMethod;
import se.jsa.twyn.internal.readmodel.ProxiedInterface;


public class ProxiedInterfaceTypeElement implements ProxiedInterface {
	private final TypeElement typeElement;
	
	public ProxiedInterfaceTypeElement(TypeElement typeElement) {
		this.typeElement = typeElement;
	}

	@Override
	public String getCanonicalName() {
		return typeElement.getQualifiedName().toString();
	}

	@Override
	public String getSimpleName() {
		return typeElement.getSimpleName().toString();
	}

	@Override
	public Collection<ImplementedMethod> getMethods() {
		return typeElement.getEnclosedElements().stream()
			.filter(e -> e instanceof ExecutableElement)
			.map(e -> new ImplementedMethodExecutableElement(ExecutableElement.class.cast(e)))
			.collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ProxiedInterfaceTypeElement) {
			return typeElement.equals(((ProxiedInterfaceTypeElement) obj).typeElement);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return typeElement.hashCode();
	}
}