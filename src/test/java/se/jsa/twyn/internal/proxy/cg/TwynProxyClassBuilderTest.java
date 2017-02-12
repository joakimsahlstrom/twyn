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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.jsa.twyn.internal.Cache;
import se.jsa.twyn.internal.TwynContext;

public class TwynProxyClassBuilderTest {

	private final TwynProxyClassBuilder builder = new TwynProxyClassBuilder();
	private final TwynContext twynContext = new TwynContext(new ObjectMapper(), builder, () -> new Cache.None(), true);

	@Test
	public void canResolveValue() throws Exception {
		JsonNode jsonNode = new ObjectMapper().readTree("{ \"name\" : \"Hello World!\" }");
		StringIF string = builder.buildProxy(StringIF.class, twynContext, jsonNode);
		assertEquals("Hello World!", string.getName());
	}
	public static interface StringIF { String getName(); };

	@Test
	public void canResolveComplexTypes() throws Exception {
		JsonNode jsonNode = new ObjectMapper().readTree("{ \"stringIF\" : { \"name\" : \"complex\" } }");
		ComplexIF complex = builder.buildProxy(ComplexIF.class, twynContext, jsonNode);
		assertEquals("complex", complex.getStringIF().getName());
	}
	public static interface ComplexIF {
		StringIF getStringIF();
	}

	@Test
	public void canReadComplexList() throws Exception {
		JsonNode jsonNode = new ObjectMapper().readTree("{ \"strings\" : [ { \"name\" : \"s1!\" }, { \"name\" : \"s2?\" }, { \"name\" : \"s3#\" } ] }");
		ListIF complexArray = builder.buildProxy(ListIF.class, twynContext, jsonNode);
		assertEquals("s2?", complexArray.getStrings().get(1).getName());
		assertEquals("s3#", complexArray.getStrings().get(2).getName());
	}
	public static interface ListIF {
		List<StringIF> getStrings();
	}

}
