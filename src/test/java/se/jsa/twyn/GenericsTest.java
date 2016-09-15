/*
 * Copyright 2016 Joakim Sahlström
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
package se.jsa.twyn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;

import org.junit.Test;

public class GenericsTest {

	public static class Apa {
		public Optional<String> ops() {
			return null;
		}
	}
	
	@Test
	public void testName() throws Exception {
		Method method = Apa.class.getMethod("ops");
		assertTrue(method.getReturnType().equals(Optional.class));
		ParameterizedType genericReturnType = (ParameterizedType) method.getGenericReturnType();
		assertEquals(String.class.getName(), genericReturnType.getActualTypeArguments()[0].getTypeName());
	}
	
}
