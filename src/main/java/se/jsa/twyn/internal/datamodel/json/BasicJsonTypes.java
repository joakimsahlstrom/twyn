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
package se.jsa.twyn.internal.datamodel.json;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

enum BasicJsonTypes implements Predicate<Class<?>> {
	BIG_DECIMAL(BigDecimal.class),
	BOOLEAN(Boolean.class),
	BYTE_ARRAY(byte[].class),
	DOUBLE(Double.class),
	FLOAT(Float.class),
	INTEGER(Integer.class),
	LONG(Long.class),
	STRING(String.class),
	;

	private final Class<?> type;

	private BasicJsonTypes(Class<?> type) {
		this.type = Objects.requireNonNull(type);
	}

	@Override
	public boolean test(Class<?> t) {
		return type.equals(t);
	}

	public static boolean isBasicJsonType(Class<?> type) {
		return typeStream().anyMatch(bjt -> bjt.test(type));
	}

	public static BasicJsonTypes get(Class<?> type) {
		return typeStream().filter(bjt -> bjt.test(type))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Not a basic Json type: " + type));
	}

	private static Stream<BasicJsonTypes> typeStream() {
		return Stream.of(BasicJsonTypes.values());
	}
}
