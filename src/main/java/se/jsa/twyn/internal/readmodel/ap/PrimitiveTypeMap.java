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
package se.jsa.twyn.internal.readmodel.ap;

import javax.lang.model.type.PrimitiveType;

class PrimitiveTypeMap {

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
