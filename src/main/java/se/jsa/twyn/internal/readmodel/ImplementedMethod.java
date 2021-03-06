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
package se.jsa.twyn.internal.readmodel;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import se.jsa.twyn.TwynProxyException;
import se.jsa.twyn.internal.readmodel.reflect.ImplementedMethodMethod;

public interface ImplementedMethod {

	public static ImplementedMethod of(Method method) {
		return new ImplementedMethodMethod(method);
	}

	String getName();
	boolean isDefault();
	int getNumParameters();
	String getParameterTypeCanonicalName(int parameterIndex);

	<T extends Annotation> boolean hasAnnotation(Class<T> annotationType);
	<T extends Annotation> T getAnnotation(Class<T> annotationType);

	boolean returnsArray();
	boolean returnsArrayOfInterface();
	boolean returnsInterface();
	boolean returns(Class<?> returnType);
	boolean returns(ProxiedInterface implementedType);

	String getReturnTypeCanonicalName();
	String getReturnComponentTypeCanonicalName();
	String getDeclaringClassSimpleName();

	String getReturnTypeParameterTypeCanonicalName(int i);
	
	default Class<?> getReturnTypeParameterType(int i) {
		try {
			return this.getClass().getClassLoader().loadClass(getReturnTypeParameterTypeCanonicalName(i));
		} catch (ClassNotFoundException e) {
			throw new TwynProxyException("Could not load expected return class=" + getReturnTypeParameterTypeCanonicalName(i), e);
		}
	}
}