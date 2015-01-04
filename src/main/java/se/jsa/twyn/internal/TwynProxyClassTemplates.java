package se.jsa.twyn.internal;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import se.jsa.twyn.TwynCollection;

public class TwynProxyClassTemplates {

	private String twynProxyClassTemplate;
	private String twynInterfaceMethodTemplate;
	private String twynValueMethodTemplate;
	private String twynArrayMethodTemplate;
	private String twynListMethodTemplate;
	private String twynMapMethodTemplate;

	public TwynProxyClassTemplates(String twynProxyClassTemplate, String twynInterfaceMethodTemplate, String twynValueMethodTemplate, String twynArrayMethodTemplate, String twynListMethodTemplate, String twynMapMethodTemplate) {
		this.twynProxyClassTemplate = Objects.requireNonNull(twynProxyClassTemplate);
		this.twynInterfaceMethodTemplate = Objects.requireNonNull(twynInterfaceMethodTemplate);
		this.twynValueMethodTemplate = Objects.requireNonNull(twynValueMethodTemplate);
		this.twynArrayMethodTemplate = Objects.requireNonNull(twynArrayMethodTemplate);
		this.twynListMethodTemplate = Objects.requireNonNull(twynListMethodTemplate);
		this.twynMapMethodTemplate = Objects.requireNonNull(twynMapMethodTemplate);
	}

	public static TwynProxyClassTemplates create() throws IOException, URISyntaxException {
		return new TwynProxyClassTemplates(
				new String(Files.readAllBytes(Paths.get(TwynProxyJavaFile.class.getResource("/TwynProxyClass.java.template").toURI()))),
				new String(Files.readAllBytes(Paths.get(TwynProxyJavaFile.class.getResource("/TwynProxyClass_interfaceMethod.java.template").toURI()))),
				new String(Files.readAllBytes(Paths.get(TwynProxyJavaFile.class.getResource("/TwynProxyClass_valueMethod.java.template").toURI()))),
				new String(Files.readAllBytes(Paths.get(TwynProxyJavaFile.class.getResource("/TwynProxyClass_arrayMethod.java.template").toURI()))),
				new String(Files.readAllBytes(Paths.get(TwynProxyJavaFile.class.getResource("/TwynProxyClass_listMethod.java.template").toURI()))),
				new String(Files.readAllBytes(Paths.get(TwynProxyJavaFile.class.getResource("/TwynProxyClass_mapMethod.java.template").toURI())))
				);
	}

	public String templateTwynProxyClass(String className, Class<?> implementedInterface, String methodBodies) {
		return twynProxyClassTemplate
				.replaceAll("CLASS_NAME", className)
				.replaceAll("TARGET_INTERFACE_QUALIFIED", implementedInterface.getCanonicalName())
				.replaceAll("TARGET_INTERFACE", implementedInterface.getSimpleName())
				.replaceAll("IMPLEMENTED_METHODS", methodBodies);
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
				.replaceAll("RETURN_TYPE", method.getReturnType().getCanonicalName())
				.replaceAll("COMPONENT_TYPE", annotation.value().getCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()))
				.replaceAll("PARALLEL", Boolean.valueOf(annotation.parallel()).toString());
	}

	public String templateMapMethod(Method method) {
		TwynCollection annotation = method.getAnnotation(TwynCollection.class);
		return twynMapMethodTemplate.replaceAll("RETURN_TYPE", method.getReturnType().getCanonicalName())
				.replaceAll("COMPONENT_TYPE", annotation.value().getCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()))
				.replaceAll("PARALLEL", Boolean.valueOf(annotation.parallel()).toString());
	}
	
}
