package se.jsa.twyn.internal;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import se.jsa.twyn.TwynCollection;

class TwynProxyClassTemplates {

	private final String twynProxyClassTemplate;
	private final String twynInterfaceMethodTemplate;
	private final String twynValueMethodTemplate;
	private final String twynArrayMethodTemplate;
	private final String twynListMethodTemplate;
	private final String twynMapMethodTemplate;
	private final String twynSetMethodTemplate;

	public TwynProxyClassTemplates(String twynProxyClassTemplate, String twynInterfaceMethodTemplate, String twynValueMethodTemplate, String twynArrayMethodTemplate, String twynListMethodTemplate, String twynSetMethodTemplate, String twynMapMethodTemplate) {
		this.twynProxyClassTemplate = Objects.requireNonNull(twynProxyClassTemplate);
		this.twynInterfaceMethodTemplate = Objects.requireNonNull(twynInterfaceMethodTemplate);
		this.twynValueMethodTemplate = Objects.requireNonNull(twynValueMethodTemplate);
		this.twynArrayMethodTemplate = Objects.requireNonNull(twynArrayMethodTemplate);
		this.twynListMethodTemplate = Objects.requireNonNull(twynListMethodTemplate);
		this.twynSetMethodTemplate = Objects.requireNonNull(twynSetMethodTemplate);
		this.twynMapMethodTemplate = Objects.requireNonNull(twynMapMethodTemplate);
	}

	public static TwynProxyClassTemplates create() throws IOException, URISyntaxException {
		return new TwynProxyClassTemplates(
				readTemplate("/TwynProxyClass.java.template"),
				readTemplate("/TwynProxyClass_interfaceMethod.java.template"),
				readTemplate("/TwynProxyClass_valueMethod.java.template"),
				readTemplate("/TwynProxyClass_arrayMethod.java.template"),
				readTemplate("/TwynProxyClass_listMethod.java.template"),
				readTemplate("/TwynProxyClass_setMethod.java.template"),
				readTemplate("/TwynProxyClass_mapMethod.java.template")
				);
	}

	private static String readTemplate(String fileName) throws IOException, URISyntaxException {
		return new String(Files.readAllBytes(Paths.get(TwynProxyJavaFile.class.getResource(fileName).toURI())));
	}

	public String templateTwynProxyClass(String className, Class<?> implementedInterface, String methodBodies, String equalsComparison, String hashCodeCalls) {
		return twynProxyClassTemplate
				.replaceAll("CLASS_NAME", className)
				.replaceAll("TARGET_INTERFACE_QUALIFIED", implementedInterface.getCanonicalName())
				.replaceAll("TARGET_INTERFACE", implementedInterface.getSimpleName())
				.replaceAll("IMPLEMENTED_METHODS", methodBodies)
				.replaceAll("EQUALS_COMPARISON", equalsComparison)
				.replaceAll("HASHCODE_CALLS", hashCodeCalls);
	}

	public String templateInterfaceMethod(Method method) {
		return twynInterfaceMethodTemplate
				.replaceAll("RETURN_TYPE", method.getReturnType().getCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()));
	}

	public String templateValueMethod(Method method) {
		return twynValueMethodTemplate
				.replaceAll("RETURN_TYPE", method.getReturnType().getCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()));
	}

	public String templateArrayMethod(Method method) {
		return twynArrayMethodTemplate
				.replaceAll("RETURN_TYPE", method.getReturnType().getCanonicalName())
				.replaceAll("COMPONENT_TYPE", method.getReturnType().getComponentType().getCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()))
				.replaceAll("PARALLEL", Boolean.valueOf(method.getAnnotation(TwynCollection.class) != null && method.getAnnotation(TwynCollection.class).parallel()).toString());
	}

	public String templateListMethod(Method method) {
		TwynCollection annotation = method.getAnnotation(TwynCollection.class);
		return twynListMethodTemplate
				.replaceAll("COMPONENT_TYPE", annotation.value().getCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()))
				.replaceAll("PARALLEL", Boolean.valueOf(annotation.parallel()).toString());
	}

	public String templateSetMethod(Method method) {
		TwynCollection annotation = method.getAnnotation(TwynCollection.class);
		return twynSetMethodTemplate
				.replaceAll("COMPONENT_TYPE", annotation.value().getCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()))
				.replaceAll("PARALLEL", Boolean.valueOf(annotation.parallel()).toString());
	}

	public String templateMapMethod(Method method) {
		TwynCollection annotation = method.getAnnotation(TwynCollection.class);
		return twynMapMethodTemplate
				.replaceAll("COMPONENT_TYPE", annotation.value().getCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()))
				.replaceAll("PARALLEL", Boolean.valueOf(annotation.parallel()).toString());
	}

}
