package se.jsa.twyn;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

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
	
}
