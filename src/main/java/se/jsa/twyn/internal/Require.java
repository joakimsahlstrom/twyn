package se.jsa.twyn.internal;

import java.util.function.Supplier;

public class Require {

	public static <T> T notNull(T value, Supplier<? extends RuntimeException> s) {
		if (value == null) {
			throw s.get();
		} else {
			return value;
		}
	}
	
	public static void that(boolean c, Supplier<? extends RuntimeException> s) {
		if (!c) {
			throw s.get();
		}
	}
	
}
