package se.jsa.twyn.internal.datamodel;

/**
 * Created by joakim on 2017-02-12.
 */
public interface Node {
    Node get(String name);
    Node get(Integer index);

    void put(String name, Node node);
    void set(Integer index, Node value);
}
