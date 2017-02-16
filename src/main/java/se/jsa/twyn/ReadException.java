package se.jsa.twyn;

import java.io.IOException;

/**
 * Created by joakim on 2017-02-13.
 */
public class ReadException extends RuntimeException {

    public ReadException(String message, Exception e) {
        super(message, e);
    }
}
