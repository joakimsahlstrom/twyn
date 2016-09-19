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
package se.jsa.twyn.internal.write;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import se.jsa.twyn.TwynIndex;
import se.jsa.twyn.internal.read.ProxiedInterface;
import se.jsa.twyn.internal.write.NodeResolver;

public class NodeResolverTest {

	@Test
	public void isArrayType() throws Exception {
		assertTrue(NodeResolver.isArrayType(ProxiedInterface.of(ArrayElement.class)));
		assertFalse(NodeResolver.isArrayType(ProxiedInterface.of(NormalType.class)));
	}
	public static interface ArrayElement {
		@TwynIndex(0) int index();
		@TwynIndex(3) String message();
	}
	public static interface NormalType {
		int index();
		String message();
	}

}
