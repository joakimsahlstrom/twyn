package se.jsa.twyn.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
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
		private static final Set<String> collectionQualifiedNames = new HashSet<>(Arrays.asList("java.util.List", "java.util.Set", "java.util.Map"));

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
			ArrayType returnArrayType = (ArrayType) executableElement.getReturnType();
			if (returnArrayType.getComponentType() instanceof DeclaredType) {
				DeclaredType declaredReturnType = (DeclaredType) returnArrayType.getComponentType();
				return declaredReturnType.asElement().getKind() == ElementKind.INTERFACE;
			}
			return false;
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
		public boolean returnsInterface() {
			return executableElement.getReturnType() instanceof DeclaredType
					&& ((DeclaredType)executableElement.getReturnType()).asElement().getKind() == ElementKind.INTERFACE;
		}

		@Override
		public boolean returns(Class<?> returnType) {
			if (executableElement.getReturnType() instanceof PrimitiveType) {
				PrimitiveType primitiveReturnType = (PrimitiveType) executableElement.getReturnType();
				switch (primitiveReturnType.getKind()) {
				case BOOLEAN: return returnType.equals(Boolean.TYPE);
				case BYTE: return returnType.equals(Byte.TYPE);
				case CHAR: return returnType.equals(Character.TYPE);
				case DOUBLE: return returnType.equals(Double.TYPE);
				case FLOAT: return returnType.equals(Float.TYPE);
				case INT: return returnType.equals(Integer.TYPE);
				case LONG: return returnType.equals(Long.TYPE);
				case SHORT: return returnType.equals(Short.TYPE);
				default:
					throw new IllegalArgumentException("Cannot handle primitive return type: " + primitiveReturnType.getKind());
				}
			}
			return getReturnTypeCanonicalName().equals(returnType.getCanonicalName());
		}

		@Override
		public boolean returns(ProxiedInterface implementedType) {
			return getReturnTypeCanonicalName().equals(implementedType.getCanonicalName());
		}

		@Override
		public String getReturnTypeCanonicalName() {
			TypeMirror returnTypeMirror = executableElement.getReturnType();
			return getCanonicalName(returnTypeMirror);
		}

		@Override
		public String getReturnComponentTypeCanonicalName() {
			ArrayType returnArrayType = (ArrayType) executableElement.getReturnType();
			return getCanonicalName(returnArrayType.getComponentType());
		}

		@Override
		public String getTwynCollectionTypeCanonicalName() {
			try {
				getAnnotation(TwynCollection.class).value();
				throw new RuntimeException("Should not get here!");
			} catch(MirroredTypeException mte) {
				return mte.getTypeMirror().toString();
			}
		}

		private String getCanonicalName(TypeMirror typeMirror) {
			if (typeMirror instanceof DeclaredType) {
				DeclaredType declaredReturnType = (DeclaredType) typeMirror;
				TypeElement returnElementType = (TypeElement) declaredReturnType.asElement();
				return returnElementType.getQualifiedName().toString();
			} else if (typeMirror instanceof PrimitiveType) {
				PrimitiveType primitiveReturnType = (PrimitiveType)typeMirror;
				switch (primitiveReturnType.getKind()) {
				case BOOLEAN: return "boolean";
				case BYTE: return "byte";
				case CHAR: return "char";
				case DOUBLE: return "double";
				case FLOAT: return "float";
				case INT: return "int";
				case LONG: return "long";
				case SHORT: return "short";
				default:
					throw new IllegalArgumentException("Cannot get canonical name for primitive return type: " + primitiveReturnType.getKind());
				}
			} else {
				throw new RuntimeException("Cannot determine canonical name of type: " + typeMirror);
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
			}
			return super.equals(obj);
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

		public Method getMethod() {
			return m;
		}
	}

}
