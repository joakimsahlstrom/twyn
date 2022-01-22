/*
 * Copyright 2015 Joakim Sahlström
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.jsa.twyn.internal;

import se.jsa.twyn.TwynProxyException;
import se.jsa.twyn.internal.datamodel.CollectionNode;
import se.jsa.twyn.internal.datamodel.ContainerNode;
import se.jsa.twyn.internal.datamodel.Node;
import se.jsa.twyn.internal.datamodel.NodeProducer;
import se.jsa.twyn.internal.proxy.TwynProxyBuilder;
import se.jsa.twyn.internal.proxy.cg.TwynProxyClassBuilder;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class TwynContext {

    private final NodeProducer nodeProducer;
    private final TwynProxyBuilder proxyBuilder;
    private final Supplier<Cache> cacheSupplier;
    private final boolean debug;
    private final IdentityMethods identityMethods = new IdentityMethods();

    public TwynContext(NodeProducer nodeProducer, TwynProxyBuilder proxyBuilder, Supplier<Cache> cacheSupplier, boolean debug) {
        this.nodeProducer = nodeProducer;
        this.proxyBuilder = Objects.requireNonNull(proxyBuilder);
        this.cacheSupplier = Objects.requireNonNull(cacheSupplier);
        this.debug = debug;
    }

    public <T> T proxy(Node node, Class<T> type) {
        try {
            return proxyBuilder.buildProxy(type, this, node);
        } catch (Exception e) {
            throw new TwynProxyException("Could not create Twyn proxy", e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Object proxyArray(CollectionNode node, Class<T> componentType) {
        List<T> result = proxyCollection(componentType, node, Collectors.toList());
        return result.toArray((T[]) Array.newInstance(componentType, result.size()));
    }

    public <T, A, R> R proxyCollection(Class<T> componentType, CollectionNode node, Collector<T, A, R> collector) {
        return node.streamChildren()
                .map((n) -> {
                    try {
                        return componentType.isInterface() ? proxy(n, componentType) : readValue(n, componentType);
                    } catch (Exception e) {
                        throw new TwynProxyException("Could not proxy collection " + n + " of type " + componentType, e);
                    }
                })
                .collect(collector);
    }

    public <K, V> Map<K, V> proxyMap(Class<K> keyType, Class<V> valueType, ContainerNode node) {
        return (Map<K, V>) node.streamFields()
                .collect(Collectors.toMap(
                        e -> readMapKey(e.getKey(), keyType),
                        e -> proxy(e.getValue(), valueType)));
    }

    private Object readMapKey(String key, Class<?> keyType) {
        if (keyType.equals(String.class)) {
            return key;
        } else {
            try {
                return keyType.getConstructor(String.class).newInstance(key);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                throw new TwynProxyException("Could not create map keyType=" + keyType + " from key=" + key, e);
            }
        }
    }

    public <T> T readValue(Node resolvedTargetNode, Class<T> valueType) throws IOException {
        return nodeProducer.readNode(resolvedTargetNode, valueType);
    }

    public Node writeValue(Object object) {
        return nodeProducer.mapToNode(object);
    }

    public Cache createCache() {
        return cacheSupplier.get();
    }

    public IdentityMethods getIdentityMethods() {
        return identityMethods;
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    public String toString() {
        return "TwynContext [nodeProducer=" + nodeProducer + ", proxyBuilder=" + proxyBuilder + "]";
    }

    public TwynContext precompile(Set<Class<?>> precompiledTypes) {
        if (proxyBuilder instanceof TwynProxyClassBuilder) {
            ((TwynProxyClassBuilder) proxyBuilder).precompile(precompiledTypes, this);
        }
        return this;
    }

    public NodeProducer getNodeProducer() {
        return nodeProducer;
    }
}
