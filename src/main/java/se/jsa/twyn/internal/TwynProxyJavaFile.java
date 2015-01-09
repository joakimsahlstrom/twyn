package se.jsa.twyn.internal;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.abstractmeta.toolbox.compilation.compiler.JavaSourceCompiler;

class TwynProxyJavaFile {
	private final String code;
	private final String className;

	public TwynProxyJavaFile(String className, String code) {
		this.className = Objects.requireNonNull(className);
		this.code = Objects.requireNonNull(code);
	}

	public static TwynProxyJavaFile create(Class<?> implementedInterface, TwynProxyClassTemplates templates, TwynContext twynContext) throws IOException, URISyntaxException {
		String className = implementedInterface.getSimpleName() + "TwynImpl";
		return new TwynProxyJavaFile(
				className,
				templates.templateTwynProxyClass(
						className,
						implementedInterface,
						buildMethods(implementedInterface, templates),
						buildEqualsComparison(implementedInterface, twynContext),
						buildHashCodeCalls(implementedInterface, twynContext),
						buildToString(implementedInterface, twynContext)));
	}

	private static String buildMethods(Class<?> implementedInterface, TwynProxyClassTemplates templates) throws IOException, URISyntaxException {
		return Stream.of(implementedInterface.getMethods()).parallel()
			.filter(m -> !MethodType.DEFAULT.test(m))
			.map(m -> { switch (MethodType.getType(m)) {
				case ARRAY: 	return templates.templateArrayMethod(m);
				case LIST: 		return templates.templateListMethod(m);
				case SET:		return templates.templateSetMethod(m);
				case MAP: 		return templates.templateMapMethod(m);
				case INTERFACE: return templates.templateInterfaceMethod(m);
				case VALUE:		return templates.templateValueMethod(m);
				case SET_VALUE: return templates.templateSetValueMethod(m, implementedInterface);
				default: 		throw new RuntimeException("Could not handle method=" + m.getName()
						+ " with methodType=" + MethodType.getType(m) + " on interface " + implementedInterface.getCanonicalName());
			} })
			.collect(StringBuilder::new, (sb, s) -> sb.append(s).append("\n\n"), (sb1, sb2) -> sb1.append(sb2.toString()))
			.toString();
	}

	private static String buildEqualsComparison(Class<?> implementedInterface, TwynContext twynContext) {
		return joinIdentityMethods(implementedInterface, m -> { return "Objects.equals(this." + m.getName() + "(), other." + m.getName() + "())"; }, "\n\t\t\t\t&& ", twynContext);
	}

	private static String buildHashCodeCalls(Class<?> implementedInterface, TwynContext twynContext) {
		return joinIdentityMethods(implementedInterface, m -> { return (MethodType.ARRAY.test(m) ? "(Object)" : "") + m.getName() + "()"; }, ", ", twynContext);
	}

	private static String buildToString(Class<?> implementedInterface, TwynContext twynContext) {
		return joinIdentityMethods(implementedInterface, m -> { return m.getName() + "()=\" + " + m.getName() + "() + \""; }, ", ", twynContext)
				+ (twynContext.isDebug() ? ", node=\" + jsonNode + \"" : "");
	}

	private static String joinIdentityMethods(Class<?> implementedInterface, Function<Method, String> fn, String separator, TwynContext twynContext) {
		return twynContext.getIdentityMethods(implementedInterface)
				.map(fn)
				.reduce(null, (s1, s2) -> { return (s1 == null ? s2 : (s2 == null ? s1 : s1 + separator + s2)); });
	}

	public JavaSourceCompiler.CompilationUnit setupCompilationUnit(JavaSourceCompiler javaSourceCompiler) {
		JavaSourceCompiler.CompilationUnit compilationUnit = javaSourceCompiler.createCompilationUnit();
		compilationUnit.addJavaSource(getCanonicalClassName(), code);
		return compilationUnit;
	}

	public String getCanonicalClassName() {
		return "se.jsa.twyn." + className;
	}

	String getCode() {
		return code;
	}

	@Override
	public String toString() {
		return "TwynProxyJavaFile [code=" + code + ", className=" + className + "]";
	}

}
