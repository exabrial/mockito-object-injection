package com.github.exabrial.junit5.injectmap;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.InjectMocks;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;

public class InjectExtension implements BeforeTestExecutionCallback {
	private static boolean isEnabled = true;

	public static void enable() {
		isEnabled = true;
	}

	public static void bypass() {
		isEnabled = false;
	}

	public static boolean status() {
		return isEnabled;
	}

	@Override
	public void beforeTestExecution(final ExtensionContext context) throws Exception {
		final List<Object> allInstances = context.getTestInstances().get().getAllInstances();
		final Map<String, Field> injectMap = new HashMap<>();
		final Map<String, Object> injectSourceOwners = new HashMap<>();

		for (final Object instance : allInstances) {
			for (final Field field : instance.getClass().getDeclaredFields()) {
				if (field.getAnnotation(InjectionSource.class) != null) {
					injectMap.put(field.getName(), field);
					injectSourceOwners.put(field.getName(), instance);
				}
			}
		}

		for (final Object instance : allInstances) {
			for (final Field testClassField : instance.getClass().getDeclaredFields()) {
				if (testClassField.getAnnotation(InjectMocks.class) != null) {
					testClassField.setAccessible(true);
					final Object injectionTarget = testClassField.get(instance);
					final ProxyFactory proxyFactory = new ProxyFactory();
					proxyFactory.setSuperclass(injectionTarget.getClass());
					proxyFactory.setFilter(createMethodFilter());
					final Class<?> proxyClass = proxyFactory.createClass();
					final Object proxy = proxyClass.newInstance();
					final Map<String, List<Field>> fieldMap = createFieldMap(injectionTarget.getClass());
					Method postConstructMethod;
					if (testClassField.getAnnotation(InvokePostConstruct.class) != null) {
						postConstructMethod = findPostConstructMethod(injectionTarget);
					} else {
						postConstructMethod = null;
					}
					final MethodHandler handler = createMethodHandler(injectMap, injectionTarget, fieldMap, injectSourceOwners,
							postConstructMethod);
					((Proxy) proxy).setHandler(handler);
					testClassField.set(instance, proxy);
				}
			}
		}
	}

	private Method findPostConstructMethod(final Object injectionTarget) {
		for (final Method method : injectionTarget.getClass().getDeclaredMethods()) {
			if (method.isAnnotationPresent(PostConstruct.class)) {
				return method;
			}
		}
		throw new RuntimeException(
				"@InvokePostConstruct is declared on:" + injectionTarget + " however no method annotated with @PostConstruct found");
	}

	private Map<String, List<Field>> createFieldMap(final Class<?> targetClass) {
		if (targetClass == Object.class) {
			return new HashMap<>();
		} else {
			final Map<String, List<Field>> fieldMap = createFieldMap(targetClass.getSuperclass());
			for (final Field field : targetClass.getDeclaredFields()) {
				fieldMap.computeIfAbsent(field.getName(), k -> new LinkedList<>()).add(field);
			}
			return fieldMap;
		}
	}

	private MethodHandler createMethodHandler(final Map<String, Field> injectMap, final Object injectionTarget,
			final Map<String, List<Field>> fieldMap, final Map<String, Object> injectSourceOwners, final Method postConstructMethod) {
		return (proxy, invokedMethod, proceedMethod, args) -> {
			invokedMethod.setAccessible(true);
			if (InjectExtension.isEnabled) {
				for (final String fieldName : injectMap.keySet()) {
					for (final Field targetField : fieldMap.get(fieldName)) {
						final Field sourceField = injectMap.get(fieldName);
						final Object sourceOwner = injectSourceOwners.get(fieldName);
						sourceField.setAccessible(true);
						targetField.setAccessible(true);
						targetField.set(injectionTarget, sourceField.get(sourceOwner));
					}
				}
				if (postConstructMethod != null) {
					postConstructMethod.setAccessible(true);
					postConstructMethod.invoke(injectionTarget);
				}
			}
			try {
				return invokedMethod.invoke(injectionTarget, args);
			} catch (final InvocationTargetException itEx) {
				if (null != itEx.getCause()) {
					throw itEx.getCause();
				} else {
					throw itEx;
				}
			}
		};
	}

	private MethodFilter createMethodFilter() {
		return method -> !Modifier.isPrivate(method.getModifiers());
	}
}
