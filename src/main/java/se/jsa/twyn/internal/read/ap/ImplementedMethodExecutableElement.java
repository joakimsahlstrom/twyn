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
package se.jsa.twyn.internal.read.ap;

import java.lang.annotation.Annotation;
import java.util.Optional;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;

import se.jsa.twyn.TwynProxyException;
import se.jsa.twyn.internal.read.ImplementedMethod;
import se.jsa.twyn.internal.read.ProxiedInterface;
import se.jsa.twyn.internal.read.common.ClassType;

public class ImplementedMethodExecutableElement implements ImplementedMethod {
	private final ExecutableElement executableElement;

	public ImplementedMethodExecutableElement(ExecutableElement executableElement) {
		this.executableElement = executableElement;
	}

	@Override
	public String getName() {
		return executableElement.getSimpleName().toString();
	}

	@Override
	public boolean isDefault() {
		return executableElement.getModifiers().contains(Modifier.DEFAULT);
	}

	@Override
	public int getNumParameters() {
		return executableElement.getParameters().size();
	}

	@Override
	public String getParameterTypeCanonicalName(int parameterIndex) {
		return getCanonicalName(executableElement.getParameters().get(parameterIndex).asType());
	}

	@Override
	public <T extends Annotation> boolean hasAnnotation(Class<T> annotationType) {
		return executableElement.getAnnotation(annotationType) != null;
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		return executableElement.getAnnotation(annotationType);
	}

	@Override
	public boolean returnsArray() {
		return executableElement.getReturnType() instanceof ArrayType;
	}

	@Override
	public boolean returnsArrayOfInterface() {
		return tryCast(ArrayType.class.cast(executableElement.getReturnType()).getComponentType(), DeclaredType.class)
			.map(dt -> dt.asElement().getKind() == ElementKind.INTERFACE)
			.orElse(false);
	}

	@Override
	public boolean returnsCollection() {
		TypeMirror returnType = executableElement.getReturnType();
		if (returnType instanceof DeclaredType) {
			TypeElement returnedElement = TypeElement.class.cast(DeclaredType.class.cast(returnType).asElement());
			return returnedElement.getInterfaces().stream()
				.filter(tm -> (tm instanceof DeclaredType))
				.map(tm -> TypeElement.class.cast(DeclaredType.class.cast(tm).asElement()).getQualifiedName().toString())
				.anyMatch(ClassType::isCollectionQualifiedName);
		}
		return false;
	}

	@Override
	public boolean returnsInterface() {
		return tryCast(executableElement.getReturnType(), DeclaredType.class)
			.map(dt -> dt.asElement().getKind() == ElementKind.INTERFACE)
			.orElse(false);
	}

	private <T> Optional<T> tryCast(Object o, Class<T> type) {
		return type.isAssignableFrom(o.getClass()) ? Optional.of(type.cast(o)) : Optional.empty();
	}

	@Override
	public boolean returns(Class<?> returnType) {
		if (executableElement.getReturnType() instanceof PrimitiveType) {
			PrimitiveType primitiveReturnType = (PrimitiveType) executableElement.getReturnType();
			return PrimitiveTypeMap.toPrimitive(primitiveReturnType).equals(returnType);
		} else {
			return getReturnTypeCanonicalName().equals(returnType.getCanonicalName());
		}
	}

	@Override
	public boolean returns(ProxiedInterface implementedType) {
		return getReturnTypeCanonicalName().equals(implementedType.getCanonicalName());
	}

	@Override
	public String getReturnTypeCanonicalName() {
		return getCanonicalName(executableElement.getReturnType());
	}

	@Override
	public String getReturnComponentTypeCanonicalName() {
		ArrayType returnArrayType = (ArrayType) executableElement.getReturnType();
		return getCanonicalName(returnArrayType.getComponentType());
	}

	@Override
	public String getDeclaringClassSimpleName() {
		return executableElement.getEnclosingElement().getSimpleName().toString();
	}
	
	@Override
	public String getReturnTypeParameterTypeCanonicalName(int i) {
		DeclaredType returnType = (DeclaredType) executableElement.getReturnType();
		return returnType.getTypeArguments().get(i).toString();
	}

	private String getCanonicalName(TypeMirror typeMirror) {
		if (typeMirror instanceof DeclaredType) {
			DeclaredType declaredReturnType = (DeclaredType) typeMirror;
			TypeElement returnElementType = (TypeElement) declaredReturnType.asElement();
			return returnElementType.getQualifiedName().toString();
		} else if (typeMirror instanceof PrimitiveType) {
			return PrimitiveTypeMap.toPrimitive((PrimitiveType)typeMirror).getName();
		} else {
			throw new TwynProxyException("Cannot determine canonical name of type: " + typeMirror);
		}
	}
}