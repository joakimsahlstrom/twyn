package se.jsa.twyn.internal;

class TwynUtil {
	private static final String[] PREFIXES = new String[] { "get", "is" };
	public static String decodeJavaBeanName(String name) {
		for (String prefix : PREFIXES) {
			int prefixLength = prefix.length();
			if (name.startsWith(prefix) && name.length() > prefixLength && Character.isUpperCase(name.charAt(prefixLength))) {
				return name.substring(prefixLength, prefixLength + 1).toLowerCase() + name.substring(prefixLength + 1);
			}
		}
		return name;
	}
}
