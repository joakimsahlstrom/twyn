package se.jsa.twyn;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

public class ErrorMessagesTest {

	public interface Person {
		Name name();
	}
	
	public interface Name {
		String firstName();
		String lastName();
	}

	Twyn twyn = Twyn.forTest();
	//Twyn twyn = Twyn.configurer().withClassGeneration().configure();
	
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

}
