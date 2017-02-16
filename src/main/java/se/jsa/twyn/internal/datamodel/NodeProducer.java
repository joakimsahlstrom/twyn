package se.jsa.twyn.internal.datamodel;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

/**
 * Created by joakim on 2017-02-12.
 */
public interface NodeProducer {

    public boolean canMapToPrimitive(Object obj);
    public Node mapToNode(Object object);

    <T> T readNode(Node resolvedTargetNode, Class<T> valueType);

    Node read(InputStream inputStream, Class<?> type);
    Node read(byte[] data, Class<?> type);
    Node read(File file, Class<?> type);
    Node read(Reader reader, Class<?> type);
    Node read(String string, Class<?> type);
    Node read(URL url, Class<?> type);
}
