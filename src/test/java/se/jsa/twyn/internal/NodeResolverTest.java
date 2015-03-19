package se.jsa.twyn.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import se.jsa.twyn.TwynIndex;

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
