package com.zenesis.qx.remote;

import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.zenesis.qx.remote.ClassWriter.Function;
import com.zenesis.qx.remote.ClassWriter.RawValue;
import com.zenesis.qx.remote.annotations.Annotation;
import com.zenesis.qx.remote.annotations.Annotations;
import com.zenesis.qx.remote.annotations.PropertyDate;
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
	
	protected PropertyDate.DateValues dateValues;
	protected boolean zeroTime;
	
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
		ProxyType keyType;
		String keyTypeName = null;
		Remote.Array array;
		ProxyType arrayClass;
		String componentTypeName;
		String check;
		ProxyType clazz;
	}
	
	protected Spec analyse() {
		Spec spec = new Spec();
		
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
				if (propertyClass.getKeyClass() != null) {
				    if (propertyClass.getKeyClass() != null && Proxied.class.isAssignableFrom(propertyClass.getKeyClass()))
				        spec.keyType = ProxyTypeManager.INSTANCE.getProxyType((Class<? extends Proxied>)propertyClass.getKeyClass());
					spec.keyTypeName = translateTypeName(propertyClass.getKeyClass());
				}
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
                spec.clazz = propertyClass.getProxyType();
				
			} else if (Proxied.class.isAssignableFrom(clazz)) {
                ProxyType type = propertyClass.getProxyType();
                spec.clazz = type;
                
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
		
		if (cw.getProxyType().getClassName().endsWith("TestQsoMap"))
		    System.out.println(this);
		
		Spec spec = analyse();
		boolean needsTransform = false;
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
			cw.use(spec.arrayClass);
		}

        if (spec.clazz != null)
            cw.use(spec.clazz);
        
		if (spec.map) {
			if (spec.array == Remote.Array.WRAP)
				pdef.put("check", arrayClassName != null ? arrayClassName :  "com.zenesis.qx.remote.Map");
			if (spec.keyTypeName != null)
				annoSets.put("keyTypeName", spec.keyTypeName);
            if (spec.keyType != null)
				cw.use(spec.keyType);
			if (spec.componentTypeName != null)
				annoSets.put("componentTypeName", spec.componentTypeName);
			

		} else if (spec.check != null) {
			pdef.put("check", spec.check);

		} else if (spec.array != null) {
			if (spec.array == Remote.Array.WRAP) {
			    String tmp = arrayClassName == null ? "qx.data.Array" : arrayClassName;
                pdef.put("transform", "__transform" + upname);
                pdef.put("check", tmp);
                cw.member("__transform" + upname, new Function("value", "return com.zenesis.qx.remote.MProxy.transformToDataArray(value, " + tmp + ");"));
			} else
				pdef.put("check", "Array");
			if (spec.componentTypeName != null)
				annoSets.put("componentTypeName", spec.componentTypeName);
		} else if (spec.clazz != null) {
		    pdef.put("check", spec.clazz.getClassName());
		}
		if ((spec.map || spec.array != null) && create) {
			Function fn = cw.method("construct");
			fn.code += "this.set" + upname + "(new " + pdef.get("check") + "());\n";
			fn = cw.method("destruct", true);
			fn.code += "this.set" + upname + "(null);\n";
		}

		pdef.put("apply", "_apply" + upname);
		cw.member("_apply" + upname, new Function("value", "oldValue", "name", 
				"this._applyProperty(\"" + name + "\", value, oldValue, name);"));
		
		if (onDemand) {
			cw.member("get" + upname, new Function("async", "return this._getPropertyOnDemand('" + name + "', async);"));
			cw.member("expire" + upname, new Function("sendToServer", "return this._expirePropertyOnDemand('" + name + "', sendToServer);"));
			cw.member("set" + upname, new Function("value", "async", "return this._setPropertyOnDemand('" + name + "', value, async);"));
			cw.member("get" + upname + "Async", new Function("return this._getPropertyOnDemandAsync('" + name + "');"));
		} else
		    cw.member("get" + upname + "Async", new Function("return qx.Promise.resolve(this.get" + upname + "()).bind(this);"));
		
        ArrayList<RawValue> arr = new ArrayList<>();
		if (!annoSets.isEmpty()) {
			if (!annoSets.isEmpty())
				arr.add(new RawValue("new com.zenesis.qx.remote.annotations.Property().set(" + cw.objectToString(annoSets) + ")"));
		}
        if (clientAnno != null) {
            for (int i = 0; i < clientAnno.size(); i++) {
                arr.add(new RawValue(clientAnno.get(i)));
            }
        }
        if (spec.check != null && spec.check.equals("Date")) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("value", dateValues);
            map.put("zeroTime", zeroTime);
            arr.add(new RawValue("new com.zenesis.qx.remote.annotations.PropertyDate().set(" + cw.objectToString(map) + ")"));
            needsTransform = true;
        }
		if (!arr.isEmpty())
		    pdef.put("@", arr);
		
		if (!cw.isInterface()) {
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
            if (spec.keyTypeName == null || NATIVE_KEY_TYPES.contains(spec.keyTypeName))
                meta.put("nativeKeyType", true);
    
    		cw.method("defer").code += "qx.lang.Object.mergeWith(clazz.$$properties." + name + ", " + cw.objectToString(meta) + ");\n";
		}
		
		if (needsTransform) {
            pdef.put("transform", "_transform" + upname);
            cw.member("_transform" + upname, new Function("value", "oldValue", 
                    "this._transformProperty(\"" + name + "\", value, oldValue);"));
		}
	}
	private static final HashSet<String> NATIVE_KEY_TYPES;
	static {
	    NATIVE_KEY_TYPES = new HashSet<>();
	    NATIVE_KEY_TYPES.add("String");
	    NATIVE_KEY_TYPES.add("Integer");
        NATIVE_KEY_TYPES.add("Double");
        NATIVE_KEY_TYPES.add("Float");
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

    public PropertyDate.DateValues getDateValues() {
        return dateValues;
    }

    public boolean isZeroTime() {
        return zeroTime;
    }

}
