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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import se.jsa.twyn.internal.read.ImplementedMethod;
import se.jsa.twyn.internal.read.ProxiedInterface;
import se.jsa.twyn.internal.read.common.ClassType;

public class ImplementedMethodMethod implements ImplementedMethod {
	private final Method m;

	public ImplementedMethodMethod(Method m) {
		this.m = m;
	}

	@Override
	public String getName() {
		return m.getName();
	}

	@Override
	public boolean isDefault() {
		return m.isDefault();
	}

	@Override
	public int getNumParameters() {
		return m.getParameterCount();
	}

	@Override
	public String getParameterTypeCanonicalName(int parameterIndex) {
		return m.getParameterTypes()[parameterIndex].getCanonicalName();
	}

	@Override
	public <T extends Annotation> boolean hasAnnotation(Class<T> annotationType) {
		return m.getAnnotation(annotationType) != null;
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		return m.getAnnotation(annotationType);
	}

	@Override
	public boolean returnsArray() {
		return m.getReturnType().isArray();
	}

	@Override
	public boolean returnsArrayOfInterface() {
		return m.getReturnType().getComponentType().isInterface();
	}

	@Override
	public boolean returnsInterface() {
		return m.getReturnType().isInterface();
	}

	@Override
	public boolean returns(Class<?> returnType) {
		return m.getReturnType().equals(returnType);
	}

	@Override
	public boolean returns(ProxiedInterface implementedType) {
		return m.getReturnType().equals(((ProxiedInterfaceClass) implementedType).getProxiedType());
	}

	@Override
	public String getReturnTypeCanonicalName() {
		return m.getReturnType().getCanonicalName();
	}

	@Override
	public String getReturnComponentTypeCanonicalName() {
		return m.getReturnType().getComponentType().getCanonicalName();
	}

	@Override
	public String getDeclaringClassSimpleName() {
		return m.getDeclaringClass().getSimpleName();
	}
	
	@Override
	public String getReturnTypeParameterTypeCanonicalName(int i) {
		ParameterizedType genericReturnType = (ParameterizedType) m.getGenericReturnType();
		return genericReturnType.getActualTypeArguments()[i].getTypeName();
	}

	public Method getMethod() {
		return m;
	}
}