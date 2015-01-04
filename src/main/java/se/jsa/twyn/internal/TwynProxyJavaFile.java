package se.jsa.twyn.internal;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import se.jsa.twyn.Twyn;
import se.jsa.twyn.TwynCollection;

public class TwynProxyJavaFile {
	private String code;
	private String className;

	public TwynProxyJavaFile(String className, String code) {
		this.className = Objects.requireNonNull(className);
		this.code = Objects.requireNonNull(code);
	}

	public static TwynProxyJavaFile create(String className, Class<?> implementedInterface, Twyn twyn, TwynProxyClassTemplates templates) throws IOException, URISyntaxException {
		return new TwynProxyJavaFile(className, templates.templateTwynProxyClass(className, implementedInterface, buildMethods(implementedInterface, twyn, templates)));
	}
	
	private static String buildMethods(Class<?> implementedInterface, Twyn twyn, TwynProxyClassTemplates templates) throws IOException, URISyntaxException {
		StringBuilder result = new StringBuilder();
		for (Method method : Arrays.asList(implementedInterface.getMethods()).stream().filter(m -> !m.isDefault()).collect(Collectors.toList())) {
			if (method.getReturnType().isArray() && method.getReturnType().getComponentType().isInterface()) {
				result.append(templates.templateArrayMethod(method, twyn));
			} else if (method.getReturnType().equals(List.class) && method.getAnnotation(TwynCollection.class) != null) {
				result.append(templates.templateListMethod(method, twyn));
			} else if (method.getReturnType().equals(Map.class) && method.getAnnotation(TwynCollection.class) != null) {
				result.append(templates.templateMapMethod(method, twyn));
			} else if (method.getReturnType().isInterface()) {
				result.append(templates.templateInterfaceMethod(method, twyn));
			} else {
				result.append(templates.templateValueMethod(method, twyn));
			}
		}
		return result.toString();
	}
	
	public String getCode() {
		return code;
	}

	public String getClassName() {
		return "se.jsa.twyn." + className;
	}
	
}
