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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import se.jsa.twyn.TwynCollection;
import se.jsa.twyn.internal.ProxiedInterface.ImplementedMethod;

public enum MethodType implements Predicate<ProxiedInterface.ImplementedMethod> {
	ILLEGAL_NONDEFAULT_METHOD_MORE_THAN_ONE_ARGUMENT(m -> !m.isDefault() && m.getNumParameters() > 1),
	ILLEGAL_COLLECTION_NO_ANNOTATION(m -> m.returnsCollection() && m.getNumParameters() == 0 && !m.hasAnnotation(TwynCollection.class)),

	DEFAULT(m 	-> m.isDefault()),
	ARRAY(m 	-> m.returnsArray() 		&& m.getNumParameters() == 0 && m.returnsArrayOfInterface()),
	LIST(m 		-> m.returns(List.class) 	&& m.getNumParameters() == 0 && m.hasAnnotation(TwynCollection.class)),
	MAP(m 		-> m.returns(Map.class) 	&& m.getNumParameters() == 0 && m.hasAnnotation(TwynCollection.class)),
	SET(m 		-> m.returns(Set.class) 	&& m.getNumParameters() == 0 && m.hasAnnotation(TwynCollection.class)),
	INTERFACE(m -> m.returnsInterface() 	&& m.getNumParameters() == 0),

	SET_VALUE(m -> m.getNumParameters() == 1),

	VALUE(m -> true);

	private Predicate<ImplementedMethod> predicate;

	private MethodType(Predicate<ImplementedMethod> predicate) {
		this.predicate = Objects.requireNonNull(predicate);
	}

	@Override
	public boolean test(ImplementedMethod m) {
		return predicate.test(m);
	}

	public static MethodType getType(ImplementedMethod m) {
		return Stream.of(values())
				.filter(mt -> mt.test(m))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Could not determine MethodType for " + m));
	}

	public static Predicate<ImplementedMethod> GETTER_TYPES_FILTER = any(ARRAY, LIST, MAP, SET, INTERFACE, VALUE);
	public static Predicate<ImplementedMethod> ILLEGAL_TYPES_FILTER = any(ILLEGAL_NONDEFAULT_METHOD_MORE_THAN_ONE_ARGUMENT, ILLEGAL_COLLECTION_NO_ANNOTATION);

	@SafeVarargs
	private static <T> Predicate<T> any(Predicate<T>... predicates) {
		return Stream.of(predicates).reduce(Predicate::or).orElse(p -> false);
	}

}