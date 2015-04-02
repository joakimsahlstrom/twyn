package se.jsa.twyn;

import java.util.List;

public interface InterfaceHolder {

	@TwynCollection(String.class) List<String> string();

	public static interface Inner {
		String getName();
	}

}
