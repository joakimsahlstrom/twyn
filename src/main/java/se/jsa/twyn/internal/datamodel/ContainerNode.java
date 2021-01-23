package se.jsa.twyn.internal.datamodel;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by joakim on 2017-02-19.
 */
public interface ContainerNode extends Node {

    Node get(String name);

    void set(String name, Node node);
    void set(String name, Object value);

    Stream<Map.Entry<String, Node>> streamFields();

    default boolean isContainerNode() {
        return true;
    }

}
