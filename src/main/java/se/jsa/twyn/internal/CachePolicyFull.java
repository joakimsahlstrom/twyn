package se.jsa.twyn.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class CachePolicyFull implements CachePolicy {
	private final Map<Object, Object> cache = new ConcurrentHashMap<Object, Object>();

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Object key, Supplier<T> producer) {
		return (T)cache.computeIfAbsent(key, (k) -> producer.get());
	}
}
