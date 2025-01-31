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
import org.junit.jupiter.api.extension.TestInstances;
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
	public void beforeTestExecution(ExtensionContext context) throws Exception {
		final Object testInstance = context.getTestInstance().get();
		final Class<?> actualTestClazz = testInstance.getClass();
		System.out.println("actualTestClass: " + actualTestClazz.getName());
		Class<?> topLevelClass = actualTestClazz;
		while (topLevelClass.getEnclosingClass() != null) {
			topLevelClass = topLevelClass.getEnclosingClass();
		}
		final Class<?> parentTestClass = topLevelClass != actualTestClazz ? topLevelClass : null;

		final Object parentClassInstance = context
				.getTestInstances()
				.get()
				.getEnclosingInstances()
				.stream()
				.filter(i -> i.getClass() == parentTestClass)
				.findFirst()
				.orElse(testInstance);
		System.out.println("enclosing instance class name: " + parentClassInstance.getClass().getName());
		final Map<String, Field> injectMap = new HashMap<>();
		for (Field testClassField : parentClassInstance.getClass().getDeclaredFields()) {
			if (testClassField.getAnnotation(InjectMocks.class) != null) {
				testClassField.setAccessible(true);
				final Object injectionTarget = testClassField.get(parentClassInstance);
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
				final MethodHandler handler = createMethodHandler(injectMap, injectionTarget, fieldMap, parentClassInstance, postConstructMethod);
				((Proxy) proxy).setHandler(handler);
				testClassField.set(parentClassInstance, proxy);
			} else if (testClassField.getAnnotation(InjectionSource.class) != null) {
				injectMap.put(testClassField.getName(), testClassField);
			}
		}
	}

	private Method findPostConstructMethod(Object injectionTarget) {
		for (Method method : injectionTarget.getClass().getDeclaredMethods()) {
			if (method.isAnnotationPresent(PostConstruct.class)) {
				return method;
			}
		}
		throw new RuntimeException(
				"@InvokePostConstruct is declared on:" + injectionTarget + " however no method annotated with @PostConstruct found");
	}

	private Map<String, List<Field>> createFieldMap(Class<?> targetClass) {
		if (targetClass == Object.class) {
			return new HashMap<>();
		} else {
			Map<String, List<Field>> fieldMap = createFieldMap(targetClass.getSuperclass());
			for (Field field : targetClass.getDeclaredFields()) {
				fieldMap.computeIfAbsent(field.getName(), k -> new LinkedList<>()).add(field);
			}
			return fieldMap;
		}
	}

	private MethodHandler createMethodHandler(final Map<String, Field> injectMap, final Object injectionTarget,
											  final Map<String, List<Field>> fieldMap, final Object testInstance, final Method postConstructMethod) {
		return (proxy, invokedMethod, proceedMethod, args) -> {
			invokedMethod.setAccessible(true);
			if (InjectExtension.isEnabled) {
				for (String fieldName : injectMap.keySet()) {
					for (Field targetField : fieldMap.get(fieldName)) {
						Field sourceField = injectMap.get(fieldName);
						sourceField.setAccessible(true);
						targetField.setAccessible(true);
						targetField.set(injectionTarget, sourceField.get(testInstance));
					}
				}
				if (postConstructMethod != null) {
					postConstructMethod.setAccessible(true);
					postConstructMethod.invoke(injectionTarget);
				}
			}
			try {
				return invokedMethod.invoke(injectionTarget, args);
			} catch (InvocationTargetException itEx) {
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
