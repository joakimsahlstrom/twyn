package se.jsa.twyn.internal;

import java.util.function.Supplier;

public interface CachePolicy {
	<T> T get(Object key, Supplier<T> producer);
}
