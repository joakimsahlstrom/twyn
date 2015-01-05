package se.jsa.twyn.internal;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import se.jsa.twyn.TwynCollection;

public class TwynProxyClassBuilderTest {

	private final TwynProxyClassBuilder builder = new TwynProxyClassBuilder();
	private final TwynContext twynContext = new TwynContext(new ObjectMapper(), builder, () -> new Cache.None());

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
		@TwynCollection(StringIF.class)
		List<StringIF> getStrings();
	}

}
