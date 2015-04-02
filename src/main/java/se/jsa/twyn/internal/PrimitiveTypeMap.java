package se.jsa.twyn.internal;

import javax.lang.model.type.PrimitiveType;

public class PrimitiveTypeMap {

	public static Class<?> toPrimitive(PrimitiveType primitiveType) {
		switch (primitiveType.getKind()) {
		case BOOLEAN: 	return Boolean.TYPE;
		case BYTE: 		return Byte.TYPE;
		case CHAR: 		return Character.TYPE;
		case DOUBLE: 	return Double.TYPE;
		case FLOAT: 	return Float.TYPE;
		case INT: 		return Integer.TYPE;
		case LONG: 		return Long.TYPE;
		case SHORT: 	return Short.TYPE;
		default:
			throw new IllegalArgumentException("Cannot map to primitive: " + primitiveType.getKind());
		}
	}

}
