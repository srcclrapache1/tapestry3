package com.primix.foundation.prop;

import com.primix.foundation.*;
import com.primix.foundation.DynamicInvocationException;
import java.beans.*;
import java.lang.reflect.*;
import java.util.*;

/*
 * Tapestry Web Application Framework
 * Copyright (c) 2000 by Howard Ship and Primix Solutions
 *
 * Primix Solutions
 * One Arsenal Marketplace
 * Watertown, MA 02472
 * http://www.primix.com
 * mailto:hship@primix.com
 * 
 * This library is free software.
 * 
 * You may redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation.
 *
 * Version 2.1 of the license should be included with this distribution in
 * the file LICENSE, as well as License.html. If the license is not
 * included with this distribution, you may find a copy at the FSF web
 * site at 'www.gnu.org' or 'www.fsf.org', or you may write to the
 * Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139 USA.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 */
 
/**
 * Streamlines access to all the properties of a given
 * JavaBean.  Static methods acts as a factory for PropertyHelper instances, which
 * are specific to a particular Bean class.
 *
 * <p>A <code>PropertyHelper</code> for a bean class simplifies getting and setting properties
 * on the bean, handling (and caching) the lookup of methods as well as the dynamic
 * invocation of those methods.  It uses an instance of {@link IPropertyAccessor}
 * for each property.
 *
 * <p>PropertyHelper allows properties to be specified in terms of a path.  A path is a
 * series of property names seperate by periods.  So a property path of
 * 'application.user.address.street' is effectively the same as
 * the code <code>getApplication().getUser().getAddress().getStreet()</code>
 * (and just as likely to throw a <code>NullPointerException</code>).
 *
 * <p>Only single-valued properties (not indexed properties) are supported, and a minimum
 * of type checking is performed.
 *
 * <p>A mechanism exists to register custom <code>PropertyHelper</code> subclasses
 * for specific classes.  This would allow, for example, the contents of a <code>Map</code>
 * to be accessed in the same way as the properties of
 * a JavaBean.
 *
 * @version $Id$
 * @author Howard Ship
 */
 
public class PropertyHelper
{
	/**
	 *  Cache of helpers, keyed on the Class of the bean.
	 */

	private static Map helpers;

	/**
	 *  Registry of helper classes.  Key is the Class of the bean.  Value
	 *  is the Class of the Helper.
	 *
	 */

	private static Map registry;
	
	/**
	 *  Map of PropertyAccessors for the helper's
	 *  bean class. The keys are the names of the properties.
	 */

	protected Map accessors;

	/**
	 *  The Java Beans class for which this helper is configured.
	 */

	protected Class beanClass;

	/**
	 *  The separator character used to divide up different
	 *  properties in a nested property name.
	 */

	public final static char PATH_SEPERATOR = '.';

	private static final int MAP_SIZE = 7;

	protected PropertyHelper(Class beanClass)
	{
		this.beanClass = beanClass;
	}

	/**
	*  Uses JavaBeans introspection to find all the properties of the
	*  bean class.
	*/

	protected void buildPropertyAccessors()
	{
		BeanInfo info;
		int i;
		int count;
		PropertyDescriptor[] props;

		try
		{
			info = Introspector.getBeanInfo(beanClass);
		}
		catch (Exception e)
		{
			throw new DynamicInvocationException(e);
		}

		props = info.getPropertyDescriptors();
		count = props.length;

		accessors = new HashMap(MAP_SIZE);

		for (i = 0; i < count; i++)
			accessors.put(props[i].getName(),
				new PropertyAccessor(props[i]));

	}

	/**
	*  Factory method which returns a <code>PropertyHelper</code> for the given
	*  JavaBean class.
	*
	*  <p>Finding the right helper class involves a sequential lookup, first for an
	*  exact match, then for an exact match on the superclass, etc.  If not specific
	*  match is found, then <code>PropertyHelper</code> itself is used, which is
	*  the most typical case.
	*
	*  @see #register(Class, Class)
	*/

	public synchronized static PropertyHelper forClass(Class beanClass)
	{
		PropertyHelper helper;
		Class helperClass = null;
		Constructor constructor;
		int i;
		Class[] inheritance;
		Class candidate;

		if (helpers == null)
			helpers = new HashMap(MAP_SIZE);

		helper = (PropertyHelper)helpers.get(beanClass);
		if (helper != null)
			return helper;
		
		if (registry != null)
		{
		
			// Do a quick search for an exact match.
			
			helperClass = (Class)registry.get(beanClass);
			
			if (helperClass == null)
			{
				// Do a more exhaustive search based
				// on the inheritance (classes and interfaces)
				// of the bean class.
				
				inheritance = getInheritance(beanClass);
				for (i = 0; i < inheritance.length; i++)
				{
					candidate = inheritance[i];
					helperClass = (Class)registry.get(candidate);
					
					if (helperClass != null)
						break;
				}
			}	

		}
						
		// If no specific class registered, then use the standard implementation.

		if (helperClass == null)
		{
			helper = new PropertyHelper(beanClass);
			helpers.put(beanClass, helper);

			return helper;
		}

		// Here it gets tricky, we need to invoke a constructor.  We want the constructor
		// which takes a Class as a parameter.

		try
		{
			constructor = helperClass.getConstructor(new Class[]
				{ Class.class });
		}
		catch (NoSuchMethodException e)
		{
			throw new DynamicInvocationException(helperClass.getName() + 
				" does not implement the required contructor for use as a PropertyHelper.",
				e);
		}

		// This is equivalent to invoking the constructor FooClass.FooClass(beanClass).

		try
		{
			helper = (PropertyHelper)constructor.newInstance(new Object[]
				{ beanClass });
		}
		catch (Exception e)
		{
			throw new DynamicInvocationException("Could not invoke PropertyHelper constructor.", e);
		}

		// We don't want to go through this again, so record permanently the correct
		// helper for this class.

		helpers.put(beanClass, helper);

		return helper;
	}
	
	// These are only accessed from getInheritance().
	// getInheritance() is only invoked from forClass(), and
	// forClass() is synchronized.
	
	private static List inheritance = null;
	private static LinkedList queue = null;
	private static Set addedInterfaces = null;

	/**
	 *  Builds a List of all the classes and interfaces the beanClass
	 *  inherits from.  The first elements in the list is the super-class of
	 *  the beanClass,
	 *  followed by its super-classes (not including java.lang.Object).
	 *  Following this are the interfaces implemented by the beanClass
	 *  and each of its super-classes, following by interfaces further
	 *  up the interface inheritance chain.
	 *
	 */
		
	private static Class[] getInheritance(Class beanClass)
	{
		Class[] result;
		Class[] interfaces;
		Class candidate;
		int i;
		boolean first = true;
		
		if (inheritance == null)
			inheritance = new ArrayList();
	
		while (beanClass != null)
		{
			// Don't include java.lang.Object
			
			if (beanClass.equals(Object.class))
				break;
			
			// Add any interfaces (possibly zero) implemented by
			// the class to the interface queue.
				
			interfaces = beanClass.getInterfaces();
			
			for (i = 0; i < interfaces.length; i++)
			{
				if (queue == null)
					queue = new LinkedList();

				queue.add(interfaces[i]);
			}
			
			// Don't write the bean class itself.  Add any superclasses
			
			if (first)
				first = false;
			else
				inheritance.add(beanClass);
			
			beanClass = beanClass.getSuperclass();
			
		}
		
		// Add all the interfaces (and super-interfaces) to the list.
		// This is kind of breadth-first searching.  We need to do some
		// filtering because multiple super-classes may implement the same
		// methods, or multiple interfaces may extend the same interfaces.
		
		while (queue != null && !queue.isEmpty())
		{
			candidate = (Class)queue.removeFirst();
			
			if (addedInterfaces == null)
				addedInterfaces = new HashSet();
			else
				if (addedInterfaces.contains(candidate))
					continue;
			
			inheritance.add(candidate);
			addedInterfaces.add(candidate);
						
			interfaces = candidate.getInterfaces();
			
			for (i = 0; i < interfaces.length; i++)
				queue.add(interfaces[i]);
		}	
		
		// Convert the result to an array, so that we
		// can clear out our three collections (inheritance, addedInterfaces
		// and queue).
		
		result = new Class[inheritance.size()];
		
		result = (Class[])inheritance.toArray(result);
		
		inheritance.clear();
		if (addedInterfaces != null)
			addedInterfaces.clear();
		
		if (queue != null)
			queue.clear();
				
		// Return the final result as an array.
		
		return result;
	}

	/**
	*  Gets the value of a property from the given object.
	*  The property name may be a nested property, or
	*  a simple one.
	*
	*  @param object The object to retrieve a property from.
	*  @param propertyPath the name of the property to get,
	*  or a list of properties, seperated by periods.
	*
	*/

	public Object get(Object object, String propertyPath)
	{
		Object current;
		PropertyHelper helper;
		IPropertyAccessor accessor;
		String propertyName;
		StringSplitter splitter;
		String[] properties;
		int i;

		splitter = new StringSplitter(PATH_SEPERATOR);

		properties = splitter.splitToArray(propertyPath);

		current = object;
		helper = this;

		for (i = 0; i < properties.length; )
		{

			propertyName = properties[i];

			// Get the helper for the current object.
			// Get the accessor for the property to access
			// within the current object.  Get the new
			// current object from it.

			accessor = helper.getAccessor(current, propertyName);

			if (accessor == null)
				throw new MissingPropertyException(current, propertyName);

			try
			{
				current = accessor.get(current);
			}
			catch (MissingAccessorException e)
			{
				throw new MissingAccessorException(object, propertyPath, current, propertyName);
			}

			if (++i < properties.length)
				helper = forClass(current.getClass());
		}

		return current;
	}

	/**
	*  Finds an accessor for the given property name.  Returns the
	*  accessor if the class has the named property, or null
	*  otherwise.
	*
	*  @param propertyName the <em>simple</em> property name of the property to
	*  get.
	*
	*/

	public IPropertyAccessor getAccessor(Object instance, String propertyName)
	{
		if (accessors == null)
			buildPropertyAccessors();

		return (PropertyAccessor)accessors.get(propertyName);
	}

	public boolean isReadable(Object instance, String propertyName)
	{
		return getAccessor(instance, propertyName).isReadable();
	}

	public boolean isWritable(Object instance, String propertyName)
	{
		return getAccessor(instance, propertyName).isWritable();
	}

	/**
	*  Registers a particular <code>PropertyHelper</code> subclass as the correct
	*  class to use with a specific class of bean (or any class derived from
	*  the bean class).  An interface may be specified as well, in which case
	*  beans that match the interface (i.e., implement the interface directly
	*  or indirectly) will use the registerd helper class.
	*
	*/

	public static synchronized void register(Class beanClass, Class helperClass)
	{
		if (registry == null)
			registry = new HashMap(MAP_SIZE);

		registry.put(beanClass, helperClass);
	}
	/**
	*  Changes the value of one of a bean's properties.
	*  In the simple case, nestedName is simply the name of a
	*  property of the target object.
	*
	*  <p>Where nestedName really is a nested name, it is more
	*  complicated.  This method navigates through the
	*  properties (much like get), the actual property change
	*  only affects the final property in the nested name.
	*
	*/

	public void set(Object object, String propertyPath, Object value)
	{
		Object current;
		PropertyHelper helper;
		IPropertyAccessor accessor;
		String propertyName;
		StringSplitter splitter;
		String[] properties;
		int i;

		splitter = new StringSplitter(PATH_SEPERATOR);

		properties = splitter.splitToArray(propertyPath);

		current = object;
		helper = this;

		for (i = 0; i < properties.length - 1; i++)
		{	
			propertyName = properties[i];

			accessor = helper.getAccessor(current, propertyName);
			if (accessor == null)
				throw new MissingAccessorException(object, propertyPath, current, propertyName);

			// This property is somewhere in the middle
			// of the nested property name.  Work through
			// it to get to the last property.

			current = accessor.get(current);
			helper = forClass(current.getClass());

		}

		// Get the right accessor for the last property named, which is the
		// property to set.

		propertyName = properties[properties.length - 1];

		accessor = helper.getAccessor(current, propertyName);

		// Set the value.

		try
		{
			accessor.set(current, value);
		}
		catch (MissingAccessorException e)
		{
			throw new MissingAccessorException(object, propertyPath, current, propertyName);
		}


	}

	public String toString()
	{
		StringBuffer buffer;

		buffer = new StringBuffer("PropertyHelper[");
		buffer.append(beanClass.getName());
		buffer.append(']');

		return buffer.toString();
	}
}


