package se.jsa.twyn.internal;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public enum BasicJsonTypes implements Predicate<Class<?>> {
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
		return typeStream().filter(bjt -> bjt.test(type)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Not a basic Json type: " + type));
	}

	private static Stream<BasicJsonTypes> typeStream() {
		return Arrays.asList(BasicJsonTypes.values()).stream();
	}
}
