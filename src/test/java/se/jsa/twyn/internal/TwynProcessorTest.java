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
package se.jsa.twyn.internal;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;
import org.truth0.Truth;

import se.jsa.twyn.InterfaceHolder;
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

	@Test
	public void internalInterfacesAreIncluded() throws Exception {
		Truth.ASSERT.about(JavaSourceSubjectFactory.javaSource())
		.that(JavaFileObjects.forSourceString("se.jsa.twyn.InterfaceHolder",
				"package se.jsa.twyn;"
				+ "@se.jsa.twyn.TwynProxy\n"
				+ "public interface InterfaceHolder {\n"
				+ "@TwynCollection(String.class) java.util.List<String> string();\n"
				+ "   public static interface Inner {\n"
				+ "		String getName();\n"
				+ "   }"
				+ "}"))
		.processedWith(new TwynProcessor())
		.compilesWithoutError()
		.and()
		.generatesSources(
				JavaFileObjects.forSourceString("se.jsa.twyn.InterfaceHolderTwynImpl", generateProxyCode(InterfaceHolder.class)),
				JavaFileObjects.forSourceString("se.jsa.twyn.InterfaceHolderTwynImpl$Inner", generateProxyCode(InterfaceHolder.Inner.class))
				);
	}

	private String generateProxyCode(Class<?> proxiedInterfacae) throws IOException, URISyntaxException {
		return TwynProxyClassJavaFile.create(
				ProxiedInterface.of(proxiedInterfacae),
				TwynProxyClassJavaTemplates.create(),
				new IdentityMethods(),
				false).getCode();
	}

}
