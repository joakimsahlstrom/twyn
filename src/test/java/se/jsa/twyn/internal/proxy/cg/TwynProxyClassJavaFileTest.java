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
package se.jsa.twyn.internal.proxy.cg;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import se.jsa.twyn.ArrayIndex;
import se.jsa.twyn.TwynTest;
import se.jsa.twyn.TwynTest.StringIF;
import se.jsa.twyn.internal.IdentityMethods;
import se.jsa.twyn.internal.readmodel.ProxiedInterface;

// For development debugging
public class TwynProxyClassJavaFileTest {

	@Ignore
	@Test
	public void generatesCorrectCode() throws Exception {
		System.out.println(getCode(Offspring.class).getCode());
	}
	public static interface Offspring {
		String myName();

		Daughter[] daughters();

		Map<String, Nick> daughterNickNames();

		String[] sons();

		List<Entity> getUnknowns();

		Set<Song> songs();
	}
	public static interface Daughter {
		String getName();
	}
	public static interface Nick {
		String nick();
	}
	public static interface Entity {
		String name();
		String type();
	}
	public static interface Song {
		String name();
	}

	@Test
	@Ignore
	public void canReadComplexSet() throws Exception {
//		System.out.println(getCode(SetIF.class).getCode());
		System.out.println(getCode(TwynTest.ObjectHoldingSetIF.class).getCode());
//		System.out.println(getCode(ArrayElement.class).getCode());
	}
	public static interface SetIF {
		Set<StringIF> getStrings();
	}

	public static interface ArrayElement {
		@ArrayIndex(0) int index();
		@ArrayIndex(3) String message();
	}

	private TwynProxyClassJavaFile getCode(Class<?> type) throws IOException, URISyntaxException {
		return TwynProxyClassJavaFile.create(ProxiedInterface.of(type), TwynProxyClassJavaTemplates.create(), new IdentityMethods(), true);
	}

}
