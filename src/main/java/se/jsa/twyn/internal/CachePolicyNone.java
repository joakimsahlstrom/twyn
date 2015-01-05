package se.jsa.twyn.internal;

import java.util.function.Supplier;

public class CachePolicyNone implements CachePolicy {

	@Override
	public <T> T get(Object key, Supplier<T> producer) {
		return producer.get();
	}

}
