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
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		Map<String, Person> persons();
	}
	
	public interface PersonList {
		List<Person> persons();
	}
	
	public interface PersonSet {
		Set<Person> persons();
	}

	public interface TypedPersonMap {
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

	@Test//(expected = NoSuchNodeException.class) // Spec has changed!
	public void missingNode() throws JsonProcessingException, IOException {
		twyn.read("{  }", Person.class).name();
	}

	@Test//(expected = NoSuchNodeException.class) // Spec has changed!
	public void missingValue() throws Exception {
		twyn.read("{ \"name\": { \"banana\": \"brown\" } }", Person.class).name().firstName();
	}

	@Test(expected = BadNodeTypeException.class)
	public void notAStruct() throws Exception {
		twyn.read("{ \"name\": \"horse\" }", Person.class).name();
	}

	@Test(expected = BadNodeTypeException.class)
	public void noArray() throws Exception {
		twyn.read("{ \"persons\": \"horse\" }", Persons.class).persons();
	}
	
	@Test(expected = BadNodeTypeException.class)
	public void noList() throws Exception {
		twyn.read("{ \"persons\": \"horse\" }", PersonList.class).persons();
	}
	
	@Test(expected = BadNodeTypeException.class)
	public void noSet() throws Exception {
		twyn.read("{ \"persons\": \"horse\" }", PersonSet.class).persons();
	}

	@Test(expected = BadNodeTypeException.class)
	public void noMap() throws Exception {
		twyn.read("{ \"persons\": \"oh.\" }", PersonMap.class).persons();
	}

	@Test(expected = BadNodeTypeException.class)
	public void noTypedMap() throws Exception {
		twyn.read("{ \"persons\": \"oh.\" }", TypedPersonMap.class).persons();
	}

	@Test(expected = BadNodeTypeException.class)
	public void directArray() throws Exception {
		twyn.read("{ \"persons\": \"oh.\" }", Person[].class)[0].name();
	}
	
}
