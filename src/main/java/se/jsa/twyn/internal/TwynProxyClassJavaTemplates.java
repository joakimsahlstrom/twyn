package se.jsa.twyn.internal;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import se.jsa.twyn.TwynCollection;
import se.jsa.twyn.internal.ProxiedInterface.ImplementedMethod;

class TwynProxyClassJavaTemplates {

	private final String twynProxyClassTemplate;
	private final String twynInterfaceMethodTemplate;
	private final String twynValueMethodTemplate;
	private final String twynArrayMethodTemplate;
	private final String twynListMethodTemplate;
	private final String twynMapMethodTemplate;
	private final String twynSetMethodTemplate;
	private final String twynSetValueMethodTemplate;

	public TwynProxyClassJavaTemplates(
			String twynProxyClassTemplate,
			String twynInterfaceMethodTemplate,
			String twynValueMethodTemplate,
			String twynArrayMethodTemplate,
			String twynListMethodTemplate,
			String twynSetMethodTemplate,
			String twynMapMethodTemplate,
			String twynSetValueMethodTemplate) {
		this.twynProxyClassTemplate = Objects.requireNonNull(twynProxyClassTemplate);
		this.twynInterfaceMethodTemplate = Objects.requireNonNull(twynInterfaceMethodTemplate);
		this.twynValueMethodTemplate = Objects.requireNonNull(twynValueMethodTemplate);
		this.twynArrayMethodTemplate = Objects.requireNonNull(twynArrayMethodTemplate);
		this.twynListMethodTemplate = Objects.requireNonNull(twynListMethodTemplate);
		this.twynSetMethodTemplate = Objects.requireNonNull(twynSetMethodTemplate);
		this.twynMapMethodTemplate = Objects.requireNonNull(twynMapMethodTemplate);
		this.twynSetValueMethodTemplate = Objects.requireNonNull(twynSetValueMethodTemplate);
	}

	public interface Reader {
		String read(String resourceName) throws IOException, URISyntaxException;
	}

	public static TwynProxyClassJavaTemplates create() throws IOException, URISyntaxException {
		return create(s -> new String(Files.readAllBytes(Paths.get(TwynProxyClassJavaFile.class.getResource("/" + s).toURI()))));
	}

	public static TwynProxyClassJavaTemplates create(Reader reader) throws IOException, URISyntaxException {
		return new TwynProxyClassJavaTemplates(
				reader.read("TwynProxyClass.java.template"),
				reader.read("TwynProxyClass_interfaceMethod.java.template"),
				reader.read("TwynProxyClass_valueMethod.java.template"),
				reader.read("TwynProxyClass_arrayMethod.java.template"),
				reader.read("TwynProxyClass_listMethod.java.template"),
				reader.read("TwynProxyClass_setMethod.java.template"),
				reader.read("TwynProxyClass_mapMethod.java.template"),
				reader.read("TwynProxyClass_setValueMethod.java.template")
				);
	}

	public String templateTwynProxyClass(String className, ProxiedInterface implementedInterface, String methodBodies, String equalsComparison, String hashCodeCalls, String toString) {
		return twynProxyClassTemplate
				.replaceAll("CLASS_NAME", className)
				.replaceAll("TARGET_INTERFACE_QUALIFIED", implementedInterface.getCanonicalName())
				.replaceAll("TARGET_INTERFACE", implementedInterface.getSimpleName())
				.replaceAll("IMPLEMENTED_METHODS", methodBodies)
				.replaceAll("EQUALS_COMPARISON", equalsComparison)
				.replaceAll("HASHCODE_CALLS", hashCodeCalls)
				.replaceAll("TOSTRING", toString);
	}

	public String templateInterfaceMethod(ImplementedMethod method, NodeResolver nodeResolver) {
		return twynInterfaceMethodTemplate
				.replaceAll("RETURN_TYPE", method.getReturnTypeCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_ID", nodeResolver.resolveNodeId(method))
				.replaceAll("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()));
	}

	public String templateValueMethod(ImplementedMethod method, NodeResolver nodeResolver) {
		return twynValueMethodTemplate
				.replaceAll("RETURN_TYPE", method.getReturnTypeCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_ID", nodeResolver.resolveNodeId(method))
				.replaceAll("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()));
	}

	public String templateArrayMethod(ImplementedMethod method, NodeResolver nodeResolver) {
		return twynArrayMethodTemplate
				.replaceAll("RETURN_TYPE", method.getReturnTypeCanonicalName())
				.replaceAll("COMPONENT_TYPE", method.getReturnComponentTypeCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_ID", nodeResolver.resolveNodeId(method))
				.replaceAll("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()))
				.replaceAll("PARALLEL", Boolean.valueOf(method.hasAnnotation(TwynCollection.class) && method.getAnnotation(TwynCollection.class).parallel()).toString());
	}

	public String templateListMethod(ImplementedMethod method, NodeResolver nodeResolver) {
		TwynCollection annotation = method.getAnnotation(TwynCollection.class);
		return twynListMethodTemplate
				.replaceAll("COMPONENT_TYPE", annotation.value().getCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_ID", nodeResolver.resolveNodeId(method))
				.replaceAll("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()))
				.replaceAll("PARALLEL", Boolean.valueOf(annotation.parallel()).toString());
	}

	public String templateSetMethod(ImplementedMethod method, NodeResolver nodeResolver) {
		TwynCollection annotation = method.getAnnotation(TwynCollection.class);
		return twynSetMethodTemplate
				.replaceAll("COMPONENT_TYPE", annotation.value().getCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_ID", nodeResolver.resolveNodeId(method))
				.replaceAll("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()))
				.replaceAll("PARALLEL", Boolean.valueOf(annotation.parallel()).toString());
	}

	public String templateMapMethod(ImplementedMethod method, NodeResolver nodeResolver) {
		TwynCollection annotation = method.getAnnotation(TwynCollection.class);
		return twynMapMethodTemplate
				.replaceAll("COMPONENT_TYPE", annotation.value().getCanonicalName())
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("FIELD_ID", nodeResolver.resolveNodeId(method))
				.replaceAll("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()))
				.replaceAll("PARALLEL", Boolean.valueOf(annotation.parallel()).toString());
	}

	public String templateSetValueMethod(ImplementedMethod method, ProxiedInterface implementedType) {
		return twynSetValueMethodTemplate
				.replaceAll("VALUE_TYPE", method.getParameterTypeCanonicalName(0))
				.replaceAll("METHOD_NAME", method.getName())
				.replaceAll("RETURN_TYPE", method.returns(implementedType) ? implementedType.getCanonicalName() : "void")
				.replaceAll("RETURN", method.returns(implementedType) ? "return this;" : "")
				.replaceAll("FIELD_NAME", TwynUtil.decodeJavaBeanSetName(method.getName()));
	}

}
