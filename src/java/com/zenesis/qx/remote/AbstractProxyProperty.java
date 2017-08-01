package com.zenesis.qx.remote;

import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;

import org.hamcrest.core.IsAnything;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.zenesis.qx.remote.ClassWriter.Function;
import com.zenesis.qx.remote.annotations.Annotation;
import com.zenesis.qx.remote.annotations.Annotations;
import com.zenesis.qx.remote.annotations.Remote;
import com.zenesis.qx.remote.collections.HashMap;

public abstract class AbstractProxyProperty implements ProxyProperty {

	// Name of the property
	protected final String name;
	
	// Whether to sync
	protected Remote.Sync sync;
	
	// Whether on demand
	protected boolean onDemand;
	protected String group;
	
	// Any event that gets fired
	protected ProxyEvent event;
	
	// Whether null is a valid value
	protected Boolean nullable;
	
	// Client annotations
	protected ArrayList<String> clientAnno;
	
	// Whether to send exceptions which occur while setting a value received from teh client
	protected Boolean sendExceptions;
	
	// Whether readonly
	protected Boolean readOnly;
	
	// The data type of the property and  (if an array) the component type of the array
	protected MetaClass propertyClass;
	
	// Whether the client should automatically initialise the property (arrays and maps only)
	protected boolean create;

	// ProxyType this is attached to
	private ProxyType proxyType;
	
	public AbstractProxyProperty(String name) {
		super();
		if (name.equals("property"))
			throw new IllegalArgumentException("Cannot have a property called 'property'");
		this.name = name;
	}
	
	public void addClientAnno(Annotation anno) {
		if (anno != null) {
			if (clientAnno == null)
				clientAnno = new ArrayList();
			clientAnno.add(anno.value());
		}
	}
	
	public void addClientAnno(Annotations annos) {
		if (annos != null) {
			if (clientAnno == null)
				clientAnno = new ArrayList();
			for (int i = 0; i < annos.value().length; i++)
				clientAnno.add(annos.value()[i].value());
		}
	}
	
	public void addAnnotations(AccessibleObject obj) {
		this.addClientAnno(obj.getAnnotation(Annotation.class));
		this.addClientAnno(obj.getAnnotation(Annotations.class));
	}
	
	protected static final class Spec {
		boolean nullable;
		boolean map = false;
		String keyTypeName = null;
		Remote.Array array;
		ProxyType arrayClass;
		String componentTypeName;
		String check;
		ProxyType clazz;
	}
	
	protected Spec analyse() {
		Spec spec = new Spec();
		
		if (name.equals("objectKeyMap"))
		    spec = spec;
		if (propertyClass != null) {
			Class clazz = propertyClass.getJavaType();
			//if (proxyType.getClazz().getName().equals("TestQsoMap"))
			//	clazz = clazz;
			if (this.nullable == null) {
				if (clazz == Boolean.TYPE || 
					clazz == Character.TYPE || 
					clazz == Byte.TYPE || 
					clazz == Short.TYPE || 
					clazz == Integer.TYPE || 
					clazz == Long.TYPE || 
					clazz == Float.TYPE || 
					clazz == Double.TYPE)
					spec.nullable = false;
				else
					spec.nullable = true;
			} else
				spec.nullable = this.nullable;
				
			if (propertyClass.isMap()) {
				spec.map = true;
				if (propertyClass.getKeyClass() != null)
					spec.keyTypeName = translateTypeName(propertyClass.getKeyClass());
			}
			
			if (propertyClass.isArray() || propertyClass.isCollection() || propertyClass.isMap()) {
				if (!propertyClass.isWrapArray())
					spec.array = Remote.Array.NATIVE;
				else
					spec.array = Remote.Array.WRAP;
				if (propertyClass.getCollectionClass() != null && Proxied.class.isAssignableFrom(propertyClass.getCollectionClass())) {
					ProxyType type = ProxyTypeManager.INSTANCE.getProxyType((Class<? extends Proxied>)propertyClass.getCollectionClass());
					spec.arrayClass = type;
				}
				spec.componentTypeName = translateTypeName(clazz);
				
			} else { 
				if (clazz == boolean.class || clazz == Boolean.class)
					spec.check = "Boolean";
				else if (clazz == int.class || clazz == Integer.class)
					spec.check = "Integer";
				else if (clazz == double.class || clazz == Double.class)
					spec.check = "Number";
				else if (clazz == float.class || clazz == Float.class)
					spec.check = "Number";
				else if (clazz == char.class || clazz == String.class)
					spec.check = "String";
				else if (Date.class.isAssignableFrom(clazz))
					spec.check = "Date";
			}
			if (Proxied.class.isAssignableFrom(clazz)) {
				ProxyType type = propertyClass.getProxyType();
				spec.clazz = type;
			}
		} else {
			spec.nullable = true;
		}
		
		return spec;
	}
	
	@Override
	public void serialize(JsonGenerator gen, SerializerProvider sp) throws IOException, JsonProcessingException {
		Spec spec = analyse();
		
		gen.writeStartObject();
		gen.writeStringField("sync", sync.remoteId);
		if (event != null)
			gen.writeStringField("event", event.getName());
		if (onDemand)
			gen.writeBooleanField("onDemand", true);
		if (isReadOnly())
			gen.writeBooleanField("readOnly", true);
		if (create)
			gen.writeBooleanField("create", true);
		if (clientAnno != null) {
			gen.writeArrayFieldStart("anno");
			for (int i = 0; i < clientAnno.size(); i++)
				gen.writeString(clientAnno.get(i));
			gen.writeEndArray();
		}
		
		gen.writeBooleanField("nullable", spec.nullable);
		if (spec.map) {
			gen.writeBooleanField("map", true);
			if (spec.keyTypeName != null)
				gen.writeStringField("keyTypeName", spec.keyTypeName);
		}
		if (spec.array != null) {
			if (spec.array == Remote.Array.NATIVE)
				gen.writeStringField("array", "native");
			else
				gen.writeStringField("array", "wrap");
			if (spec.arrayClass != null) {
				ProxyType type = ProxyTypeManager.INSTANCE.getProxyType((Class<? extends Proxied>)propertyClass.getCollectionClass());
				gen.writeObjectField("arrayClass", spec.arrayClass);
			}
			gen.writeStringField("componentTypeName", spec.componentTypeName);
		} else {
			if (spec.check != null)
				gen.writeStringField("check", spec.check);
		}
		if (spec.clazz != null)
			gen.writeObjectField("clazz", spec.clazz);
		
		gen.writeEndObject();
	}
	
	@Override
	public void write(ClassWriter cw) {
		String upname = Character.toUpperCase(name.charAt(0)) + name.substring(1);
		HashMap<String, Object> pdef = new HashMap<>();
		cw.property(name, pdef);
		
		Spec spec = analyse();
		pdef.put("nullable", spec.nullable);
		HashMap<String, Object> annoSets = new HashMap<>();
		if (isReadOnly())
			annoSets.put("readOnly", true);
		if (!spec.nullable && spec.check != null) {
			switch(spec.check) {
			case "Boolean":
				pdef.put("init", false);
				break;
				
			case "Number":
			case "Integer":
				pdef.put("init", 0);
				break;
				
			case "String":
				pdef.put("init", "");
				break;
				
			case "Date":
				pdef.put("init", new Function("new Date()"));
				break;
			}
		}
		if (event != null)
			pdef.put("event", event.getName());
		
		String arrayClassName = null;
		if ((spec.map || spec.array != null) && spec.arrayClass != null) {
			arrayClassName = spec.arrayClass.getClassName();
		}

		if (spec.map) {
			if (spec.array == Remote.Array.WRAP)
				pdef.put("check", arrayClassName != null ? arrayClassName :  "com.zenesis.qx.remote.Map");
			if (spec.keyTypeName != null)
				annoSets.put("keyTypeName", spec.keyTypeName);
			if (spec.componentTypeName != null)
				annoSets.put("componentTypeName", spec.componentTypeName);

		} else if (spec.check != null) {
			pdef.put("check", spec.check);

		} else if (spec.array != null) {
			if (spec.array == Remote.Array.WRAP) {
				pdef.put("transform", "_transformToDataArray");
				pdef.put("check", arrayClassName != null ? arrayClassName : "qx.data.Array");
			} else
				pdef.put("check", "Array");
			if (spec.componentTypeName != null)
				annoSets.put("componentTypeName", spec.componentTypeName);
		}
		if ((spec.map || spec.array != null) && create) {
			Function fn = cw.method("constructor");
			fn.code += "this.set" + upname + "(new " + pdef.get("check") + "());\n";
			fn = cw.method("destructor", true);
			fn.code += "this.set" + upname + "(null);\n";
		}

		cw.member("_apply" + upname, new Function("value", "oldValue", "name", 
				"this._applyProperty(\"" + name + "\", value, oldValue, name);"));
		
		if (onDemand) {
			cw.member("get" + upname, new Function("async", "return this._getPropertyOnDemand('" + name + "', async);"));
			cw.member("expire" + upname, new Function("sendToServer", "return this._expirePropertyOnDemand('" + name + "', sendToServer);"));
			cw.member("set" + upname, new Function("value", "async", "return this._setPropertyOnDemand('" + name + "', value, async);"));
			cw.member("get" + upname + "Async", new Function(
		          "return new qx.Promise(function(resolve) {\n" +
		          "  this._getPropertyOnDemand('" + name + "', function(result) {\n" +
		          "    resolve(result);\n" + 
		          "  });\n" +
		          "}, this);"));
		} else {
			Function fn = cw.method("defer", true);
			fn.code += "com.zenesis.qx.remote.ProxyManager.patchNormalProperty(this, \"" + name + "\");\n";
		}
		cw.member("get" + upname + "Async", new Function("return qx.Promise.resolve(this.get" + upname + "()).bind(this);"));
		
		if (!annoSets.isEmpty() || clientAnno != null) {
			ArrayList<String> arr = new ArrayList<>();
			if (!annoSets.isEmpty())
				arr.add("new com.zenesis.qx.remote.annotations.Property().set(" + cw.objectToString(annoSets) + ")");
			if (clientAnno != null) {
				for (int i = 0; i < clientAnno.size(); i++) {
					arr.add(clientAnno.get(i));
				}
			}
			cw.property("@" + name, arr);
		}
		
		HashMap<String, Object> meta = new HashMap<>();
		meta.put("isServer", true);
		if (sync != null)
			meta.put("sync", sync.toString().toLowerCase());
		meta.put("onDemand", onDemand);
		meta.put("readOnly", readOnly);
		if (spec.array != null)
			meta.put("array", spec.array == Remote.Array.WRAP ? "wrap" : "native");
		if (spec.arrayClass != null)
			meta.put("arrayClass", spec.arrayClass.getClassName());
		if (spec.map)
			meta.put("map", true);
		cw.method("defer").code += "qx.lang.Object.mergeWith(properties." + name + ", " + cw.objectToString(meta) + ");\n";
	}
	
	private String translateTypeName(Class clazz) {
		if (clazz == boolean.class || clazz == Boolean.class)
			return "Boolean";
		else if (clazz == int.class || clazz == Integer.class)
			return "Integer";
		else if (clazz == double.class || clazz == Double.class)
			return "Number";
		else if (clazz == float.class || clazz == Float.class)
			return "Number";
		else if (clazz == char.class || clazz == String.class)
			return "String";
		else if (Date.class.isAssignableFrom(clazz))
			return "Date";
		if (Proxied.class.isAssignableFrom(clazz))
			return clazz.getName();
		return null;
	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonSerializable#serializeWithType(com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider, com.fasterxml.jackson.databind.jsontype.TypeSerializer)
	 */
	@Override
	public void serializeWithType(JsonGenerator gen, SerializerProvider sp, TypeSerializer ts) throws IOException, JsonProcessingException {
		serialize(gen, sp);
	}

	/**
	 * Returns true if this property is readonly
	 * @return
	 */
	@Override
	public boolean isReadOnly() {
		return readOnly != null && readOnly;
	}


	/**
	 * @return the name
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * @return the sync
	 */
	@Override
	public Remote.Sync getSync() {
		return sync;
	}

	/**
	 * @return the event
	 */
	@Override
	public ProxyEvent getEvent() {
		return event;
	}

	/**
	 * @return the nullable
	 */
	@Override
	public boolean isNullable() {
		return nullable;
	}

	/**
	 * @return the onDemand
	 */
	@Override
	public boolean isOnDemand() {
		return onDemand;
	}

	/**
	 * @return the group
	 */
	@Override
	public String getGroup() {
		return group;
	}

	/**
	 * @return the sendExceptions
	 */
	@Override
	public boolean isSendExceptions() {
		return sendExceptions != null && sendExceptions;
	}

	/**
	 * @return the propertyClass
	 */
	@Override
	public MetaClass getPropertyClass() {
		return propertyClass;
	}

	/**
	 * @return the autoClientCreate
	 */
	public boolean isCreate() {
		return create;
	}

	/**
	 * @return the proxyType
	 */
	public ProxyType getProxyType() {
		return proxyType;
	}

	/**
	 * @param proxyType the proxyType to set
	 */
	public void setProxyType(ProxyType proxyType) {
		this.proxyType = proxyType;
	}

}
