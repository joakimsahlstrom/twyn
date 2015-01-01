package se.jsa.twyn;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

import se.jsa.twyn.Twyn;


public class TwynTest {

	private Twyn twyn = new Twyn();
	
	@Test
	public void canReadString() throws Exception {
		StringIF string = twyn.read(StringIF.class, input("{ \"name\" : \"Hello World!\" }"));
		assertEquals("Hello World!", string.getName());
	}
	public static interface StringIF { String getName(); };
	
	@Test
	public void canReadAllJsonTypes() throws Exception {
		TypesIF types = twyn.read(TypesIF.class, input(
				"{ "
				+ "\"i\" : \"1\", \"integer\" : \"2\", "
				+ "\"b\" : \"false\", \"boolean\" : \"true\", "
				+ "\"d\" : \"1.01\", \"double\" : \"1.02\", "
				+ "\"l\" : \"1000000000000\", \"long\" : \"1000000000001\" "
				+ "}"));
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
		DefaultMethodIF defaultMethod = twyn.read(DefaultMethodIF.class, input("{ \"name\" : \"Java8\" }"));
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
		ComplexIF complex = twyn.read(ComplexIF.class, input("{ \"stringIF\" : { \"name\" : \"complex\" } }"));
		assertEquals("complex", complex.getStringIF().getName());
	}
	public static interface ComplexIF {
		StringIF getStringIF();
	}
	
	@Test
	public void canResolveComplexObjects() throws Exception {
		ObjectHoldingIF complexObject = twyn.read(ObjectHoldingIF.class, input("{ \"id\" : { \"val\" : \"2\" } }"));
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
		PrimitiveArrayIF primitiveArray = twyn.read(PrimitiveArrayIF.class, input("{ \"data\" : [ \"1\", \"2\", \"97\" ] }"));
		assertEquals(97, primitiveArray.getData()[2]);
	}
	public static interface PrimitiveArrayIF {
		int[] getData();
	}
	
	@Test
	public void canReadComplexArrays() throws Exception {
		ComplexArrayIF complexArray = twyn.read(ComplexArrayIF.class, input("{ \"strings\" : [ { \"name\" : \"s1!\" }, { \"name\" : \"s2?\" }, { \"name\" : \"s3#\" } ] }"));
		assertEquals("s2?", complexArray.getStrings()[1].getName());
		assertEquals("s3#", complexArray.getStringList().get(2).getName());
	}
	public static class StringIFList {
		private StringIF[] arr;
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
		IsIF is = twyn.read(IsIF.class, input("{ \"ok\" : \"true\" }"));
		assertTrue(is.isOk());
	}
	public static interface IsIF {
		boolean isOk();
	}
	
	@Test
	public void canReadNonJavaBeanField() throws Exception {
		ShortNameIF shortName = twyn.read(ShortNameIF.class, input("{ \"ok\" : \"true\" }"));
		assertTrue(shortName.ok());
	}
	public static interface ShortNameIF {
		boolean ok();
	}
	
	@Test
	public void returnsNullForNullValue() throws Exception {
		StringIF nulled = twyn.read(StringIF.class, input("{ \"name\" : null }"));
		assertNull(nulled.getName());
	}
	
	@Test(expected = NullPointerException.class)
	public void throwsExceptionIfValueIsMissing() throws Exception {
		StringIF missing = twyn.read(StringIF.class, input("{ }"));
		missing.getName();
	}
	
	private InputStream input(String string) {
		return new ByteArrayInputStream(string.getBytes());
	}
	
}
