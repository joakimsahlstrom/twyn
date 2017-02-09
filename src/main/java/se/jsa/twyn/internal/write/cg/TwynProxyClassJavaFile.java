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
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.abstractmeta.toolbox.compilation.compiler.JavaSourceCompiler;

import se.jsa.twyn.TwynProxyException;
import se.jsa.twyn.internal.IdentityMethods;
import se.jsa.twyn.internal.MethodType;
import se.jsa.twyn.internal.read.ImplementedMethod;
import se.jsa.twyn.internal.read.ProxiedInterface;
import se.jsa.twyn.internal.write.common.NodeResolver;

class TwynProxyClassJavaFile {
	private final String code;
	private final String className;

	public TwynProxyClassJavaFile(String className, String code) {
		this.className = Objects.requireNonNull(className);
		this.code = Objects.requireNonNull(code);
	}

	public static TwynProxyClassJavaFile create(ProxiedInterface implementedInterface, TwynProxyClassJavaTemplates templates, IdentityMethods identityMethods, boolean isDebug) throws IOException, URISyntaxException {
		NodeResolver nodeResolver = NodeResolver.getResolver(implementedInterface);
		return new TwynProxyClassJavaFile(
				generateClassName(implementedInterface),
				templates.templateTwynProxyClass(
						generateSimpleClassName(implementedInterface),
						implementedInterface,
						buildMethods(implementedInterface, templates, nodeResolver),
						buildEqualsComparison(implementedInterface, identityMethods),
						buildHashCodeCalls(implementedInterface, identityMethods),
						buildToString(implementedInterface, identityMethods, isDebug)));
	}

	public static String generateSimpleClassName(ProxiedInterface implementedInterface) {
		return implementedInterface.getSimpleName() + "TwynImpl";
	}

	public static String generateClassName(ProxiedInterface implementedInterface) {
		return "se.jsa.twyn." + generateSimpleClassName(implementedInterface);
	}

	private static String buildMethods(ProxiedInterface implementedInterface, TwynProxyClassJavaTemplates templates, NodeResolver nodeResolver) throws IOException, URISyntaxException {
		return implementedInterface.getMethods().stream().parallel()
			.filter(m -> !MethodType.DEFAULT.test(m))
			.map(m -> { switch (MethodType.getType(m)) {
				case ARRAY: 	return templates.templateArrayMethod(m, nodeResolver);
				case LIST: 		return templates.templateListMethod(m, nodeResolver);
				case SET:		return templates.templateSetMethod(m, nodeResolver);
				case MAP: 		return templates.templateMapMethod(m, nodeResolver);
				case INTERFACE: return templates.templateInterfaceMethod(m, nodeResolver);
				case VALUE:		return templates.templateValueMethod(m, nodeResolver);
				case SET_VALUE: return templates.templateSetValueMethod(m, implementedInterface);
				case OPTIONAL:	return templates.templateOptionalMethod(m, nodeResolver);
				default: 		throw new TwynProxyException("Could not handle method=" + m.getName()
						+ " with methodType=" + MethodType.getType(m) + " on interface " + implementedInterface.getCanonicalName());
			} })
			.collect(Collectors.joining("\n\n"))
			.toString();
	}

	private static String buildEqualsComparison(ProxiedInterface implementedInterface, IdentityMethods identityMethods) {
		return joinIdentityMethods(implementedInterface, m -> "Objects.equals(this." + m.getName() + "(), other." + m.getName() + "())", "\n\t\t\t\t&& ", identityMethods);
	}

	private static String buildHashCodeCalls(ProxiedInterface implementedInterface, IdentityMethods identityMethods) {
		return joinIdentityMethods(implementedInterface, m -> (MethodType.ARRAY.test(m) ? "(Object)" : "") + m.getName() + "()", ", ", identityMethods);
	}

	private static String buildToString(ProxiedInterface implementedInterface, IdentityMethods identityMethods, boolean isDebug) {
		return joinIdentityMethods(implementedInterface, m -> m.getName() + "()=\" + " + m.getName() + "() + \"", ", ", identityMethods)
				+ (isDebug ? ", node=\" + jsonNode + \"" : "");
	}

	private static String joinIdentityMethods(ProxiedInterface implementedInterface, Function<ImplementedMethod, String> fn, String separator, IdentityMethods identityMethods) {
		return identityMethods.getIdentityMethods(implementedInterface)
				.map(fn)
				.reduce(null, (s1, s2) -> s1 == null ? s2 : (s2 == null ? s1 : s1 + separator + s2));
	}

	public JavaSourceCompiler.CompilationUnit setupCompilationUnit(JavaSourceCompiler javaSourceCompiler) {
		JavaSourceCompiler.CompilationUnit compilationUnit = javaSourceCompiler.createCompilationUnit();
		compilationUnit.addJavaSource(getCanonicalClassName(), code);
		return compilationUnit;
	}

	public String getCanonicalClassName() {
		return className;
	}

	String getCode() {
		return code;
	}

	@Override
	public String toString() {
		return "TwynProxyJavaFile [code=" + code + ", className=" + className + "]";
	}

}
