package se.jsa.twyn.internal;

class TwynUtil {

	// Getter helpers

	private static final String[] GET_PREFIXES = new String[] { "get", "is" };
	public static String decodeJavaBeanGetName(String name) {
		return resolve(name, GET_PREFIXES);
	}

	/// Setter helpers

	private static final String[] SET_PREFIXES = new String[] { "set" };
	public static String decodeJavaBeanSetName(String name) {
		return resolve(name, SET_PREFIXES);
	}

	// private helper methods

	private static String resolve(String name, String[] prefixes) {
		for (String prefix : prefixes) {
			int prefixLength = prefix.length();
			if (name.startsWith(prefix) && name.length() > prefixLength && Character.isUpperCase(name.charAt(prefixLength))) {
				return name.substring(prefixLength, prefixLength + 1).toLowerCase() + name.substring(prefixLength + 1);
			}
		}
		return name;
	}
}
