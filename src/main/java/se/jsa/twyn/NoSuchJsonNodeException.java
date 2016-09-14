package se.jsa.twyn;

public class NoSuchJsonNodeException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public NoSuchJsonNodeException(String message) {
		super(message);
	}
}
