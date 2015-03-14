package se.jsa.twyn.internal;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public interface ProxiedInterface {

	String getSimpleName();
	Collection<ImplementedMethod> getMethods();

	interface ImplementedMethod {
		boolean isDefault();
		boolean returnsArray();
		int getNumParameters();
		boolean returnsCollection();
		boolean hasAnnotation(Class<?> annotationType);
	}

	class ProxiedTypeElement implements ProxiedInterface {
		private final TypeElement typeElement;

		public ProxiedTypeElement(TypeElement typeElement) {
			this.typeElement = typeElement;
		}

		@Override
		public String getSimpleName() {
			return typeElement.getSimpleName().toString();
		}

		@Override
		public Collection<ImplementedMethod> getMethods() {
			return typeElement.getEnclosedElements().stream()
				.filter(e -> e instanceof ExecutableElement)
				.map(e -> new ImplentedMethodExecutableElement(ExecutableElement.class.cast(e)))
				.collect(Collectors.toList());
		}
	}

	class ImplentedMethodExecutableElement implements ImplementedMethod {
		private static final Set<String> collectionQualifiedNames = new HashSet<>(Arrays.asList("java.util.List", "java.util.Set", "java.util.Map"));

		private final ExecutableElement executableElement;

		public ImplentedMethodExecutableElement(ExecutableElement executableElement) {
			this.executableElement = executableElement;
		}

		@Override
		public boolean isDefault() {
			return executableElement.getModifiers().contains(Modifier.DEFAULT);
		}

		@Override
		public boolean returnsArray() {
			return executableElement.getReturnType() instanceof ArrayType;
		}

		@Override
		public int getNumParameters() {
			return executableElement.getParameters().size();
		}

		@Override
		public boolean returnsCollection() {
			TypeMirror returnType = executableElement.getReturnType();
			if (returnType instanceof DeclaredType) {
				TypeElement returnedElement = TypeElement.class.cast(DeclaredType.class.cast(returnType).asElement());
				return returnedElement.getInterfaces().stream()
					.filter(tm -> (tm instanceof DeclaredType))
					.map(tm -> TypeElement.class.cast(DeclaredType.class.cast(tm).asElement()).getQualifiedName().toString())
					.anyMatch(n -> collectionQualifiedNames.contains(n));
			}
			return false;
		}

		@Override
		public boolean hasAnnotation(Class<?> annotationType) {
			return executableElement.getAnnotation(annotationType)
		}
	}

	class ProxiedElementClass implements ProxiedInterface {
		private final Class<?> type;

		public ProxiedElementClass(Class<?> type) {
			this.type = type;
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
	}

	class ImplementedMethodMethod implements ImplementedMethod {
		private final Method m;

		public ImplementedMethodMethod(Method m) {
			this.m = m;
		}

		@Override
		public boolean isDefault() {
			return m.isDefault();
		}

		@Override
		public boolean returnsArray() {
			return m.getReturnType().isArray();
		}

		@Override
		public int getNumParameters() {
			return m.getParameterCount();
		}

		@Override
		public boolean returnsCollection() {
			return ClassType.isCollection(m.getReturnType());
		}

		@Override
		public boolean hasAnnotation(Class<?> annotationType) {
			return m.getAnnotation(annotationType) != null;
		}
	}


}
