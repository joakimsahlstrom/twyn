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
package se.jsa.twyn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.JsonParseException;

@RunWith(Parameterized.class)
public class TwynTest {

	private final Twyn twyn;

	public TwynTest(Twyn twyn) {
		this.twyn = twyn;
	}

	@Parameters
	public static Collection<Object[]> twyns() {
		return Arrays.<Object[]>asList(
				new Object[] { Twyn.configurer().withJavaProxies().withDebugMode().configure() },
				new Object[] { Twyn.configurer().withJavaProxies().withFullCaching().withDebugMode().configure() },
				new Object[] { Twyn.configurer().withClassGeneration()
						.withPrecompiledClasses(getInterfaces())
						.withDebugMode().configure() },
				new Object[] { Twyn.configurer().withClassGeneration()
						.withPrecompiledClasses(getInterfaces())
						.withFullCaching().withDebugMode().configure() }
				);
	}

	private static final Collection<Class<?>> INTENTIONALLY_BROKEN_INTERFACES = Arrays.asList(
			InterfaceWithNonDefaultMethodWithParameters.class,
			NoAnnotCollectionIF.class);
	private static Collection<Class<?>> getInterfaces() {
		return Arrays.asList(TwynTest.class.getDeclaredClasses()).stream()
			.filter(c -> c.isInterface() && !INTENTIONALLY_BROKEN_INTERFACES.contains(c))
			.collect(Collectors.toList());
	}

	@Test
	public void canReadString() throws Exception {
		StringIF string = twyn.read(input("{ \"name\" : \"Hello World!\" }"), StringIF.class);
		assertEquals("Hello World!", string.getName());
	}
	public static interface StringIF { String getName(); };

	@Test
	public void canReadAllJsonTypes() throws Exception {
		TypesIF types = twyn.read(input(
				"{ "
				+ "\"i\" : \"1\", \"integer\" : \"2\", "
				+ "\"b\" : \"false\", \"boolean\" : \"true\", "
				+ "\"d\" : \"1.01\", \"double\" : \"1.02\", "
				+ "\"l\" : \"1000000000000\", \"long\" : \"1000000000001\" "
				+ "}"), TypesIF.class);
		assertEquals(1, types.getI());
		assertEquals(Integer.valueOf(2), types.getInteger());
		assertEquals(false, types.getB());
		assertEquals(Boolean.TRUE, types.getBoolean());
		assertEquals(1.01, types.getD(), 0.001);
		assertEquals(Double.valueOf(1.02), types.getDouble());
		assertEquals(1000000000000L, types.getL());
		assertEquals(Long.valueOf(1000000000001L), types.getLong());
	}
	public static interface TypesIF {
		int getI();
		Integer getInteger();
		boolean getB();
		Boolean getBoolean();
		double getD();
		Double getDouble();
		long getL();
		Long getLong();
	}

	@Test
	public void canCallDefaultMethodOnInterface() throws Exception {
		DefaultMethodIF defaultMethod = twyn.read(input("{ \"name\" : \"Java8\" }"), DefaultMethodIF.class);
		assertEquals("Hello Java8!", defaultMethod.getDecoratedName());
	}
	public static interface DefaultMethodIF {
		String getName();
		default String getDecoratedName() {
			return "Hello " + getName() + "!";
		}
	}

	@Test
	public void canResolveComplexTypes() throws Exception {
		ComplexIF complex = twyn.read(input("{ \"stringIF\" : { \"name\" : \"complex\" } }"), ComplexIF.class);
		assertEquals("complex", complex.getStringIF().getName());
	}
	public static interface ComplexIF {
		StringIF getStringIF();
	}

	@Test
	public void canResolveComplexObjects() throws Exception {
		ObjectHoldingIF complexObject = twyn.read(input("{ \"id\" : { \"val\" : \"2\" } }"), ObjectHoldingIF.class);
		assertEquals(2, complexObject.getId().getVal());
	}
	public static class MyId {
		private int val;
		public MyId() { }
		public MyId(int val) {
			this.val = val;
		}
		public int getVal() {
			return val;
		}
	}
	public static interface ObjectHoldingIF {
		public MyId getId();
	}

	@Test
	public void canReadPrimitiveArrays() throws Exception {
		PrimitiveArrayIF primitiveArray = twyn.read(input("{ \"data\" : [ \"1\", \"2\", \"97\" ] }"), PrimitiveArrayIF.class);
		assertEquals(97, primitiveArray.getData()[2]);
	}
	public static interface PrimitiveArrayIF {
		int[] getData();
	}

	@Test
	public void canReadComplexArrays() throws Exception {
		ComplexArrayIF complexArray = twyn.read(input("{ \"strings\" : [ { \"name\" : \"s1!\" }, { \"name\" : \"s2?\" }, { \"name\" : \"s3#\" } ] }"), ComplexArrayIF.class);
		assertEquals("s2?", complexArray.getStrings()[1].getName());
		assertEquals("s3#", complexArray.getStringList().get(2).getName());
	}
	public static class StringIFList {
		private final StringIF[] arr;
		public StringIFList(StringIF[] arr) {
			this.arr = arr;
		}
		public StringIF get(int index) {
			return arr[index];
		}
	}
	public static interface ComplexArrayIF {
		StringIF[] getStrings();
		default StringIFList getStringList() {
			return new StringIFList(getStrings());
		}
	}

	@Test
	public void canReadIsField() throws Exception {
		IsIF is = twyn.read(input("{ \"ok\" : \"true\" }"), IsIF.class);
		assertTrue(is.isOk());
	}
	public static interface IsIF {
		boolean isOk();
	}

	@Test
	public void canReadNonJavaBeanField() throws Exception {
		ShortNameIF shortName = twyn.read(input("{ \"ok\" : \"true\" }"), ShortNameIF.class);
		assertTrue(shortName.ok());
	}
	public static interface ShortNameIF {
		boolean ok();
	}

	@Test
	public void returnsNullForNullValue() throws Exception {
		StringIF nulled = twyn.read(input("{ \"name\" : null }"), StringIF.class);
		assertNull(nulled.getName());
	}

	@Test(expected = NullPointerException.class)
	public void throwsExceptionIfValueIsMissing() throws Exception {
		StringIF missing = twyn.read(input("{ }"), StringIF.class);
		missing.getName();
	}

	@Test
	public void canReadComplexList() throws Exception {
		ListIF complexArray = twyn.read("{ \"strings\" : [ { \"name\" : \"s1!\" }, { \"name\" : \"s2?\" }, { \"name\" : \"s3#\" } ] }", ListIF.class);
		assertEquals("s2?", complexArray.getStrings().get(1).getName());
		assertEquals("s3#", complexArray.getStrings().get(2).getName());
	}
	public static interface ListIF {
		@TwynCollection(StringIF.class)
		List<StringIF> getStrings();
	}

	@Test
	public void canReadComplexListParallel() throws Exception {
		ParallelListIF complexArray = twyn.read("{ \"strings\" : [ { \"name\" : \"s1!\" }, { \"name\" : \"s2?\" }, { \"name\" : \"s3#\" } ] }", ParallelListIF.class);
		assertEquals("s2?", complexArray.getStrings().get(1).getName());
		assertEquals("s3#", complexArray.getStrings().get(2).getName());
	}
	public static interface ParallelListIF {
		@TwynCollection(value = StringIF.class, parallel = true)
		List<StringIF> getStrings();
	}

	@Test
	public void defaultMethodsCanHaveParameters() throws Exception {
		DefaultParamMethodIF defaultMethod = twyn.read(input("{ \"name\" : \"Java8\" }"), DefaultParamMethodIF.class);
		assertEquals("Hello Java8EXKL", defaultMethod.getDecoratedName("EXKL"));
	}
	public static interface DefaultParamMethodIF {
		String getName();
		default String getDecoratedName(String exclamation) {
			return "Hello " + getName() + exclamation;
		}
	}

	@Test
	public void canMapWithStringKeys() throws Exception {
		MapIF maps = twyn.read("{ \"data\" : { \"k1\" : { \"name\" : \"s1!\" },  \"k2\" : { \"name\" : \"s2?\" }, \"k3\" : { \"name\" : \"s3#\" } } }", MapIF.class);
		assertEquals(3, maps.data().size());
		assertEquals("s1!", maps.data().get("k1").getName());
	}
	public static interface MapIF {
		@TwynCollection(StringIF.class)
		Map<String, StringIF> data();
	}

	@Test
	public void githubExample() throws Exception {
		String json = "{\n" +
				"	\"daughters\" : [ { \"name\" : \"Inara\" }, { \"name\" : \"Kaylee\" }, { \"name\" : \"River\" } ],\n" +
				"	\"daughterNickNames\" : {\n" +
				"		\"Inara\" : { \"nick\" : \"innie\" },\n" +
				"		\"Kaylee\" : { \"nick\" : \"lee\" }\n" +
				"	},\n" +
				"	\"sons\" : [ \"Mal\", \"Wash\" ],\n" +
				"	\"unknowns\" : [ { \"name\" : \"Chtulu\", \"type\" : \"squid\" }, { \"name\" : \"Donald\", \"type\" : \"duck\" } ],\n" +
				"	\"songs\" : [ { \"name\" : \"Come out and play\" }, { \"name\" : \"LAPD\" } ]\n" +
				"}";
		Offspring offspring = twyn.read(json, Offspring.class);
		assertEquals("River", offspring.daughters()[2].getName());
		assertEquals("innie", offspring.daughterNickNames().get("Inara").nick());
		assertEquals("Mal", offspring.sons()[0]);
		assertEquals("squid", offspring.getUnknowns().get(0).type());
		assertEquals(2, offspring.songs().size());
	}
	public static interface Offspring {
		Daughter[] daughters();

		@TwynCollection(Nick.class)
		Map<String, Nick> daughterNickNames();

		String[] sons();

		@TwynCollection(Entity.class)
		List<Entity> getUnknowns();

		@TwynCollection(Song.class)
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
	public void toStringWorksOnProxies() throws Exception {
		String toString = twyn.read(input("{ \"name\" : \"Hello World!\" }"), StringIF.class).toString();
		assertTrue(toString, toString.contains("getName()=Hello World!"));
	}

	@Test
	public void readSameDataManyTimesPerformanceTest() throws Exception {
		for (int i = 0; i < 1_000; i++) {
			githubExample();
		}
	}

	@Test
	public void readSameDataManyTimesPerformanceTest2() throws Exception {
		readSameDataManyTimesPerformanceTest();
	}

	@Test
	public void equalsForValueObjects() throws Exception {
		Entity e1 = twyn.read("{ \"name\" : \"n1\", \"type\" : \"t1\" }", Entity.class);
		Entity e2 = twyn.read("{ \"name\" : \"n2\", \"type\" : \"t1\" }", Entity.class);
		Entity e3 = twyn.read("{ \"name\" : \"n1\", \"type\" : \"t2\" }", Entity.class);

		assertFalse(e1.equals(e2));
		assertFalse(e1.equals(e3));
		assertFalse(e2.equals(e3));

		assertEquals(e1, twyn.read("{ \"name\" : \"n1\", \"type\" : \"t1\" }", Entity.class));
		assertEquals(e2, twyn.read("{ \"name\" : \"n2\", \"type\" : \"t1\" }", Entity.class));
		assertEquals(e3, twyn.read("{ \"name\" : \"n1\", \"type\" : \"t2\" }", Entity.class));
	}

	@Test
	public void hashCodeForValueObjects() throws Exception {
		Entity e1 = twyn.read("{ \"name\" : \"n1\", \"type\" : \"t1\" }", Entity.class);
		Entity e2 = twyn.read("{ \"name\" : \"n2\", \"type\" : \"t1\" }", Entity.class);
		Entity e3 = twyn.read("{ \"name\" : \"n1\", \"type\" : \"t2\" }", Entity.class);

		assertFalse(e1.hashCode() == e2.hashCode());
		assertFalse(e1.hashCode() == e3.hashCode());
		assertFalse(e2.hashCode() == e3.hashCode());

		assertEquals(e1.hashCode(), twyn.read("{ \"name\" : \"n1\", \"type\" : \"t1\" }", Entity.class).hashCode());
		assertEquals(e2.hashCode(), twyn.read("{ \"name\" : \"n2\", \"type\" : \"t1\" }", Entity.class).hashCode());
		assertEquals(e3.hashCode(), twyn.read("{ \"name\" : \"n1\", \"type\" : \"t2\" }", Entity.class).hashCode());

		// We do not support per element array equality
		assertFalse(
				twyn.read(input("{ \"strings\" : [ { \"name\" : \"s1!\" }, { \"name\" : \"s2?\" }, { \"name\" : \"s3#\" } ] }"), ComplexArrayIF.class).hashCode() ==
				twyn.read(input("{ \"strings\" : [ { \"name\" : \"s1!\" }, { \"name\" : \"s2?\" }, { \"name\" : \"s3#\" } ] }"), ComplexArrayIF.class).hashCode());
	}

	@Test
	public void equalsForReferenceObjects() throws Exception {
		ReferenceEntity e1 = twyn.read("{ \"name\" : \"n1\", \"type\" : \"t1\" }", ReferenceEntity.class);
		ReferenceEntity e2 = twyn.read("{ \"name\" : \"n2\", \"type\" : \"t1\" }", ReferenceEntity.class);
		ReferenceEntity e3 = twyn.read("{ \"name\" : \"n1\", \"type\" : \"t2\" }", ReferenceEntity.class);

		assertFalse(e1.equals(e2));
		assertTrue(e1.equals(e3));
		assertFalse(e2.equals(e3));

		assertEquals(e1, twyn.read("{ \"name\" : \"n1\", \"type\" : \"t1\" }", ReferenceEntity.class));
		assertEquals(e2, twyn.read("{ \"name\" : \"n2\", \"type\" : \"t1\" }", ReferenceEntity.class));
		assertEquals(e3, twyn.read("{ \"name\" : \"n1\", \"type\" : \"t2\" }", ReferenceEntity.class));
	}
	public static interface ReferenceEntity {
		@TwynId
		String name();
		String type();
	}

	@Test
	public void hashCodeForReferenceObjects() throws Exception {
		ReferenceEntity e1 = twyn.read("{ \"name\" : \"n1\", \"type\" : \"t1\" }", ReferenceEntity.class);
		ReferenceEntity e2 = twyn.read("{ \"name\" : \"n2\", \"type\" : \"t1\" }", ReferenceEntity.class);
		ReferenceEntity e3 = twyn.read("{ \"name\" : \"n1\", \"type\" : \"t2\" }", ReferenceEntity.class);

		assertNotEquals(e1.hashCode(), e2.hashCode());
		assertEquals(e1.hashCode(), e3.hashCode());
		assertNotEquals(e2.hashCode(), e3.hashCode());

		assertEquals(e1.hashCode(), twyn.read("{ \"name\" : \"n1\", \"type\" : \"t1\" }", ReferenceEntity.class).hashCode());
		assertEquals(e2.hashCode(), twyn.read("{ \"name\" : \"n2\", \"type\" : \"t1\" }", ReferenceEntity.class).hashCode());
		assertEquals(e3.hashCode(), twyn.read("{ \"name\" : \"n1\", \"type\" : \"t2\" }", ReferenceEntity.class).hashCode());
	}

	@Test
	public void canReadComplexSet() throws Exception {
		SetIF complexArray = twyn.read("{ \"strings\" : [ { \"name\" : \"s1!\" }, { \"name\" : \"s2?\" }, { \"name\" : \"s1!\" } ] }", SetIF.class);
		assertEquals(2, complexArray.getStrings().size());
	}
	public static interface SetIF {
		@TwynCollection(StringIF.class)
		Set<StringIF> getStrings();
	}

	@Test(expected = JsonParseException.class)
	public void throwsJsonParseExceptionIfUnableToParseXml() throws Exception {
		twyn.read("{ \"name\" : \"n1\", \"type\" : ERROR }", ReferenceEntity.class);
	}

	@Test(expected = IOException.class)
	public void throwsIOExceptionIfUnderlyingStreamThrowsException() throws Exception {
		InputStream inputStream = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException("Forced error");
			}
		};
		twyn.read(inputStream, ReferenceEntity.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void throwsIllegalArgumentExceptionIfUnableToCreateProxy() throws Exception {
		twyn.read("{ \"name\" : \"n1\", \"type\" : \"test\" }", InterfaceWithNonDefaultMethodWithParameters.class);
	}
	public static interface InterfaceWithNonDefaultMethodWithParameters {
		@TwynId
		String name();
		String type(String param, int i);
	}

	@Test
	public void canSetValue() throws Exception {
		GetSet getSet = twyn.read("{ "
				+ "\"bigDecimal\" : \"1.0\", "
				+ "\"byteArray\" : null, "
				+ "\"bool\" : \"false\" , "
				+ "\"double\" : \"0.1\" , "
				+ "\"float\" : \"0.2\" , "
				+ "\"int\" : \"21\" , "
				+ "\"long\" : \"22\" , "
				+ "\"string\" : \"n1\" "
				+ "}", GetSet.class);

		assertEquals(BigDecimal.valueOf(1.0), getSet.getBigDecimal());
		getSet.setBigDecimal(BigDecimal.valueOf(1.1));
		assertEquals(BigDecimal.valueOf(1.1), getSet.getBigDecimal());

		assertEquals(false, getSet.getBool());
		getSet.setBool(true);
		assertEquals(true, getSet.getBool());

		assertEquals(null, getSet.getByteArray());
		getSet.setByteArray("apa".getBytes());
		assertTrue(Arrays.equals("apa".getBytes(), getSet.getByteArray()));

		assertEquals(0.1d, getSet.getDouble(), 0.001);
		getSet.setDouble(0.2d);
		assertEquals(0.2d, getSet.getDouble(), 0.001);

		assertEquals(0.2f, getSet.getFloat(), 0.001f);
		getSet.setFloat(0.3f);
		assertEquals(0.3f, getSet.getFloat(), 0.001f);

		assertEquals(21, getSet.getInt());
		getSet.setInt(46);
		assertEquals(46, getSet.getInt());

		assertEquals(22L, getSet.getLong());
		getSet.setLong(98L);
		assertEquals(98L, getSet.getLong());

		assertEquals("n1", getSet.getString());
		getSet.string("n2");
		assertEquals("n2", getSet.getString());
	}
	public static interface GetSet {
		BigDecimal getBigDecimal();
		void setBigDecimal(BigDecimal value);

		Boolean getBool();
		void setBool(Boolean value);

		byte[] getByteArray();
		void setByteArray(byte[] data);

		double getDouble();
		void setDouble(double val);

		float getFloat();
		void setFloat(float val);

		int getInt();
		void setInt(int val);

		long getLong();
		void setLong(long val);

		String getString();
		void string(String name);
	}

	@Test
	public void canRetrieveUnderlyingJsonNode() throws Exception {
		Simple string = twyn.read(input("{ \"name\" : \"Hello World!\" }"), Simple.class);
		string.name("Hi there!");
		assertEquals("{\"name\":\"Hi there!\"}", twyn.getJsonNode(string).toString());
	}
	public static interface Simple {
		String name();
		void name(String val);
	}

	@Test
	public void canSetComplexValue() throws Exception {
		ObjectHoldingSetIF complexObject = twyn.read(input("{ \"id\" : { \"val\" : \"2\" } }"), ObjectHoldingSetIF.class);
		assertEquals(2, complexObject.getId().getVal());
		complexObject.setId(new MyId(21));
		assertEquals(21, complexObject.getId().getVal());

	}
	public static interface ObjectHoldingSetIF {
		public MyId getId();
		public void setId(MyId myId);
	}

	@Test
	public void setterCanReturnInstance() throws Exception {
		SelfSimple string = twyn.read(input("{ \"name\" : \"Hello World!\" }"), SelfSimple.class);
		assertEquals("Hola!", string.name("Hola!").name());
	}
	public static interface SelfSimple {
		String name();
		SelfSimple name(String val);
	}

	@Test
	public void canCallToStringOnCollectionWithPrimitiveType() throws Exception {
		StringList stringList = twyn.read("{\"get\" : [\"a\", \"b\"]}", StringList.class);
		stringList.toString();
	}
	public static interface StringList {
		@TwynCollection(String.class)
		List<String> get();
	}

	@Test
	public void canMapArraysToObjects() throws Exception {
		ArrayObject arrayObject = twyn.read("{ \"arr\" : [ 1, \"JS\", 33, \"iCode\" ] }", ArrayObject.class);
		assertEquals(1, arrayObject.arr().index());
		assertEquals("iCode", arrayObject.arr().message());
	}
	public static interface ArrayObject {
		ArrayElement arr();
	}
	public static interface ArrayElement {
		@TwynIndex(0) int index();
		@TwynIndex(3) String message();
	}

	@Test
	public void twoDimensionalArraysCanBeMapped() throws Exception {
		Array2DObject arrayObject = twyn.read("{ \"arr\" : [[ 1, \"JS\", 33, \"iCode\" ], [ 2, \"LS\", 30, \"iChem\" ]] }", Array2DObject.class);
		assertEquals(1, arrayObject.arr()[0].index());
		assertEquals(2, arrayObject.arr()[1].index());
	}
	public static interface Array2DObject {
		ArrayElement[] arr();
	}

	@Test
	public void canParseJsonStartingWithArray() throws Exception {
		ArrayElement[] elements = twyn.read("[[ 1, \"JS\", 33, \"iCode\" ], [ 2, \"LS\", 30, \"iChem\" ]]", ArrayElement[].class);
		assertEquals(1, elements[0].index());
		assertEquals(2, elements[1].index());
	}

	@Test(expected = IllegalArgumentException.class)
	public void nonAnnotatedCollectionMethodResultsInIllegalArgumentException() throws Exception {
		twyn.read("{ \"col\" : [] }", NoAnnotCollectionIF.class);
	}
	public static interface NoAnnotCollectionIF {
		List<ArrayElement> col();
	}

	// Helper methods

	private InputStream input(String string) {
		return new ByteArrayInputStream(string.getBytes());
	}

}