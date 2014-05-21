package com.zenesis.qx.remote;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.zenesis.qx.remote.annotations.Remote;
import com.zenesis.qx.remote.annotations.Remote.Array;

public abstract class AbstractProxyType implements ProxyType {
	
	/**
	 * Returns the class
	 * @return
	 */
	protected Class getClazz() {
		String className = getClassName();
		if (className == null)
			return null;
		try {
			return Class.forName(className);
		}catch(ClassNotFoundException e) {
			throw new IllegalStateException("Cannot find class called " + getClassName());
		}
	}

	/*package*/ void serialize(JsonGenerator gen, SerializerProvider sp, Remote.Array array) throws IOException {
		try {
			ProxySessionTracker tracker = ((ProxyObjectMapper)gen.getCodec()).getTracker();
			
			Class clazz = getClazz();
			Set<ProxyType> interfaces = getInterfaces();
			ProxyMethod[] methods = getMethods();
			Map<String, ProxyProperty> properties = getProperties();
			Map<String, ProxyEvent> events = getEvents();
			Set<String> propertyEventNames = createPropertyEventNames();
	
			String className = getClassName();
			if (array == Array.NATIVE)
				className += "[]";
			else if (array == Array.WRAP)
				className += "[wrap]";
			
			if (tracker.isTypeDelivered(this)) {
				gen.writeString(className);
				return;
			}
			
			tracker.setTypeDelivered(this);
			gen.writeStartObject();
			gen.writeStringField("className", className);
			if (clazz != null && clazz.isInterface())
				gen.writeBooleanField("isInterface", true);
			if (getSuperType() != null)
				gen.writeObjectField("extend", getSuperType());
			if (!interfaces.isEmpty()) {
				gen.writeArrayFieldStart("interfaces");
				for (ProxyType type : interfaces)
					gen.writeObject(type);
				gen.writeEndArray();
			}
			if (methods.length > 0) {
				gen.writeObjectFieldStart("methods");
				for (ProxyMethod method : methods)
					gen.writeObjectField(method.getName(), method);
				gen.writeEndObject();
			}
			if (clazz == null || !clazz.isInterface()) {
				if (properties != null && !properties.isEmpty()) {
					gen.writeObjectFieldStart("properties");
					for (ProxyProperty property : properties.values())
						gen.writeObjectField(property.getName(), property);
					gen.writeEndObject();
				}
				if (events != null && !events.isEmpty()) {
					gen.writeObjectFieldStart("events");
					for (ProxyEvent event : events.values()) {
						gen.writeObjectFieldStart(event.getName());
						if (propertyEventNames.contains(event.getName()))
							gen.writeBooleanField("isProperty", true);
						gen.writeEndObject();
					}
					gen.writeEndObject();
				}
			}
			gen.writeEndObject();
		}catch(IOException e) {
			throw new ProxyTypeSerialisationException(e);
		}
	}

	/**
	 * Compiles a list of
	 * @return
	 */
	public Set<String> createPropertyEventNames() {
		Map<String, ProxyProperty> properties = getProperties();
		
		if (properties.isEmpty())
			return Collections.EMPTY_SET;
		HashSet<String> propertyEventNames = new HashSet<String>();
		for (ProxyProperty property : properties.values()) {
			ProxyEvent event = property.getEvent();
			if (event != null)
				propertyEventNames.add(event.getName());
		}
		if (propertyEventNames.isEmpty())
			return Collections.EMPTY_SET;
		return propertyEventNames;
	}
	

}
