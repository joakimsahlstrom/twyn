package se.jsa.twyn;

public class BadJsonNodeTypeException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public BadJsonNodeTypeException(String message) {
		super(message);
	}
}
