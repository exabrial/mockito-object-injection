package com.emoneyusa.junit5.injectmap;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.InjectMocks;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;

public class InjectMapExtension implements BeforeTestExecutionCallback {
	@SuppressWarnings("unchecked")
	@Override
	public void beforeTestExecution(ExtensionContext context) throws Exception {
		Object testInstance = context.getTestInstance().get();
		if (testInstance != null) {
			Object injectionTarget = null;
			Field injectMocksField = null;
			Map<String, Object> injectMap = null;
			for (Field testClassField : testInstance.getClass().getDeclaredFields()) {
				if (testClassField.getAnnotation(InjectMocks.class) != null) {
					testClassField.setAccessible(true);
					injectionTarget = testClassField.get(testInstance);
					injectMocksField = testClassField;
				} else if (testClassField.getAnnotation(InjectionMap.class) != null) {
					testClassField.setAccessible(true);
					injectMap = (Map<String, Object>) testClassField.get(testInstance);
				}
			}
			if (injectionTarget == null || injectMocksField == null) {
				throw new RuntimeException("Couldn't find an instantiated field on your Test Case annotated with @InjectMocks");
			} else if (injectMap == null) {
				throw new RuntimeException("Couldn't find an instantiated field on your Test Case annotated with @InjectionMap");
			} else if (!(injectionTarget instanceof Proxy)) {
				ProxyFactory proxyFactory = new ProxyFactory();
				proxyFactory.setSuperclass(injectionTarget.getClass());
				proxyFactory.setFilter(createMethodFilter());
				Class<?> proxyClass = proxyFactory.createClass();
				Object proxy = proxyClass.newInstance();
				MethodHandler handler = createMethodHandler(injectMap, injectionTarget);
				((Proxy) proxy).setHandler(handler);
				injectMocksField.setAccessible(true);
				injectMocksField.set(testInstance, proxy);
			}
		}
	}

	private MethodHandler createMethodHandler(final Map<String, Object> handlerInjectMap, final Object handlerInjectionTarget) {
		return new MethodHandler() {
			@Override
			public Object invoke(Object proxy, Method invokedMethod, Method proceedMethod, Object[] args) throws Throwable {
				for (String fieldName : handlerInjectMap.keySet()) {
					Field field = handlerInjectionTarget.getClass().getDeclaredField(fieldName);
					field.setAccessible(true);
					field.set(handlerInjectionTarget, handlerInjectMap.get(fieldName));
				}
				try {
					return invokedMethod.invoke(handlerInjectionTarget, args);
				} catch (InvocationTargetException itEx) {
					if (null != itEx.getCause()) {
						throw itEx.getCause();
					} else {
						throw itEx;
					}
				}
			}
		};
	}

	private MethodFilter createMethodFilter() {
		return new MethodFilter() {
			@Override
			public boolean isHandled(Method method) {
				return !Modifier.isPrivate(method.getModifiers());
			}
		};
	}
}
