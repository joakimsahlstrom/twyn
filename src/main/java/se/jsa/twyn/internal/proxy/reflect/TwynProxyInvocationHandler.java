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
package se.jsa.twyn.internal.proxy.reflect;

import se.jsa.twyn.TwynProxyException;
import se.jsa.twyn.internal.*;
import se.jsa.twyn.internal.datamodel.CollectionNode;
import se.jsa.twyn.internal.datamodel.ContainerNode;
import se.jsa.twyn.internal.datamodel.Node;
import se.jsa.twyn.internal.proxy.common.NodeResolver;
import se.jsa.twyn.internal.proxy.common.TwynUtil;
import se.jsa.twyn.internal.readmodel.ImplementedMethod;
import se.jsa.twyn.internal.readmodel.reflect.ImplementedMethodMethod;
import se.jsa.twyn.internal.readmodel.reflect.ProxiedInterfaceClass;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

class TwynProxyInvocationHandler implements InvocationHandler, NodeSupplier {
    private static final Object[] NO_ARGS = new Object[]{};

    private final Node node;
    private final TwynContext twynContext;
    private final ProxiedInterfaceClass implementedType;
    private final Cache cache;
    private final NodeResolver nodeResolver;
    private final DefaultMethodLookup defaultMethodLookup;

    public TwynProxyInvocationHandler(Node node, TwynContext twynContext, ProxiedInterfaceClass implementedType) {
        this.node = node;
        this.twynContext = Objects.requireNonNull(twynContext);
        this.implementedType = Objects.requireNonNull(implementedType);
        this.cache = Objects.requireNonNull(twynContext.createCache());
        this.nodeResolver = NodeResolver.getResolver(implementedType);
        this.defaultMethodLookup = DefaultMethodLookup.create();
    }

    public static TwynProxyInvocationHandler create(Node node, TwynContext twynContext, ProxiedInterfaceClass implementedType) throws Exception {
        return new TwynProxyInvocationHandler(node, twynContext, implementedType);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (hasSignature(method, "toString")) {
            return toString();
        } else if (hasSignature(method, "equals", Object.class)) {
            return equals(args[0]);
        } else if (hasSignature(method, "hashCode")) {
            return hashCode();
        }

        ImplementedMethod implementedMethod = ImplementedMethod.of(method);
        switch (MethodType.getType(implementedMethod)) {
            case DEFAULT:
                return callDefaultMethod(proxy, method, args);
            case ARRAY:
                return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> innerArrayProxy(method));
            case LIST:
                return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> innerCollectionProxy(method, Collectors.toList()));
            case SET:
                return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> innerCollectionProxy(method, Collectors.toSet()));
            case MAP:
                return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> innerMapProxy(method));
            case INTERFACE:
                return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> innerProxy(method));
            case VALUE:
                return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> resolveValue(method));
            case OPTIONAL:
                return cache.get(TwynUtil.decodeJavaBeanName(method.getName()), () -> resolveOptional(method));
            case SET_VALUE:
                return setValue(proxy, method, args);
            default:
                throw ErrorFactory.proxyValidationError(implementedType, implementedMethod).get();
        }
    }

    private boolean hasSignature(Method method, String name, Class<?>... params) {
        return method.getName().equals(name) && Arrays.deepEquals(method.getParameterTypes(), params);
    }

    private Object callDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> declaringClass = method.getDeclaringClass();

        return MethodHandles.lookup()
                .findSpecial(declaringClass,
                        method.getName(),
                        java.lang.invoke.MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                        declaringClass)
                .bindTo(proxy)
                .invokeWithArguments(args);
    }

    @SuppressWarnings("unchecked")
    private <T, A, R> R innerCollectionProxy(Method method, Collector<T, A, R> collector) {
        ImplementedMethod implementedMethod = ImplementedMethod.of(method);
        String returnTypeParameterTypeCanonicalName = implementedMethod.getReturnTypeParameterTypeCanonicalName(0);
        return tryResolveTargetGetNode(method).map(node -> {
            Require.that(node.isCollection(), ErrorFactory.proxyCollectionNotCollectionType(returnTypeParameterTypeCanonicalName, method, this.node));
            return twynContext.proxyCollection((Class<T>) implementedMethod.getReturnTypeParameterType(0),
                    (CollectionNode) node,
                    collector);
        }).orElseGet(() -> collector.finisher().apply(collector.supplier().get()));
    }

    private Object innerMapProxy(Method method) {
        ImplementedMethod implementedMethod = ImplementedMethod.of(method);
        Class<?> valueComponentType = implementedMethod.getReturnTypeParameterType(1);
        Class<?> keyType = implementedMethod.getReturnTypeParameterType(0);

        return tryResolveTargetGetNode(method).map(node -> {
            Require.that(node.isContainerNode(), ErrorFactory.innerMapProxyNoMapStructure(method, node));
            return twynContext.proxyMap(keyType, valueComponentType, ContainerNode.class.cast(node));
        }).orElseGet(Collections::emptyMap);
    }

    private <T> Object innerArrayProxy(Method method) {
        @SuppressWarnings("unchecked")
        Class<T> componentType = (Class<T>) method.getReturnType().getComponentType();
        return tryResolveTargetGetNode(method).map(node -> {
            Require.that(node.isCollection(), ErrorFactory.proxyArrayNodeNotCollectionType(componentType, method, this.node));
            return twynContext.proxyArray((CollectionNode) node, componentType);
        }).orElseGet(() -> Array.newInstance(componentType, 0));
    }

    private Object innerProxy(Method method) {
        return tryResolveTargetGetNode(method).map(node -> {
            Require.that(node.isContainerNode(), ErrorFactory.innerProxyNoStruct(method, node));
            return twynContext.proxy(node, method.getReturnType());
        }).orElse(null);
    }

    private Object setValue(Object proxy, Method method, Object[] args) {
        if (twynContext.getNodeProducer().canMapToPrimitive(args[0])) {
            nodeResolver.setNode(ImplementedMethod.of(method), node, args[0]);
        } else {
            nodeResolver.setNode(ImplementedMethod.of(method), node, twynContext.writeValue(args[0]));
        }
        cache.clear(TwynUtil.decodeJavaBeanName(method.getName()));
        return proxy;
    }

    private Object resolveValue(Method method) {
        return tryResolveTargetGetNode(method).map(node -> {
            try {
                return (Object) twynContext.readValue(node, method.getReturnType());
            } catch (IOException e) {
                throw new TwynProxyException("Could not resolve value for node " + node + ". Wanted type: " + method.getReturnType(), e);
            }
        }).orElseGet(() -> ImplementedMethod.of(method).returnsArray() ? Array.newInstance(method.getReturnType().getComponentType(), 0) : null);
    }

    private Object resolveOptional(Method method) {
        ImplementedMethod implementedMethod = ImplementedMethod.of(method);
        return implementedMethod.getReturnTypeParameterType(0).isInterface()
                ? resolveOptionalInterface(method, implementedMethod)
                : resolveOptionalValue(method, implementedMethod);
    }

    private Object resolveOptionalInterface(Method method, ImplementedMethod implementedMethod) {
        return tryResolveTargetGetNode(method).map(node -> {
            Require.that(node.isContainerNode(), ErrorFactory.innerProxyNoStruct(method.getName(), implementedMethod.getReturnTypeParameterTypeCanonicalName(0), node));
            return Optional.of(twynContext.proxy(node, implementedMethod.getReturnTypeParameterType(0)));
        }).orElse(Optional.empty());
    }

    private Object resolveOptionalValue(Method method, ImplementedMethod implementedMethod) {
        return tryResolveTargetGetNode(method).<Object>map(node -> {
            try {
                return Optional.of(twynContext.readValue(node, implementedMethod.getReturnTypeParameterType(0)));
            } catch (IOException e) {
                throw new TwynProxyException("Could not resolve value for node " + node + ". Wanted type: " + implementedMethod.getReturnTypeParameterType(0), e);
            }
        }).orElse(Optional.empty());
    }

    private Optional<Node> tryResolveTargetGetNode(Method method) {
        return Optional.ofNullable(nodeResolver.resolveNode(ImplementedMethod.of(method), node));
    }

    @Override
    public Node getNode() {
        return node;
    }

    @Override
    public String toString() {
        return "TwynProxyInvocationHandler<" + implementedType.getSimpleName() + "> [" +
                twynContext.getIdentityMethods().getIdentityMethods(implementedType)
                        .map((m) -> {
                            try {
                                return m.getName() + "()=" + this.invoke(null, getMethod(m), NO_ARGS);
                            } catch (Throwable e) {
                                throw new TwynProxyException("Could not call method " + m + " for toString calculation.", e);
                            }
                        })
                        .reduce(null, (s1, s2) -> (s1 == null ? s2 : (s2 == null ? s1 : s1 + ", " + s2)))
                + (twynContext.isDebug() ? ", node=\"" + node + "\"" : "")
                + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!implementedType.isAssignableFrom(obj.getClass())) {
            return false;
        }
        return twynContext.getIdentityMethods().getIdentityMethods(implementedType)
                .allMatch((m) -> {
                    try {
                        return Objects.equals(getMethod(m).invoke(obj), this.invoke(null, getMethod(m), NO_ARGS));
                    } catch (Throwable e) {
                        throw new TwynProxyException("Could not call method " + m + " for equals comparison..", e);
                    }
                });
    }

    @Override
    public int hashCode() {
        return Objects.hash(twynContext.getIdentityMethods().getIdentityMethods(implementedType)
                .map((m) -> {
                    try {
                        return this.invoke(null, getMethod(m), NO_ARGS);
                    } catch (Throwable e) {
                        throw new TwynProxyException("Could not call method " + m + " for hashCode calculation.", e);
                    }
                })
                .toArray());
    }

    private Method getMethod(ImplementedMethod method) {
        return ((ImplementedMethodMethod) method).getMethod();
    }

}