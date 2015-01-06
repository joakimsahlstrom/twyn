package se.jsa.twyn.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public interface Cache {
	<T> T get(Object key, Supplier<T> supplier);
	void clear();

	class Full implements Cache {
		private final Map<Object, Object> cache = new HashMap<Object, Object>();
		@SuppressWarnings("unchecked")
		@Override
		public <T> T get(Object key, Supplier<T> supplier) {
			return (T)cache.computeIfAbsent(key, (k) -> supplier.get());
		}
		@Override
		public void clear() {
			cache.clear();
		}
	}
	class FullConcurrent implements Cache {
		private final Map<Object, Object> cache = new ConcurrentHashMap<Object, Object>();
		@SuppressWarnings("unchecked")
		@Override
		public <T> T get(Object key, Supplier<T> supplier) {
			return (T)cache.computeIfAbsent(key, (k) -> supplier.get());
		}
		@Override
		public void clear() {
			cache.clear();
		}
	}
	class None implements Cache {
		@Override
		public <T> T get(Object key, Supplier<T> supplier) {
			return supplier.get();
		}
		@Override
		public void clear() {
			// do nothing
		}
	}
}
