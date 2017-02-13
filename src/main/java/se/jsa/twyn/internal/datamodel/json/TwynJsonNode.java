package se.jsa.twyn.internal.datamodel.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import se.jsa.twyn.internal.datamodel.Node;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by joakim on 2017-02-13.
 */
public class TwynJsonNode implements Node {

    private JsonNode jsonNode;

    private TwynJsonNode(JsonNode jsonNode) {
        this.jsonNode = Objects.requireNonNull(jsonNode);
    }

    public static TwynJsonNode create(JsonNode node) {
        return node == null ? null : new TwynJsonNode(node);
    }

    JsonNode toJsonNode() {
        return jsonNode;
    }

    @Override
    public Node get(String name) {
        return TwynJsonNode.create(jsonNode.get(name));
    }

    @Override
    public Node get(int index) {
        return TwynJsonNode.create(jsonNode.get(index));
    }

    @Override
    public void set(String name, Node node) {
        ((ObjectNode) jsonNode).set(name, ((TwynJsonNode) node).jsonNode);
    }

    @Override
    public void set(String name, Object value) {
        switch (BasicJsonTypes.get(value.getClass())) {
            case BIG_DECIMAL: 	((ObjectNode) jsonNode).put(name, (BigDecimal)value); break;
            case BOOLEAN:		((ObjectNode) jsonNode).put(name, (Boolean)value); break;
            case BYTE_ARRAY:	((ObjectNode) jsonNode).put(name, (byte[])value); break;
            case DOUBLE:		((ObjectNode) jsonNode).put(name, (Double)value); break;
            case FLOAT:			((ObjectNode) jsonNode).put(name, (Float)value); break;
            case INTEGER:		((ObjectNode) jsonNode).put(name, (Integer) value); break;
            case LONG:			((ObjectNode) jsonNode).put(name, (Long)value); break;
            case STRING:		((ObjectNode) jsonNode).put(name, (String)value); break;
            default: throw new IllegalArgumentException("Could not map value type=" + value.getClass());
        }
    }

    @Override
    public void set(int index, Node node) {
        ((ArrayNode) jsonNode).set(index, ((TwynJsonNode) node).jsonNode);
    }

    @Override
    public void set(int index, Object value) {
        switch (BasicJsonTypes.get(value.getClass())) {
            case BIG_DECIMAL: 	((ArrayNode) jsonNode).insert(index, (BigDecimal)value); break;
            case BOOLEAN:		((ArrayNode) jsonNode).insert(index, (Boolean)value); break;
            case BYTE_ARRAY:	((ArrayNode) jsonNode).insert(index, (byte[])value); break;
            case DOUBLE:		((ArrayNode) jsonNode).insert(index, (Double)value); break;
            case FLOAT:			((ArrayNode) jsonNode).insert(index, (Float)value); break;
            case INTEGER:		((ArrayNode) jsonNode).insert(index, (Integer) value); break;
            case LONG:			((ArrayNode) jsonNode).insert(index, (Long)value); break;
            case STRING:		((ArrayNode) jsonNode).insert(index, (String)value); break;
            default: throw new IllegalArgumentException("Could not map value type=" + value.getClass());
        }
    }

    @Override
    public boolean isCollection() {
        return jsonNode.isArray();
    }

    @Override
    public boolean isContainerNode() {
        return jsonNode.isContainerNode();
    }

    @Override
    public Stream<Node> streamChildren() {
        return StreamSupport.stream(jsonNode.spliterator(), false).map(TwynJsonNode::new);
    }

    @Override
    public Stream<Map.Entry<String, Node>> streamFields() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(jsonNode.fields(), 0), false)
                .map(e -> new AbstractMap.SimpleEntry<String, Node>(e.getKey(), TwynJsonNode.create(e.getValue())));
    }

    @Override
    public String toString() {
        return jsonNode.toString();
    }
}
