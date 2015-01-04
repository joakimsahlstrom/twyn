package se.jsa.twyn.internal;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;

import org.abstractmeta.toolbox.compilation.compiler.JavaSourceCompiler;

class TwynProxyJavaFile {
	private final String code;
	private final String className;

	public TwynProxyJavaFile(String className, String code) {
		this.className = Objects.requireNonNull(className);
		this.code = Objects.requireNonNull(code);
	}

	public static TwynProxyJavaFile create(Class<?> implementedInterface, TwynProxyClassTemplates templates) throws IOException, URISyntaxException {
		String className = implementedInterface.getSimpleName() + "TwynImpl";
		return new TwynProxyJavaFile(className, templates.templateTwynProxyClass(className, implementedInterface, buildMethods(implementedInterface, templates)));
	}

	private static String buildMethods(Class<?> implementedInterface, TwynProxyClassTemplates templates) throws IOException, URISyntaxException {
		return Arrays.asList(implementedInterface.getMethods()).stream().parallel()
			.filter(m -> !MethodType.DEFAULT.test(m))
			.map(m -> { switch (MethodType.getType(m)) {
				case ARRAY: 	return templates.templateArrayMethod(m);
				case LIST: 		return templates.templateListMethod(m);
				case MAP: 		return templates.templateMapMethod(m);
				case INTERFACE: return templates.templateInterfaceMethod(m);
				case VALUE:		return templates.templateValueMethod(m);
				default: 		throw new RuntimeException("Could not handle methodType=" + MethodType.getType(m));
			} })
			.collect(StringBuilder::new, (sb, s) -> sb.append(s), (sb1, sb2) -> sb1.append(sb2.toString()))
			.toString();
	}

	public JavaSourceCompiler.CompilationUnit setupCompilationUnit(JavaSourceCompiler javaSourceCompiler) {
		JavaSourceCompiler.CompilationUnit compilationUnit = javaSourceCompiler.createCompilationUnit();
		compilationUnit.addJavaSource(getClassName(), code);
		return compilationUnit;
	}

	public String getClassName() {
		return "se.jsa.twyn." + className;
	}

	@Override
	public String toString() {
		return "TwynProxyJavaFile [code=" + code + ", className=" + className + "]";
	}

}
