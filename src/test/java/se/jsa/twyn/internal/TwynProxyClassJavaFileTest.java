package se.jsa.twyn.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;

import se.jsa.twyn.TwynCollection;


public class TwynProxyClassJavaFileTest {

	@Test
	@Ignore("To be used for manual verification")
	public void generatesCorrectCode() throws Exception {
		System.out.println(TwynProxyClassJavaFile.create(Offspring.class, TwynProxyClassTemplates.create(), new TwynContext(new ObjectMapper(), new TwynProxyClassBuilder(), () -> new Cache.None(), true))
			.getCode());
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

}
