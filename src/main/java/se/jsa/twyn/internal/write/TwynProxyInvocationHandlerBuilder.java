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
package se.jsa.twyn.internal.write;

import java.lang.reflect.Proxy;

import com.fasterxml.jackson.databind.JsonNode;

import se.jsa.twyn.internal.TwynContext;
import se.jsa.twyn.internal.read.ProxiedInterface;

public class TwynProxyInvocationHandlerBuilder implements TwynProxyBuilder {

	@Override
	public <T> T buildProxy(Class<T> type, TwynContext twynContext, JsonNode jsonNode) {
		return type.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
				new Class<?>[] { type },
				new TwynProxyInvocationHandler(jsonNode, twynContext, ProxiedInterface.of(type))));
	}

	@Override
	public String toString() {
		return "TwynProxyInvocationHandlerBuilder []";
	}

}