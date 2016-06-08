/*
 * Copyright (C) 2013 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linuxtek.kona.util.KFileUtil;
import com.linuxtek.kona.util.KResourceUtil;

public class KClassLoader extends ClassLoader {
	private static Logger logger = LoggerFactory.getLogger(KClassLoader.class);

    private static Map<String, Class<?>> classes = 
    		new HashMap<String, Class<?>>();

    public KClassLoader() {
        super(KClassLoader.class.getClassLoader());
    }

    @Override
    public String toString() {
        return KClassLoader.class.getName();
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        logger.debug("findClass called for name: " + name);

        if (classes.containsKey(name)) {
            return classes.get(name);
        }

        String path = name.replace('.', File.separatorChar) + ".class";
        byte[] b = null;
        
        try {
            b = loadClassData(path);
        } catch (IOException e) {
            throw new ClassNotFoundException(
            		"Class not found at path: " 
            		+ new File(name).getAbsolutePath(), e);
        }

        Class<?> c = defineClass(name, b, 0, b.length);
        resolveClass(c);
        classes.put(name, c);

        return c;
    }

    private byte[] loadClassData(String name) throws IOException {
        logger.debug("loadClassData called for name: " + name);
        InputStream in = KResourceUtil.getResourceAsStream(name);
        return KFileUtil.toByteArray(in);
    }
    
    @SuppressWarnings("unused")
	protected void finalize() {
        for (Class<?> c : classes.values()) {
        	c = null;
        }
        logger.debug("KClassLoader finalize called ...");
    }
    
    /*
    public static Class<?> loadClass(String className) {
        return loadClass(null, className);
        
	}
    */
    
	public static Class<?> loadClass(ClassLoader cl, String className) {
		logger.debug("Attempting to load class: " + className);

        boolean useSystemClassLoader = false;
        
		Class<?> c = (Class<?>) classes.get(className);

		if (c == null) {
			Class<KClassLoader> clazz = KClassLoader.class;
            if (cl == null) {
            	cl = clazz.getClassLoader();
                useSystemClassLoader = true;
            }

			try {
				c = cl.loadClass(className);
				classes.put(className, c);
			} catch (ClassNotFoundException e) {
                if (!useSystemClassLoader) return null;
				cl = ClassLoader.getSystemClassLoader();
				try {
					c = cl.loadClass(className);
					classes.put(className, c);
				} catch (ClassNotFoundException e1) {
					StringWriter s = new StringWriter();
					e1.printStackTrace(new PrintWriter(s));
					String err = "Could not load class: " + className + "\n"
							+ e.getMessage() + "\n" + s;
					logger.error(err);
					throw new IllegalArgumentException(err);
				}
			}
		}

		logger.debug("Class loaded: " + c);
		return (c);
	}

	public static Object loadObject(String className, Object[] args) {
        return loadObject(null, className, args);
	}
    
	@SuppressWarnings("rawtypes")
	public static Object loadObject(ClassLoader cl, String className, Object[] args) {
		Object o = null;

		Class<?> c = loadClass(cl, className);

		if (c == null)
			return (null);

		// Create a class array
		Class[] params = new Class[args.length];
		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			if (arg != null)
				params[i] = arg.getClass();
			else
				params[i] = null;
		}

		// Constructor[] cons = c.getConstructors();
		boolean foundConstructor = false;
		Constructor[] cons = c.getDeclaredConstructors();
		for (int i = 0; i < cons.length; i++) {
			if (cons[i].getParameterTypes().length == args.length) {
				foundConstructor = true;
				logger.debug("found declared constructor");
				// try to instantiate using this constructor
				try {
					// allow private and protected access
					cons[i].setAccessible(true);
					o = cons[i].newInstance(args);
					break; // if successful, we're done!
				} catch (Exception e) {
					StringWriter s = new StringWriter();
					e.printStackTrace(new PrintWriter(s));
					String err = "Could not instantiate object: " + className
							+ "\n" + e.getMessage() + "\n" + s;
					logger.error(err);
					throw new IllegalArgumentException(err);
				}
			}
		}

		if (!foundConstructor)
			logger.debug("no matching constructors found");

		logger.debug("Object loaded: " + o);
		return (o);
	}
}