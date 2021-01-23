package se.jsa.twyn.internal.datamodel;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by joakim on 2017-02-12.
 */
public interface Node {

    default boolean isCollection() {
        return false;
    }

    default boolean isContainerNode() {
        return false;
    }

}
