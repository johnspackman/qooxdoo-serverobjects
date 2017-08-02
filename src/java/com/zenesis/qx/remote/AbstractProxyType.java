package com.zenesis.qx.remote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.zenesis.qx.remote.annotations.Mixin;
import com.zenesis.qx.remote.annotations.Remote;
import com.zenesis.qx.remote.annotations.Remote.Array;
import com.zenesis.qx.remote.annotations.Use;
import com.zenesis.qx.remote.ClassWriter.Function;

public abstract class AbstractProxyType implements ProxyType {
	
	@Override
	public Class getClazz() {
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
			
			Class<?> clazz = getClazz();
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
			else if (getQooxdooExtend() != null)
				gen.writeObjectField("extend", getQooxdooExtend());
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
	
	public ClassWriter write() {
		ClassWriter cw = new ClassWriter(this);
		Class<?> clazz = getClazz();
		
		boolean isInterface = clazz != null && clazz.isInterface();
		if (!isInterface) {
	        cw.method("construct", new Function(
	                "var args = qx.lang.Array.fromArguments(arguments);\n" + 
	                "args.unshift(arguments);\n" + 
	                "this.base.apply(this, args);\n" + 
	                "this.initialiseProxy();\n"));
	        
			Function fn = cw.method("defer", true);
			String extend = null;
			if (getSuperType() != null)
				extend = getSuperType().getClassName();
			else if (getQooxdooExtend() != null)
				extend = getQooxdooExtend();
			cw.extend(extend == null ? "qx.core.Object" : extend);
			
			if (getMixins() != null) {
				for (Mixin mixin : getMixins())
					if (mixin.patch()) {
						fn.code += "qx.Class.patch(clazz, " + mixin.value() + ");\n";
					}
				ArrayList<String> arr = new ArrayList<>();
				for (Mixin mixin : getMixins())
					if (!mixin.patch())
						arr.add(mixin.value());
				if (!arr.isEmpty())
					cw.include(arr);
			}
			fn.code += "com.zenesis.qx.remote.MProxy.deferredClassInitialisation(clazz);\n";
		}
		
		if (getUses() != null) {
		    for (Use use : getUses()) {
		        ProxyType type = ProxyTypeManager.INSTANCE.getProxyType((Class<? extends Proxied>)use.value());
		        cw.use(type);
		    }
		}
		
		Set<ProxyType> interfaces = getInterfaces();
		if (!interfaces.isEmpty()) {
			ArrayList<String> arr = new ArrayList<>();
			for (ProxyType type : interfaces)
				arr.add(type.getClassName());
			if (isInterface)
				cw.extend(arr);
			else
				cw.implement(arr);
		}
		
		ProxyMethod[] methods = getMethods();
		for (ProxyMethod method : methods)
			method.write(cw);
		
		if (clazz == null || !isInterface) {
			Map<String, ProxyProperty> properties = getProperties();
			if (properties != null)
				for (ProxyProperty prop : properties.values())
					prop.write(cw);
			
			Map<String, ProxyEvent> events = getEvents();
			Set<String> propertyEventNames = createPropertyEventNames();
			if (events != null && !events.isEmpty()) {
				for (ProxyEvent event : events.values()) {
					HashMap<String, Object> meta = new HashMap<>();
					meta.put("isServer", true);
					if (!propertyEventNames.contains(event.getName())) {
						cw.event(event.getName(), "qx.event.type.Data");
						meta.put("isProperty", false);
					} else
						meta.put("isProperty", true);
					cw.method("defer").code += "clazz.$$eventMeta." + event.getName() + " = " + cw.objectToString(meta) + ";\n";
				}
			}
		}
		
		return cw;
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
