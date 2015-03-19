package se.jsa.twyn;

import java.util.List;

public interface StringHolder {

	@TwynCollection(String.class) List<String> string();

}
