package se.jsa.twyn.internal;

import static javax.tools.StandardLocation.CLASS_PATH;

import java.io.Writer;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import se.jsa.twyn.TwynProxy;

@SupportedAnnotationTypes("se.jsa.twyn.TwynProxy")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TwynProcessor extends AbstractProcessor {
	private static final Logger LOGGER = Logger.getLogger(TwynProcessor.class.getName());

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		roundEnv.getElementsAnnotatedWith(TwynProxy.class)
			.stream()
			.filter(e -> (e instanceof TypeElement))
			.map(e -> TypeElement.class.cast(e))
			.forEach(typeElement -> {
				try {
					LOGGER.info("Generating file for: " + typeElement);

					TwynProxyClassJavaFile javaFile = TwynProxyClassJavaFile.create(
							ProxiedInterface.of(typeElement),
							TwynProxyClassJavaTemplates.create(s -> processingEnv.getFiler().getResource(CLASS_PATH, "", s).getCharContent(true).toString()),
							new IdentityMethods(),
							false);
					JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(javaFile.getCanonicalClassName());
					try (Writer writer = sourceFile.openWriter()) {
						writer.write(javaFile.getCode());
					}

					LOGGER.info("Generated: file for " + sourceFile.toUri().toString());
				} catch (Throwable t) {
					LOGGER.log(Level.SEVERE, "Could not generate proxy class for: " + typeElement.getQualifiedName(), t);
					throw new IllegalArgumentException("Could not generate proxy for class " + typeElement.getQualifiedName(), t);
				}
			});
		return true;
	}

}