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
package se.jsa.twyn.internal.read;

import java.util.Collection;

import javax.lang.model.element.TypeElement;

import se.jsa.twyn.internal.read.element.ProxiedInterfaceTypeElement;
import se.jsa.twyn.internal.read.reflect.ProxiedInterfaceClass;

public interface ProxiedInterface {

	public static ProxiedInterface of(TypeElement typeElement) {
		return new ProxiedInterfaceTypeElement(typeElement);
	}

	public static ProxiedInterfaceClass of(Class<?> elementClass) {
		return new ProxiedInterfaceClass(elementClass);
	}

	String getCanonicalName();
	String getSimpleName();
	Collection<ImplementedMethod> getMethods();

	@Override int hashCode();
	@Override boolean equals(Object other);

}
