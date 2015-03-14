package se.jsa.twyn.internal;

import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import se.jsa.twyn.Generate;

@SupportedAnnotationTypes("se.jsa.twyn.Generate")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TwynProcessor extends AbstractProcessor {

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		roundEnv.getElementsAnnotatedWith(Generate.class)
			.stream()
			.filter(e -> (e instanceof TypeElement))
			.map(e -> TypeElement.class.cast(e))
			.forEach(typeElement -> {
				try {

					TwynProxyClassJavaTemplates templates = TwynProxyClassJavaTemplates.create();
					TwynProxyClassJavaFile javaFile = TwynProxyClassJavaFile.create(typeClass, templates, new IdentityMethods(), false);

					JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(javaFile.getCanonicalClassName());
					try (Writer writer = sourceFile.openWriter()) {
						writer.write(javaFile.getCode());
					}
					System.out.println("Generated: " + sourceFile.toUri().toString());
				} catch (ClassNotFoundException | IOException | URISyntaxException e) {
					System.out.println(e);
					e.printStackTrace();
					throw new IllegalArgumentException("Could not load/generate proxy for class " + typeElement.getQualifiedName(), e);
				}
			});
		return true;


//		for (Element element : roundEnv.getElementsAnnotatedWith(Generate.class)) {
//			TypeElement typeElement = TypeElement.class.cast(element);
//			try {
//				Class<?> typeClass = Thread.currentThread().getContextClassLoader().loadClass(typeElement.getQualifiedName().toString());
//
//				TwynProxyClassJavaTemplates templates = TwynProxyClassJavaTemplates.create();
//				TwynProxyClassJavaFile javaFile = TwynProxyClassJavaFile.create(typeClass, templates, new IdentityMethods(), false);
//
//				JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(javaFile.getCanonicalClassName());
//				try (Writer writer = sourceFile.openWriter()) {
//					writer.write(javaFile.getCode());
//				}
//			} catch (ClassNotFoundException | IOException | URISyntaxException e) {
//				throw new IllegalArgumentException("Could not load/generate proxy for class " + typeElement.getQualifiedName());
//			}
//		}
//		return false;
	}

}