/*
 * Copyright 2015 Joakim Sahlström
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.jsa.twyn.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public interface Cache {
	<T> T get(String key, Supplier<T> supplier);
	void clear(String key);

	class Full implements Cache {
		private final Map<Object, Object> cache = new HashMap<Object, Object>();
		@SuppressWarnings("unchecked")
		@Override
		public <T> T get(String key, Supplier<T> supplier) {
			return (T)cache.computeIfAbsent(key, (k) -> supplier.get());
		}
		@Override
		public void clear(String key) {
			cache.remove(key);
		}
	}

	class FullConcurrent implements Cache {
		private final Map<Object, Object> cache = new ConcurrentHashMap<Object, Object>();
		@SuppressWarnings("unchecked")
		@Override
		public <T> T get(String key, Supplier<T> supplier) {
			return (T)cache.computeIfAbsent(key, (k) -> supplier.get());
		}
		@Override
		public void clear(String key) {
			cache.remove(key);
		}
	}

	class None implements Cache {
		@Override
		public <T> T get(String key, Supplier<T> supplier) {
			return supplier.get();
		}
		@Override
		public void clear(String key) {
			// do nothing
		}
	}

}
