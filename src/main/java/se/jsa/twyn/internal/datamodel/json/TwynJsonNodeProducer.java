package se.jsa.twyn.internal.datamodel.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import se.jsa.twyn.ReadException;
import se.jsa.twyn.internal.datamodel.Node;
import se.jsa.twyn.internal.datamodel.NodeProducer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

/**
 * Created by joakim on 2017-02-13.
 */
public class TwynJsonNodeProducer implements NodeProducer {

    private ObjectMapper objectMapper;

    public TwynJsonNodeProducer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canMapToPrimitive(Object obj) {
        return BasicJsonTypes.isBasicJsonType(obj.getClass());
    }

    @Override
    public Node mapToNode(Object object) {
        return TwynJsonNode.create(objectMapper.valueToTree(object));
    }

    @Override
    public <T> T readNode(Node resolvedTargetNode, Class<T> valueType) {
        try {
            return objectMapper.treeToValue(((TwynJsonNode) resolvedTargetNode).toJsonNode(), valueType);
        } catch (JsonProcessingException e) {
            throw new ReadException("Could not map node=" + resolvedTargetNode + " to type=" + valueType, e);
        }
    }

    @Override
    public Node read(InputStream inputStream, Class<?> type) {
        try {
            return TwynJsonNode.create(objectMapper.readTree(inputStream));
        } catch (IOException e) {
            throw new ReadException("Could not read data!", e);
        }
    }

    @Override
    public Node read(byte[] data, Class<?> type) {
        try {
            return TwynJsonNode.create(objectMapper.readTree(data));
        } catch (IOException e) {
            throw new ReadException("Could not read data!", e);
        }
    }

    @Override
    public Node read(File file, Class<?> type) {
        try {
            return TwynJsonNode.create(objectMapper.readTree(file));
        } catch (IOException e) {
            throw new ReadException("Could not read data!", e);
        }
    }

    @Override
    public Node read(Reader reader, Class<?> type) {
        try {
            return TwynJsonNode.create(objectMapper.readTree(reader));
        } catch (IOException e) {
            throw new ReadException("Could not read data!", e);
        }
    }

    @Override
    public Node read(String string, Class<?> type) {
        try {
            return TwynJsonNode.create(objectMapper.readTree(string));
        } catch (IOException e) {
            throw new ReadException("Could not read data!", e);
        }
    }

    @Override
    public Node read(URL url, Class<?> type) {
        try {
            return TwynJsonNode.create(objectMapper.readTree(url));
        } catch (IOException e) {
            throw new ReadException("Could not read data!", e);
        }
    }
}
