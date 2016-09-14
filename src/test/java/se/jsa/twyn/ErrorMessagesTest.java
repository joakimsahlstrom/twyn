/*
 * Copyright 2016 Joakim Sahlström
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
package se.jsa.twyn;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.JsonProcessingException;

@RunWith(Parameterized.class)
public class ErrorMessagesTest {

	private final Twyn twyn;

	public ErrorMessagesTest(Twyn twyn) {
		this.twyn = twyn;
	}
	
	@Parameters
	public static Collection<Object[]> twyns() {
		return Arrays.<Object[]>asList(
				new Object[] { Twyn.configurer().withJavaProxies().configure() },
				new Object[] { Twyn.configurer().withClassGeneration().configure() }
				);
	}
	
	public interface Person {
		Name name();
	}
	
	public interface Persons {
		Person[] persons();
	}
	
	public interface PersonMap {
		@TwynCollection(Person.class)
		Map<String, Person> persons();
	}
	
	public interface TypedPersonMap {
		@TwynCollection(value = Person.class, keyType = Key.class)
		Map<Key, Person> persons();
	}
	
	public static class Key {
		String key;
		public Key(String key) {
			this.key = key;
		}
	}
	
	public interface Name {
		String firstName();
		String lastName();
	}

	@Test(expected = NoSuchJsonNodeException.class)
	public void missingNode() throws JsonProcessingException, IOException {
		twyn.read("{  }", Person.class).name().firstName();
	}
	
	@Test(expected = NoSuchJsonNodeException.class)
	public void missingValue() throws Exception {
		twyn.read("{ \"name\": { \"banana\": \"brown\" } }", Person.class).name().firstName();
	}
	
	@Test(expected = NoSuchJsonNodeException.class)
	public void notAStruct() throws Exception {
		twyn.read("{ \"name\": \"horse\" }", Person.class).name().firstName();
	}

	@Test(expected = BadJsonNodeTypeException.class)
	public void noArray() throws Exception {
		twyn.read("{ \"persons\": \"horse\" }", Persons.class).persons()[0].name();
	}
	
	@Test(expected = BadJsonNodeTypeException.class)
	public void noMap() throws Exception {
		twyn.read("{ \"persons\": \"oh.\" }", PersonMap.class).persons();
	}
	
	@Test(expected = BadJsonNodeTypeException.class)
	public void noTypedMap() throws Exception {
		twyn.read("{ \"persons\": \"oh.\" }", TypedPersonMap.class).persons();
	}
	
}
