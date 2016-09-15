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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;

import se.jsa.twyn.TwynCollection;
import se.jsa.twyn.TwynProxyException;

public interface ProxiedInterface {

	public static ProxiedInterface of(TypeElement typeElement) {
		return new ProxiedTypeElement(typeElement);
	}

	public static ProxiedElementClass of(Class<?> elementClass) {
		return new ProxiedElementClass(elementClass);
	}

	String getCanonicalName();
	String getSimpleName();
	Collection<ImplementedMethod> getMethods();

	@Override int hashCode();
	@Override boolean equals(Object other);

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
		boolean returnsCollection();
		boolean returnsInterface();
		boolean returns(Class<?> returnType);
		boolean returns(ProxiedInterface implementedType);

		String getReturnTypeCanonicalName();
		String getReturnComponentTypeCanonicalName();
		String getTwynCollectionTypeCanonicalName();
		String getDeclaringClassSimpleName();

	}

	class ProxiedTypeElement implements ProxiedInterface {
		private final TypeElement typeElement;

		public ProxiedTypeElement(TypeElement typeElement) {
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
			if (obj instanceof ProxiedTypeElement) {
				return typeElement.equals(((ProxiedTypeElement) obj).typeElement);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return typeElement.hashCode();
		}
	}

	class ImplementedMethodExecutableElement implements ImplementedMethod {
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
		public String getTwynCollectionTypeCanonicalName() {
			// Butt ugly but quick and easy way to get this data
			try {
				getAnnotation(TwynCollection.class).value();
				throw new TwynProxyException("Expected exception before this line!");
			} catch (MirroredTypeException mte) {
				return mte.getTypeMirror().toString();
			}
		}

		@Override
		public String getDeclaringClassSimpleName() {
			return executableElement.getEnclosingElement().getSimpleName().toString();
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

	class ProxiedElementClass implements ProxiedInterface {
		private final Class<?> type;

		public ProxiedElementClass(Class<?> type) {
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
			if (obj instanceof ProxiedElementClass) {
				return type.equals(((ProxiedElementClass) obj).type);
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
	}

	class ImplementedMethodMethod implements ImplementedMethod {
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
		public boolean returnsCollection() {
			return ClassType.isCollection(m.getReturnType());
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
			return m.getReturnType().equals(((ProxiedElementClass) implementedType).type);
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
		public String getTwynCollectionTypeCanonicalName() {
			return m.getAnnotation(TwynCollection.class).value().getCanonicalName();
		}

		@Override
		public String getDeclaringClassSimpleName() {
			return m.getDeclaringClass().getSimpleName();
		}

		public Method getMethod() {
			return m;
		}
	}

}
