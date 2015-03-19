package se.jsa.twyn.internal;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;
import org.truth0.Truth;

import se.jsa.twyn.Simple;
import se.jsa.twyn.StringHolder;

import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourceSubjectFactory;

public class TwynProcessorTest {

	@Test
	public void canGenerateProxyJavaFileWithAP() throws Exception {
		Truth.ASSERT.about(JavaSourceSubjectFactory.javaSource())
			.that(JavaFileObjects.forSourceString("se.jsa.twyn.Simple",
					"package se.jsa.twyn;"
					+ "@se.jsa.twyn.TwynProxy\n"
					+ "public interface Simple {\n"
					+ "boolean getValue();\n"
					+ "}"))
			.processedWith(new TwynProcessor())
			.compilesWithoutError()
			.and().generatesSources(JavaFileObjects.forSourceString("se.jsa.twyn.SimpleTwynImpl",
					generateProxyCode(Simple.class)));
	}

	@Test
	public void handlesAnnotationsWithClassesAsValues() throws Exception {
		Truth.ASSERT.about(JavaSourceSubjectFactory.javaSource())
		.that(JavaFileObjects.forSourceString("se.jsa.twyn.StringHolder",
				"package se.jsa.twyn;"
				+ "@se.jsa.twyn.TwynProxy\n"
				+ "public interface StringHolder {\n"
				+ "@TwynCollection(String.class) java.util.List<String> string();\n"
				+ "}"))
		.processedWith(new TwynProcessor())
		.compilesWithoutError()
		.and().generatesSources(JavaFileObjects.forSourceString("se.jsa.twyn.StringHolderTwynImpl",
				generateProxyCode(StringHolder.class)));
	}

	private String generateProxyCode(Class<?> proxiedInterfacae) throws IOException, URISyntaxException {
		return TwynProxyClassJavaFile.create(
				ProxiedInterface.of(proxiedInterfacae),
				TwynProxyClassJavaTemplates.create(),
				new IdentityMethods(),
				false).getCode();
	}

}
