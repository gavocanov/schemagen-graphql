package com.bretpatterson.schemagen.graphql.utils;

import com.bretpatterson.schemagen.graphql.datafetchers.DefaultMethodDataFetcher;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Common Annotation related utility methods.
 */
public class AnnotationUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationUtils.class);
	public static final String DEFAULT_NULL = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";
	private static ClassLoader classLoader;
	private static ClassPath classPath;

	public final static class DEFAULT_NULL_CLASS extends DefaultMethodDataFetcher {}
	static {
		try {
			// Change classloader to work with spring boot executable jars
			// http://docs.spring.io/spring-boot/docs/current/reference/html/executable-jar.html#executable-jar-system-classloader
			classLoader = Thread.currentThread().getContextClassLoader();
			classPath = ClassPath.from(classLoader);
		} catch (IOException ex) {
			Throwables.propagate(ex);
		}
	}
	@SuppressWarnings("unchecked")
	public static <T> T findAnnotation(Annotation[] annotations, Class<T> type) {
		for (Annotation annotation : annotations) {
			if (annotation.annotationType() == type) {
				return (T) annotation;
			}
		}
		return null;
	}

	public static <T extends Annotation> Map<Class<?>, T> getClassesWithAnnotation(Class<T> annotation, String packageName) {
		ImmutableMap.Builder<Class<?>, T> results = ImmutableMap.builder();
		try {
			ImmutableSet<ClassPath.ClassInfo> classes = classPath.getTopLevelClassesRecursive(packageName);
			for (ClassPath.ClassInfo info : classes) {
				try {
					Class<?> type = info.load();
					T classAnnotation = type.getAnnotation(annotation);
					if (classAnnotation != null) {
						LOGGER.info("Found {} with annotation {}.", type.getCanonicalName(), annotation.getClass());
						results.put(type, classAnnotation);
					}
				} catch (NoClassDefFoundError ex) {
					LOGGER.warn("Failed to load {}.  This is probably because of an unsatisfied runtime dependency.", ex);
				}
			}
		} catch (Exception ex) {
			Throwables.propagate(ex);
		}

		return results.build();
	}

	public static <T extends Annotation> List<Method> getMethodsWithAnnotation(Class<?> targetClass, Class<T> annotationClass) {
		ImmutableList.Builder<Method> methods = ImmutableList.builder();

		for (Method method : targetClass.getDeclaredMethods()) {
			T annotation = method.getAnnotation(annotationClass);
			if (annotation != null) {
				methods.add(method);
			}
		}

		return methods.build();
	}

	public static <T extends Annotation> List<Field> getFieldsWithAnnotation(Class<?> targetClass, Class<T> annotationClass) {
		ImmutableList.Builder<Field> fields = ImmutableList.builder();

		for (Field field : targetClass.getDeclaredFields()) {
			T annotation = field.getAnnotation(annotationClass);
			if (annotation != null) {
				fields.add(field);
			}
		}

		return fields.build();
	}

	public static boolean isNullValue(String value) {
		return DEFAULT_NULL.equals(value);
	}

	public static boolean isNullValue(Class<?> value) {
		return DEFAULT_NULL_CLASS.class.equals(value);
	}
}
