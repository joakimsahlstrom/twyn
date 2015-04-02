package se.jsa.twyn.internal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import se.jsa.twyn.TwynId;
import se.jsa.twyn.internal.ProxiedInterface.ImplementedMethod;

class IdentityMethods {
	private final Map<ProxiedInterface, List<ImplementedMethod>> methods = new ConcurrentHashMap<ProxiedInterface, List<ImplementedMethod>>();

	public Stream<ImplementedMethod> getIdentityMethods(ProxiedInterface implementedType) {
		return methods.computeIfAbsent(implementedType, t -> IdentityMethods.get(t)).stream();
	}

	private static List<ImplementedMethod> get(ProxiedInterface implementedType) {
		List<ImplementedMethod> methods = implementedType.getMethods().stream().parallel()
				.filter(m -> !MethodType.DEFAULT.test(m) && m.getNumParameters() == 0)
				.collect(Collectors.toList());
		List<ImplementedMethod> idAnnotatedMethods = methods.stream().filter(m ->  m.hasAnnotation(TwynId.class)).collect(Collectors.toList());
		return (idAnnotatedMethods.size() > 0) ? idAnnotatedMethods : methods;
	}
}
