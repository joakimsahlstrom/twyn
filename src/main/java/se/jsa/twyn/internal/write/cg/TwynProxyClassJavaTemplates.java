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
package se.jsa.twyn.internal.write.cg;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import se.jsa.twyn.internal.read.ImplementedMethod;
import se.jsa.twyn.internal.read.ProxiedInterface;
import se.jsa.twyn.internal.write.NodeResolver;
import se.jsa.twyn.internal.write.TwynUtil;

class TwynProxyClassJavaTemplates {

	private final String twynProxyClassTemplate;
	private final String twynInterfaceMethodTemplate;
	private final String twynValueMethodTemplate;
	private final String twynArrayMethodTemplate;
	private final String twynListMethodTemplate;
	private final String twynMapMethodTemplate;
	private final String twynMapMethodTypedKeyTemplate;
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
			String twynSetValueMethodTemplate,
			String twynMapMethodTypedKeyTemplate) {
		this.twynProxyClassTemplate = Objects.requireNonNull(twynProxyClassTemplate);
		this.twynInterfaceMethodTemplate = Objects.requireNonNull(twynInterfaceMethodTemplate);
		this.twynValueMethodTemplate = Objects.requireNonNull(twynValueMethodTemplate);
		this.twynArrayMethodTemplate = Objects.requireNonNull(twynArrayMethodTemplate);
		this.twynListMethodTemplate = Objects.requireNonNull(twynListMethodTemplate);
		this.twynSetMethodTemplate = Objects.requireNonNull(twynSetMethodTemplate);
		this.twynMapMethodTemplate = Objects.requireNonNull(twynMapMethodTemplate);
		this.twynSetValueMethodTemplate = Objects.requireNonNull(twynSetValueMethodTemplate);
		this.twynMapMethodTypedKeyTemplate = Objects.requireNonNull(twynMapMethodTypedKeyTemplate);
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
				reader.read("TwynProxyClass_setValueMethod.java.template"),
				reader.read("TwynProxyClass_mapMethodTyped.java.template")
				);
	}

	public String templateTwynProxyClass(String className, ProxiedInterface implementedInterface, String methodBodies, String equalsComparison, String hashCodeCalls, String toString) {
		return twynProxyClassTemplate
				.replace("CLASS_NAME", className)
				.replace("TARGET_INTERFACE_QUALIFIED", implementedInterface.getCanonicalName())
				.replace("TARGET_INTERFACE", implementedInterface.getSimpleName())
				.replace("IMPLEMENTED_METHODS", methodBodies)
				.replace("EQUALS_COMPARISON", equalsComparison)
				.replace("HASHCODE_CALLS", hashCodeCalls)
				.replace("TOSTRING", toString);
	}

	public String templateInterfaceMethod(ImplementedMethod method, NodeResolver nodeResolver) {
		return twynInterfaceMethodTemplate
				.replace("RETURN_TYPE", method.getReturnTypeCanonicalName())
				.replace("METHOD_NAME", method.getName())
				.replace("FIELD_ID", nodeResolver.resolveNodeId(method))
				.replace("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()))
				.replace("DECLARING_CLASS", method.getDeclaringClassSimpleName());
	}

	public String templateValueMethod(ImplementedMethod method, NodeResolver nodeResolver) {
		return twynValueMethodTemplate
				.replace("RETURN_TYPE", method.getReturnTypeCanonicalName())
				.replace("METHOD_NAME", method.getName())
				.replace("FIELD_ID", nodeResolver.resolveNodeId(method))
				.replace("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()))
				.replace("DECLARING_CLASS", method.getDeclaringClassSimpleName());
	}

	public String templateArrayMethod(ImplementedMethod method, NodeResolver nodeResolver) {
		return twynArrayMethodTemplate
				.replace("RETURN_TYPE", method.getReturnTypeCanonicalName())
				.replace("COMPONENT_TYPE", method.getReturnComponentTypeCanonicalName())
				.replace("METHOD_NAME", method.getName())
				.replace("FIELD_ID", nodeResolver.resolveNodeId(method))
				.replace("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()))
				.replace("DECLARING_CLASS", method.getDeclaringClassSimpleName());
	}

	public String templateListMethod(ImplementedMethod method, NodeResolver nodeResolver) {
		return twynListMethodTemplate
				.replace("COMPONENT_TYPE", method.getReturnTypeParameterTypeCanonicalName(0).replace("$", "."))
				.replace("METHOD_NAME", method.getName())
				.replace("FIELD_ID", nodeResolver.resolveNodeId(method))
				.replace("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()))
				.replace("DECLARING_CLASS", method.getDeclaringClassSimpleName());
	}

	public String templateSetMethod(ImplementedMethod method, NodeResolver nodeResolver) {
		return twynSetMethodTemplate
				.replace("COMPONENT_TYPE", method.getReturnTypeParameterTypeCanonicalName(0).replace("$", "."))
				.replace("METHOD_NAME", method.getName())
				.replace("FIELD_ID", nodeResolver.resolveNodeId(method))
				.replace("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()))
				.replace("DECLARING_CLASS", method.getDeclaringClassSimpleName());
	}

	public String templateMapMethod(ImplementedMethod method, NodeResolver nodeResolver) {
		if (!method.getReturnTypeParameterTypeCanonicalName(0).replace("$", ".").equals(String.class.getCanonicalName())) {
			return templateMapMethodTyped(method, nodeResolver);
		} else {
			return twynMapMethodTemplate
					.replace("COMPONENT_TYPE", method.getReturnTypeParameterTypeCanonicalName(1).replace("$", "."))
					.replace("METHOD_NAME", method.getName())
					.replace("FIELD_ID", nodeResolver.resolveNodeId(method))
					.replace("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()))
					.replace("DECLARING_CLASS", method.getDeclaringClassSimpleName());
		}
	}

	public String templateMapMethodTyped(ImplementedMethod method, NodeResolver nodeResolver) {
		return twynMapMethodTypedKeyTemplate
				.replace("COMPONENT_TYPE", method.getReturnTypeParameterTypeCanonicalName(1).replace("$", "."))
				.replace("METHOD_NAME", method.getName())
				.replace("FIELD_ID", nodeResolver.resolveNodeId(method))
				.replace("FIELD_NAME", TwynUtil.decodeJavaBeanName(method.getName()))
				.replace("KEY_TYPE", method.getReturnTypeParameterTypeCanonicalName(0).replace("$", "."))
				.replace("DECLARING_CLASS", method.getDeclaringClassSimpleName());
	}

	public String templateSetValueMethod(ImplementedMethod method, ProxiedInterface implementedType) {
		return twynSetValueMethodTemplate
				.replace("VALUE_TYPE", method.getParameterTypeCanonicalName(0))
				.replace("METHOD_NAME", method.getName())
				.replace("RETURN_TYPE", method.returns(implementedType) ? implementedType.getCanonicalName() : "void")
				.replace("RETURN", method.returns(implementedType) ? "return this;" : "")
				.replace("FIELD_NAME", TwynUtil.decodeJavaBeanSetName(method.getName()));
	}

}
