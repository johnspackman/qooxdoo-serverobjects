package com.zenesis.qx.remote;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;

import org.apache.log4j.Logger;

import com.zenesis.qx.event.EventManager;
import com.zenesis.qx.remote.annotations.Properties;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.annotations.Remote;
import com.zenesis.qx.remote.annotations.Remote.Toggle;

public class ProxyPropertyImpl extends AbstractProxyProperty {
	
	private static final Logger log = Logger.getLogger(ProxyPropertyImpl.class); 

	public static final Comparator<ProxyProperty> ALPHA_COMPARATOR = new Comparator<ProxyProperty>() {

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(ProxyProperty o1, ProxyProperty o2) {
			return o1.getName().compareTo(o2.getName());
		}
	};
	
	private static final Class[] NO_CLASSES = {};

	// The class the property belongs to
	private final Class clazz;
	
	// Accessors or field
	private Method getMethod;
	private Method setMethod;
	private Field field;
	private String changeEventName;
	
	// Translators
	private Method serializeMethod;
	private Method deserializeMethod;
	private Method expireMethod;
	
	// Annotation
	private final Property anno;
	
	/**
	 * Creates a ProxyProperty from a Property annotation
	 * @param anno
	 */
	public ProxyPropertyImpl(Class clazz, String name, Property anno, Properties annoProperties) {
		super(name);
		changeEventName = "change" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
		this.anno = anno;
		this.clazz = clazz;
		sync = anno.sync();
		if (anno.event().length() == 0) {
			if (annoProperties == null || annoProperties.autoEvents())
				event = new ProxyEvent("change" + upname(name));
			else
				event = null;
		} else 
			event = new ProxyEvent(anno.event());
		if (anno.nullable() != Toggle.DEFAULT)
			nullable = anno.nullable() == Toggle.TRUE;
		if (anno.group().length() != 0) {
			onDemand = true;
			group = anno.group();
		} else {
			onDemand = anno.onDemand();
			group = null;
		}
		sendExceptions = anno.exceptions().booleanValue;
		if (anno.readOnly() != Remote.Toggle.DEFAULT)
			readOnly = anno.readOnly().booleanValue;
		if (anno.serialize().length() > 0)
			serializeMethod = findMethod(anno.serialize(), new Class[] { ProxyProperty.class, Object.class });
		if (anno.deserialize().length() > 0)
			deserializeMethod = findMethod(anno.deserialize(), new Class[] { ProxyProperty.class, Object.class });
		if (anno.expire().length() > 0)
			expireMethod = findMethod(anno.expire(), new Class[] { ProxyProperty.class });
	}

	/**
	 * Helper method to get a (de-)serializer method
	 * @param name
	 * @return
	 */
	private Method findMethod(String name, Class[] paramTypes) {
		try {
			Method method = clazz.getMethod(name, paramTypes);
			method.setAccessible(true);
			return method;
		}catch(NoSuchMethodException e) {
			log.fatal("Cannot find a method called " + name + ": " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Gets the accessor(s) for the property, caching the result
	 */
	private void getAccessors() {
		if (field != null || getMethod != null)
			return;
		
		if (anno.get().length() > 0)
			getMethod = findMethod(anno.get(), new Class[0]);
		
		// Look for a public field first
		if (getMethod == null)
			try {
				field = clazz.getField(name);
				field.setAccessible(true); // Disable access tests, reputed performance improvement
				if (sendExceptions == null)
					sendExceptions = false;
				propertyClass = new MetaClass(field.getType(), anno);
			}catch(NoSuchFieldException e) {
				field = null;
			}

		// Try for a getXxxx() method
		String upname = upname(name);
		
		// Find the get accessor
		if (getMethod == null && field == null) {
			try {
				getMethod = clazz.getMethod("get" + upname, NO_CLASSES);
				getMethod.setAccessible(true); // Disable access tests, reputed performance improvement
			} catch(NoSuchMethodException e) {
			}
			
			// Fallback to a isXxxx() method
			if (getMethod == null)
				try {
					if (upname.startsWith("Is"))
						getMethod = clazz.getMethod(name, NO_CLASSES);
					else
						getMethod = clazz.getMethod("is" + upname, NO_CLASSES);
					getMethod.setAccessible(true); // Disable access tests, reputed performance improvement
				} catch(NoSuchMethodException e) {
				}
		}
		if (getMethod == null && field == null) {
			log.fatal("Cannot find any accessor for property " + name + " in class " + clazz);
			return;
		}
		if (field != null)
			propertyClass = new MetaClass(field.getType(), anno);
		else
			propertyClass = new MetaClass(getMethod.getReturnType(), anno);
			
		// Check exception handling
		if (sendExceptions == null && getMethod.getExceptionTypes().length > 0)
			sendExceptions = true;
		
		if (anno.set().length() > 0)
			setMethod = findMethod(anno.set(), new Class[] { clazz });
		
		// Try for a setXxxx() method
		if (setMethod == null && (readOnly == null || !readOnly))
			try {
				Class actualJavaType;
				if (propertyClass.isArray())
					actualJavaType = Array.newInstance(propertyClass.getJavaType(), 0).getClass();
				else if (propertyClass.isCollection() || propertyClass.isMap())
					actualJavaType = propertyClass.getCollectionClass();
				else
					actualJavaType = propertyClass.getJavaType();
				setMethod = clazz.getMethod("set" + upname, new Class[] { actualJavaType });
				setMethod.setAccessible(true); // Disable access tests, reputed performance improvement
				if (sendExceptions == null && setMethod.getExceptionTypes().length > 0)
					sendExceptions = true;
			} catch(NoSuchMethodException e) {
				setMethod = null;
			}
		
		// If there is a custom serialiser, then use it's return type as the definitive Java class 
		if (serializeMethod != null && serializeMethod.getReturnType() != Object.class)
			propertyClass = new MetaClass(serializeMethod.getReturnType(), anno);
		
		// ArrayList
		if (propertyClass.isCollection()) {
			if (anno.arrayType() != Object.class)
				propertyClass.setJavaType(anno.arrayType());
			else if (readOnly != null && !readOnly)
				log.fatal("Missing @Property.arrayType for property " + this);
			else {
				log.warn("Missing @Property.arrayType for property " + this);
				readOnly = true;
			}
			propertyClass.setWrapArray(anno.array() != Remote.Array.NATIVE);
			
		// Array
		} else if (propertyClass.isArray()) {
			if (anno.arrayType() != Object.class) {
				if (anno.arrayType() != propertyClass.getJavaType())
					throw new IllegalStateException("Conflicting array types between annotation and declaration in " + this);
				propertyClass.setJavaType(anno.arrayType());
			}
			propertyClass.setWrapArray(anno.array() == Remote.Array.WRAP);

		// Maps
		} else if (propertyClass.isMap()) {
			if (anno.arrayType() != Object.class)
				propertyClass.setJavaType(anno.arrayType());
			else if (readOnly == null || !readOnly)
				log.fatal("Missing @Property.arrayType for property " + this);
			else
				log.warn("Missing @Property.arrayType for property " + this);
			propertyClass.setWrapArray(anno.array() != Remote.Array.NATIVE);
		}
		
		// Finish up
		if (sendExceptions == null)
			sendExceptions = false;
		if (readOnly == null) {
			if (field != null || setMethod != null)
				readOnly = false;
			else if (setMethod == null && (propertyClass.isCollection() || propertyClass.isMap()))
				readOnly = false;
			else
				readOnly = true;
		}
	}
	
	/**
	 * Returns the value currently in the property of an object 
	 * @param proxied
	 * @return
	 */
	@Override
	public Object getValue(Proxied proxied) throws ProxyException {
		getAccessors();
		Object result = null;
		try {
			if (field != null)
				result = field.get(proxied);
			else if (getMethod != null)
				result = getMethod.invoke(proxied);
			else
				log.error("Cannot get value for " + this + " because there is no accessor");
			
			result = serialize(proxied, result);
		} catch(InvocationTargetException e) {
			Throwable t = e.getTargetException();
			log.error("Exception while getting value for " + this + " on " + proxied + ": " + t.getMessage(), t);
			throw new ProxyException(proxied, "Cannot read property " + name + " in class " + clazz + " in object " + proxied + ": " + t.getMessage(), t);
		} catch(IllegalAccessException e) {
			log.error("Exception while getting value for " + this + " on " + proxied + ": " + e.getMessage(), e);
			throw new ProxyException(proxied, "Cannot read property " + name + " in class " + clazz + " in object " + proxied + ": " + e.getMessage(), e);
		}
		return result;
	}
	
	/**
	 * Sets the value of a property in an object
	 * @param proxied
	 * @param value
	 */
	@Override
	public void setValue(Proxied proxied, Object value) throws ProxyException {
		getAccessors();
		value = deserialize(proxied, value);
		Object oldValue = getValue(proxied);
		if (value == oldValue)
			return;
		try {
			if (field != null) {
				if (!readOnly) {
					field.set(proxied, value);
					changedValue(proxied, value, oldValue);
					return;
				}
			}
			
			if (setMethod != null) {
				// If it's not a collection then we don't try to handle it because we want Java
				//	autoboxing to handle conversions between primitive types
				if (value == null || !propertyClass.isCollection()) {
					setMethod.invoke(proxied, value);
					changedValue(proxied, value, oldValue);
					return;
				}
				Class type = setMethod.getParameterTypes()[0];
				if (type.isAssignableFrom(value.getClass())) {
					setMethod.invoke(proxied, value);
					changedValue(proxied, value, oldValue);
					return;
				}
			}
			
			// If we're setting a collection or array, then we can choose to replace the contents
			//	 of the current property value if we don't have a setXxx method 
			if (value != null && propertyClass.isCollection()) {
				Collection coll = (Collection)getValue(proxied);
				boolean setValue = false;
				
				// If the setMethod takes a Collection then we can create one
				if (coll == null && setMethod != null && Collection.class.isAssignableFrom(setMethod.getParameterTypes()[0])) {
					try {
						coll = (Collection)getPropertyClass().getCollectionClass().newInstance();
					}catch(Exception e) {
						throw new IllegalArgumentException(e.getMessage(), e);
					}
					setValue = true;
				}

				if (coll != null) {
					if (value.getClass().isArray()) {
						coll.clear();
						Object[] src = (Object[]) value;
						for (Object obj : src)
							coll.add(obj);
					} else if (value instanceof Collection) {
						Collection src = (Collection) value;
						for (Object obj : src)
							coll.add(obj);
					}
					if (setValue)
						setMethod.invoke(proxied, coll);
					changedValue(proxied, value, oldValue);
					return;
				}
			}
			
			// If a field is a collection then there doesn't have to be a setXxx method - but if the
			//	current value is null (eg client constructed object) setValue() will be called.  For
			//	client constructed objects, the best action is usually to reset the client's property 
			//	value to match the server
			log.warn("Cannot set property " + this + " because there is no set method and field is not accessible");
			Object current = getValue(proxied);
			ProxyManager.propertyChanged(proxied, getName(), current, null);
			
		} catch(InvocationTargetException e) {
			Throwable t = e.getTargetException();
			log.error("Exception while getting value for " + this + " on " + proxied + ": " + t.getMessage(), t);
			throw new ProxyException(proxied, "Cannot write property " + name + " in class " + clazz + " in object " + proxied + ": " + t.getMessage(), t);
		} catch(IllegalAccessException e) {
			log.error("Exception while getting value for " + this + " on " + proxied + ": " + e.getMessage(), e);
			throw new ProxyException(proxied, "Cannot write property " + name + " in class " + clazz + " in object " + proxied + ": " + e.getMessage(), e);
		} catch(IllegalArgumentException e) {
			log.error("Exception while getting value for " + this + " on " + proxied + ": " + e.getMessage(), e);
			throw new ProxyException(proxied, "Failed to set value for property " + name + " in class " + clazz + " to value " + value + ", method=" + setMethod + ", field=" + field, e);
		}
	}
	
	/**
	 * Called when setValue has just changed the value of a property
	 * @param proxied
	 * @param value
	 * @param oldValue
	 */
	protected void changedValue(Proxied proxied, Object value, Object oldValue) {
		if (proxied instanceof PropertyChangeListener)
			((PropertyChangeListener)proxied).propertyChanged(this, value, oldValue);
		if (EventManager.hasListener(proxied, changeEventName, null))
			EventManager.fireDataEvent(proxied, changeEventName, new EventManager.ChangeValue(value, oldValue));
	}

	/**
	 * Expires the cached value, in response to the same event on the client
	 * @param proxied
	 */
	@Override
	public void expire(Proxied proxied) {
		getAccessors();
		if (expireMethod != null)
			try {
				expireMethod.invoke(proxied, this);
			} catch(InvocationTargetException e) {
				Throwable t = e.getTargetException();
				throw new IllegalArgumentException("Cannot write property " + name + " in class " + clazz + " in object " + proxied + ": " + t.getMessage(), t);
			} catch(IllegalAccessException e) {
				throw new IllegalArgumentException("Cannot write property " + name + " in class " + clazz + " in object " + proxied + ": " + e.getMessage(), e);
			} catch(IllegalArgumentException e) {
				throw new IllegalArgumentException("Failed to expire value for property " + name + " in class " + clazz, e);
			}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.zenesis.qx.remote.ProxyProperty#serialize(com.zenesis.qx.remote.Proxied, java.lang.Object)
	 */
	@Override
	public Object serialize(Proxied proxied, Object value) {
		try {
			if (serializeMethod != null)
				value = serializeMethod.invoke(proxied, this, value);
		} catch(InvocationTargetException e) {
			Throwable t = e.getTargetException();
			throw new IllegalArgumentException("Cannot write property " + name + " in class " + clazz + " in object " + proxied + ": " + t.getMessage(), t);
		} catch(IllegalAccessException e) {
			throw new IllegalArgumentException("Cannot write property " + name + " in class " + clazz + " in object " + proxied + ": " + e.getMessage(), e);
		} catch(IllegalArgumentException e) {
			throw new IllegalArgumentException("Failed to set value for property " + name + " in class " + clazz + " to value " + value, e);
		}
		return value;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.zenesis.qx.remote.ProxyProperty#deserialize(com.zenesis.qx.remote.Proxied, java.lang.Object)
	 */
	@Override
	public Object deserialize(Proxied proxied, Object value) {
		try {
			if (value != null && propertyClass.isSubclassOf(Date.class)) {
				long millis = ((Number)value).longValue();
				value = new Date(millis);
			}
			if (deserializeMethod != null)
				value = deserializeMethod.invoke(proxied, this, value);
		} catch(InvocationTargetException e) {
			Throwable t = e.getTargetException();
			throw new IllegalArgumentException("Cannot write property " + name + " in class " + clazz + " in object " + proxied + ": " + t.getMessage(), t);
		} catch(IllegalAccessException e) {
			throw new IllegalArgumentException("Cannot write property " + name + " in class " + clazz + " in object " + proxied + ": " + e.getMessage(), e);
		} catch(IllegalArgumentException e) {
			throw new IllegalArgumentException("Failed to set value for property " + name + " in class " + clazz + " to value " + value, e);
		}
		return value;
	}
	
	/**
	 * Returns true if this property is readonly
	 * @return
	 */
	@Override
	public boolean isReadOnly() {
		getAccessors();
		return super.isReadOnly();
	}

	/**
	 * @return the serializeMethod
	 */
	public Method getSerializeMethod() {
		return serializeMethod;
	}

	/**
	 * @return the deserializeMethod
	 */
	public Method getDeserializeMethod() {
		return deserializeMethod;
	}

	/**
	 * @return the getMethod
	 */
	public Method getGetMethod() {
		return getMethod;
	}

	/**
	 * @return the setMethod
	 */
	public Method getSetMethod() {
		return setMethod;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return clazz.toString() + "." + name;
	}
	
	/**
	 * Converts the first character of name to uppercase
	 * @param name
	 * @return
	 */
	private String upname(String name) {
		String upname = Character.toUpperCase(name.charAt(0)) + name.substring(1);
		return upname;
	}
}
