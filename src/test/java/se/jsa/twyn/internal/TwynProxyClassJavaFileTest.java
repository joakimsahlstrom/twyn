package se.jsa.twyn.internal;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import se.jsa.twyn.TwynCollection;
import se.jsa.twyn.TwynIndex;
import se.jsa.twyn.TwynTest;
import se.jsa.twyn.TwynTest.StringIF;

import com.fasterxml.jackson.databind.ObjectMapper;

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

		@TwynCollection(value = Nick.class, parallel = true)
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
//	@Ignore
	public void canReadComplexSet() throws Exception {
//		System.out.println(getCode(SetIF.class).getCode());
		System.out.println(getCode(TwynTest.ObjectHoldingSetIF.class).getCode());
//		System.out.println(getCode(ArrayElement.class).getCode());
	}
	public static interface SetIF {
		@TwynCollection(StringIF.class)
		Set<StringIF> getStrings();
	}

	public static interface ArrayElement {
		@TwynIndex(0) int index();
		@TwynIndex(3) String message();
	}

	private TwynProxyClassJavaFile getCode(Class<?> type) throws IOException, URISyntaxException {
		return TwynProxyClassJavaFile.create(type, TwynProxyClassJavaTemplates.create(), new TwynContext(new ObjectMapper(), new TwynProxyClassBuilder(), () -> new Cache.None(), true));
	}

}
