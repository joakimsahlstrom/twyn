package se.jsa.twyn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TwynTest {

	private final Twyn twyn;

	public TwynTest(Twyn twyn) {
		this.twyn = twyn;
	}

	@Parameters
	public static Collection<Object[]> twyns() {
		return Arrays.<Object[]>asList(
				new Object[] { Twyn.configurer().withJavaProxies().configure() },
				new Object[] { Twyn.configurer().withClassGeneration().configure() }
				);
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

		@TwynCollection(StringIF.class)
		Set<StringIF> songs();
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

	@Test
	public void toStringWorksOnProxies() throws Exception {
		assertTrue(twyn.read(input("{ \"name\" : \"Hello World!\" }"), StringIF.class).toString().contains("{\"name\":\"Hello World!\"}"));
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

	// Helper methods

	private InputStream input(String string) {
		return new ByteArrayInputStream(string.getBytes());
	}

}
