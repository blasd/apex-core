/**
 * The MIT License
 * Copyright (c) 2014 Benoit Lacelle
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package blasd.apex.core.jmx;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.ReflectionUtils;

/**
 * This MBean enables the modification of primitive static variables, like DEBUG modes
 * 
 * @author Benoit Lacelle
 * 
 */
@ManagedResource
public class SetStaticMBean {
	protected static final Logger LOGGER = LoggerFactory.getLogger(SetStaticMBean.class);

	// THere might be a way to change private final fields... but it seems not
	// to work on a unit-test :|
	protected boolean forceForPrivateFinal = true;

	@ManagedOperation
	public void setStatic(String className, String fieldName, String newValueAsString)
			throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		Class<?> classToSet = Class.forName(className);

		Field field = getField(classToSet, fieldName);

		Class<?> fieldType = field.getType();
		if (fieldType == Boolean.class || fieldType == boolean.class) {
			field.set(null, Boolean.parseBoolean(newValueAsString));
		} else if (fieldType == Float.class || fieldType == float.class) {
			field.set(null, Float.parseFloat(newValueAsString));
		} else if (fieldType == Double.class || fieldType == double.class) {
			field.set(null, Double.parseDouble(newValueAsString));
		} else if (fieldType == Integer.class || fieldType == int.class) {
			field.set(null, Integer.parseInt(newValueAsString));
		} else if (fieldType == Long.class || fieldType == long.class) {
			field.set(null, Long.parseLong(newValueAsString));
		} else if (fieldType == String.class) {
			field.set(null, newValueAsString);
		} else {
			Object asObject = safeTrySingleArgConstructor(fieldType, newValueAsString);

			if (asObject != null) {
				// Instantiation succeeded
				field.set(null, asObject);
				return;
			}

			throw new RuntimeException("The field " + fieldType + " is not managed");
		}
	}

	@ManagedOperation
	public String getStaticAsString(String className, String fieldName)
			throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		return String.valueOf(getStatic(className, fieldName));
	}

	public Object getStatic(String className, String fieldName)
			throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		Class<?> classToSet = Class.forName(className);

		Field field = getField(classToSet, fieldName);

		// Instantiation succeeded
		return field.get(null);
	}

	private Field getField(Class<?> classToSet, String fieldName)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field field = ReflectionUtils.findField(classToSet, fieldName);

		if (forceForPrivateFinal) {
			// http://stackoverflow.com/questions/3301635/change-private-static-final-field-using-java-reflection
			ReflectionUtils.makeAccessible(field);

			// It may not work for primitive fields
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			ReflectionUtils.makeAccessible(modifiersField);
			modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
		}

		return field;
	}

	public static Object safeTrySingleArgConstructor(Class<?> fieldType, Object argument) {
		if (argument == null) {
			// TODO: try to find any Constructor accepting any Object
			return null;
		} else {
			// iterate through classes and interfaces
			{
				Class<?> classToTry = argument.getClass();

				while (classToTry != null) {
					Object asObject = safeTrySingleArgConstructor(fieldType, classToTry, argument);

					if (asObject != null) {
						// Instantiation succeeded
						return asObject;
					} else {
						classToTry = classToTry.getSuperclass();
					}
				}
			}

			for (Class<?> classToTry : argument.getClass().getInterfaces()) {
				Object asObject = safeTrySingleArgConstructor(fieldType, classToTry, argument);

				if (asObject != null) {
					// Instantiation succeeded
					return asObject;
				} else {
					classToTry = classToTry.getSuperclass();
				}
			}

			// Found nothing
			return null;
		}
	}

	/**
	 * We expect this method not to throw because of an invalid class, invalid type, etc
	 * 
	 * @param fieldType
	 * @param constructorClass
	 * @param argument
	 * @return an instance of the default contructor
	 */
	public static Object safeTrySingleArgConstructor(Class<?> fieldType, Class<?> constructorClass, Object argument) {
		// Unknown field: we will try to call the constructor taking a single String
		// It will work for joda LocalDate for instance
		try {
			Constructor<?> stringConstructor = fieldType.getConstructor(constructorClass);

			return stringConstructor.newInstance(argument);
		} catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException
				| RuntimeException e) {
			LOGGER.trace("No constructor for {} with {} argumennt", fieldType, constructorClass);
			return null;
		}
	}

	/**
	 * One could write "org.joda.time.LocalDate" One could write "org/joda/time/LocalDate.class"
	 */
	@ManagedOperation
	public List<String> getResourcesFor(String path) throws IOException {
		List<String> resources = new ArrayList<>();

		Enumeration<URL> urlEnum = this.getClass().getClassLoader().getResources(path);
		if (!urlEnum.hasMoreElements()) {
			// Transform "org.joda.time.LocalDate" to
			// "org/joda/time/LocalDate.class"
			urlEnum = this.getClass().getClassLoader().getResources(path.replace('.', '/') + ".class");
		}
		while (urlEnum.hasMoreElements()) {
			resources.add(urlEnum.nextElement().toString());
		}

		return resources;
	}
}
