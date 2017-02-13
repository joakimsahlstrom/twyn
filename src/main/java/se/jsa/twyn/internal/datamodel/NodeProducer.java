package se.jsa.twyn.internal.datamodel;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.function.Supplier;

/**
 * Created by joakim on 2017-02-12.
 */
public interface NodeProducer {

    public boolean canMapToPrimitive(Object obj);
    public Node mapToNode(Object object);

    <T> T readNode(Node resolvedTargetNode, Class<T> valueType);

    Node read(InputStream inputStream);
    Node read(byte[] data);
    Node read(File file);
    Node read(Reader reader);
    Node read(String string);
    Node read(URL url);
}
