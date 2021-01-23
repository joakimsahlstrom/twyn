package se.jsa.twyn.internal.datamodel;

import java.util.stream.Stream;

/**
 * Created by joakim on 2017-02-19.
 */
public interface CollectionNode extends Node {

    Node get(int index);

    void set(int index, Node value);
    void set(int index, Object value);

    default boolean isCollection() {
        return true;
    }

    default boolean isContainerNode() {
        return true;
    }

    Stream<Node> streamChildren();

}
