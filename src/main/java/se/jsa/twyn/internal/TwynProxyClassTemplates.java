package se.jsa.twyn.internal;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import se.jsa.twyn.Twyn;
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
				new String(Files.readAllBytes(Paths.get(TwynProxyClassFile.class.getResource("/TwynProxyClass.java.template").toURI()))),
				new String(Files.readAllBytes(Paths.get(TwynProxyClassFile.class.getResource("/TwynProxyClass_interfaceMethod.java.template").toURI()))),
				new String(Files.readAllBytes(Paths.get(TwynProxyClassFile.class.getResource("/TwynProxyClass_valueMethod.java.template").toURI()))),
				new String(Files.readAllBytes(Paths.get(TwynProxyClassFile.class.getResource("/TwynProxyClass_arrayMethod.java.template").toURI()))),
				new String(Files.readAllBytes(Paths.get(TwynProxyClassFile.class.getResource("/TwynProxyClass_listMethod.java.template").toURI()))),
				new String(Files.readAllBytes(Paths.get(TwynProxyClassFile.class.getResource("/TwynProxyClass_mapMethod.java.template").toURI())))
				);
	}

	public String templateTwynProxyClass(String className, Class<?> implementedInterface, String methodBodies) {
		return twynProxyClassTemplate
				.replaceAll("CLASS_NAME", className)
				.replaceAll("TARGET_INTERFACE_QUALIFIED", implementedInterface.getCanonicalName())
				.replaceAll("TARGET_INTERFACE", implementedInterface.getSimpleName())
				.replaceAll("IMPLEMENTED_METHODS", methodBodies);
	}

	public String templateInterfaceMethod(Method method, Twyn twyn) {
		return twynInterfaceMethodTemplate
				.replaceAll("RETURN_TYPE", method.getReturnType().getCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_NAME", twyn.decodeJavaBeanName(method.getName()));
	}

	public String templateValueMethod(Method method, Twyn twyn) {
		return twynValueMethodTemplate
				.replaceAll("RETURN_TYPE", method.getReturnType().getCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_NAME", twyn.decodeJavaBeanName(method.getName()));
	}
	
	public String templateArrayMethod(Method method, Twyn twyn) {
		return twynArrayMethodTemplate
				.replaceAll("RETURN_TYPE", method.getReturnType().getCanonicalName())
				.replaceAll("COMPONENT_TYPE", method.getReturnType().getComponentType().getCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_NAME", twyn.decodeJavaBeanName(method.getName()));
	}

	public String templateListMethod(Method method, Twyn twyn) {
		return twynListMethodTemplate
				.replaceAll("RETURN_TYPE", method.getReturnType().getCanonicalName())
				.replaceAll("COMPONENT_TYPE", method.getAnnotation(TwynCollection.class).value().getCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_NAME", twyn.decodeJavaBeanName(method.getName()));
	}

	public String templateMapMethod(Method method, Twyn twyn) {
		return twynMapMethodTemplate.replaceAll("RETURN_TYPE", method.getReturnType().getCanonicalName())
				.replaceAll("COMPONENT_TYPE", method.getAnnotation(TwynCollection.class).value().getCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_NAME", twyn.decodeJavaBeanName(method.getName()));
	}
	
}
