package se.jsa.twyn.internal.datamodel;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by joakim on 2017-02-12.
 */
public interface Node {
    Node get(String name);
    Node get(int index);

    void set(String name, Node node);
    void set(String name, Object value);
    void set(int index, Node value);
    void set(int index, Object value);

    boolean isCollection();

    boolean isContainerNode();

    Stream<Node> streamChildren();

    Stream<Map.Entry<String, Node>> streamFields();
}
