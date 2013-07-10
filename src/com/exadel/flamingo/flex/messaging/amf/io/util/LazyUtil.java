/*
 * Exadel Flamingo
 * Copyright (C) 2008 Exadel, Inc.
 * 
 * This file is part of Exadel Flamingo.
 * 
 * Exadel Flamingo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation
 * 
 * LazyUtil.java
 * 
 * Last modified by: $Author: klebed $
 * $Revision: 1603 $   $Date: 2008-06-09 00:40:18 -0700 (Mon, 09 Jun 2008) $
 */
package com.exadel.flamingo.flex.messaging.amf.io.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for prevent 
 * lazy initialization exception 
 * 
 * @author klebed
 */
public class LazyUtil {
    
	/*
	 * class reference
	 */
    private static final String HIBERNATE_CLASS = "org.hibernate.Hibernate";
	
	/*
	 * method reference 
	 */
    private static final String IS_INITIALIZED = "isInitialized";
	
    /**
     * Return Hibernate class instance
	 * @return Hibernate class instance
	 * if it exist 
	 */
    private static Class < ? > getHibernateClass() {
    	Class < ? > cl = null;
    	
    	try {
            cl = Class.forName(HIBERNATE_CLASS);
    	} catch (ClassNotFoundException e) {
            // in this case jar which contain 
    		// Hibernate class not found
        }
    	return cl;
    }
	
	/**
	 * Return "isInitialized" Hibernate static method
	 * @param cl - "Hibernate" class instance
	 * @return "isInitialized" Hibernate static method
	 */
    private static Method getInitializeMethod(Class< ? > cl) {
        Method method = null;
        
        try {
            method = cl.getDeclaredMethod(IS_INITIALIZED, new Class[]{Object.class});
        } catch (NoSuchMethodException e) {
			//in this case 'isInitialized' method can't be null
        } 
        return method;		
    }
	
	/**
	 * Check is current property was initialized
	 * @param method - hibernate static method, which check
	 * is initilized property
	 * @param obj - object which need for lazy check
	 * @return boolean value
	 */
    private static boolean isPropertyInitialized(Method method, Object obj) {
        boolean isInitialized = true;
        
        try {
            isInitialized = (Boolean) method.invoke(null, new Object[] {obj});
        } catch (IllegalArgumentException e) {
		   // do nothing
        } catch (IllegalAccessException e) {
		   // do nothing
        } catch (InvocationTargetException e) {
		   // do nothing
        }
        return isInitialized;
    }
	
	/**
	 * This method remove from JavaClassDescriptor class 
	 * lazy properties
	 * @param desc - current JavaClassDescriptor class
	 * @param o - target class reference, which contain lazy
	 *            properties
	 */
    public static void removeLazyFields(JavaClassDescriptor desc, Object o) {
    	
        Class < ? > cl =  getHibernateClass();
    	
        if (cl == null) {
            return;
        }
        
        Method method = getInitializeMethod(cl);
        
        List<Integer> deleteIndexes = new ArrayList<Integer>(); 
        
        for (int i = 0; i < desc.getPropertiesCount(); i++) {
            Object obj = desc.getPropertyValue(i, o);
                          
            if (!isPropertyInitialized(method, obj)) {
            	deleteIndexes.add(i);
            }
    	}
        
        deleteProperties(deleteIndexes, desc);
    }
    
    private static void deleteProperties(List<Integer> deleteIndexes, JavaClassDescriptor desc) {
    	if (deleteIndexes != null && !deleteIndexes.isEmpty()) {
            for (int i = deleteIndexes.size() - 1; i >= 0; i--) {
                desc.removeProperty(deleteIndexes.get(i));	
            }
    	}
    }
    
    public static boolean isProxy(Object object) {
    	boolean isProxy = false;
    	//Class clazz = object.getClass();
        // check if it's a cglib proxy
        /*if (net.sf.cglib.proxy.Proxy.isProxyClass(clazz)) {
        	isProxy = true;
        } else if (net.sf.cglib.proxy.Enhancer.isEnhanced(clazz)) {
        	isProxy = true;
        } else if (org.hibernate.proxy.HibernateProxy.class.isInstance(object)) {
        	isProxy = true;
        } else if (object instanceof org.hibernate.proxy.HibernateProxy) {
        	isProxy = true;
        }*/
        try {
			if (Class.forName("org.hibernate.proxy.HibernateProxy").isInstance(object)) {
				isProxy = true;
			}
		} catch (ClassNotFoundException e) {
			//Do nothing
		}
    	
        return isProxy;
    }
    
}
