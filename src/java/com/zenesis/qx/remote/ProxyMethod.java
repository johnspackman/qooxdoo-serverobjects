/**
 * ************************************************************************
 * 
 *    server-objects - a contrib to the Qooxdoo project that makes server 
 *    and client objects operate seamlessly; like Qooxdoo, server objects 
 *    have properties, events, and methods all of which can be access from
 *    either server or client, regardless of where the original object was
 *    created.
 * 
 *    http://qooxdoo.org
 * 
 *    Copyright:
 *      2010 Zenesis Limited, http://www.zenesis.com
 * 
 *    License:
 *      LGPL: http://www.gnu.org/licenses/lgpl.html
 *      EPL: http://www.eclipse.org/org/documents/epl-v10.php
 *      
 *      This software is provided under the same licensing terms as Qooxdoo,
 *      please see the LICENSE file in the Qooxdoo project's top-level directory 
 *      for details.
 * 
 *    Authors:
 *      * John Spackman (john.spackman@zenesis.com)
 * 
 * ************************************************************************
 */
package com.zenesis.qx.remote;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.zenesis.qx.remote.annotations.Remote;
import com.zenesis.qx.remote.collections.ArrayList;
import com.zenesis.qx.remote.ClassWriter.Function;

/**
 * ProxyMethod is compiled by ProxyManager and attached to ProxyType to define
 * a method that is proxied.
 * 
 * @author John Spackman
 *
 */
public class ProxyMethod implements JsonSerializable {
	
	private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ProxyMethod.class); 
	
	public static final Comparator<ProxyMethod> ALPHA_COMPARATOR = new Comparator<ProxyMethod>() {

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(ProxyMethod o1, ProxyMethod o2) {
			return o1.getName().compareTo(o2.getName());
		}
	};

	private final Method method;
	private final Remote.Array array;
	private final boolean isMap;
	@SuppressWarnings("unused")
	private final Class keyType;
	private final Class arrayType;
	private final boolean prefetchResult;
	private final boolean cacheResult;
	private final boolean staticMethod;
	private String[] clientAnno;
	
	
	/**
	 * @param name
	 * @param returnType
	 * @param parameters
	 */
	public ProxyMethod(Method method) {
		super();
		this.method = method;
		
		Class returnType = method.getReturnType();
		Class keyType = String.class;
		boolean prefetchResult = false;
		boolean cacheResult = false;
		isMap = Map.class.isAssignableFrom(returnType);
		com.zenesis.qx.remote.annotations.Method anno = method.getAnnotation(com.zenesis.qx.remote.annotations.Method.class);
		
		if (returnType.isArray() || Iterable.class.isAssignableFrom(returnType) || isMap) {
			// How to present on the client - only ArrayList by default is wrapped on the client
			Remote.Array array;
			if (returnType.isArray()) {
				returnType = returnType.getComponentType();
				array = Remote.Array.NATIVE;
			} else {
				returnType = Object.class;
				array = Remote.Array.WRAP;
			}
			
			// Component type
			if (anno != null) {
				if (anno.array() != Remote.Array.DEFAULT)
					array = anno.array();
				if (anno.arrayType() != Object.class)
					returnType = anno.arrayType();
				if (anno.keyType() != Object.class)
					keyType = anno.keyType();
			}
			this.array = array;
			this.arrayType = returnType;
		} else {
			array = null;
			this.arrayType = null;
		}
		
		if (anno != null) {
			if (method.getParameterTypes().length == 0) {
				prefetchResult = anno.prefetchResult();
				cacheResult = anno.cacheResult()||prefetchResult;
			}
			if (anno.anno().length() > 0)
				clientAnno = new String[] { anno.anno() };
		}
		
		this.keyType = keyType;
		this.prefetchResult = prefetchResult;
		this.staticMethod = (method.getModifiers() & Modifier.STATIC) != 0;
		if (staticMethod && cacheResult) {
			log.warn("Cannot cacheResult on static method " + method);
			cacheResult = false;
		}
		this.cacheResult = cacheResult;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.jackson.map.JsonSerializable#serialize(org.codehaus.jackson.JsonGenerator, org.codehaus.jackson.map.SerializerProvider)
	 */
	@Override
	public void serialize(JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeStartObject();

		// Write the return type
		Class clazz = arrayType != null ? arrayType : method.getReturnType();
		if (Proxied.class.isAssignableFrom(clazz)) {
			ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(clazz);
			jgen.writeObjectField("returnType", type);
		} else if (isMap) {
			jgen.writeBooleanField("map", true);
		}
		if (cacheResult)
			jgen.writeBooleanField("cacheResult", true);
		if (staticMethod)
			jgen.writeBooleanField("staticMethod", true);
		if (clientAnno != null) {
			jgen.writeArrayFieldStart("anno");
			for (int i = 0; i < clientAnno.length; i++)
				jgen.writeString(clientAnno[i]);
			jgen.writeEndArray();
		}
			
		
		// Whether to wrap the return
		if (array != null)
			jgen.writeObjectField("returnArray", array.toString().toLowerCase());
		
		// The parameters - if any are Proxied objects, we need to write their class
		Class[] parameters = method.getParameterTypes();
		if (parameters.length > 0) {
			jgen.writeArrayFieldStart("parameters");
			for (int i = 0; i < parameters.length; i++) {
				if (Proxied.class.isAssignableFrom(parameters[i]))
					jgen.writeObject(ProxyTypeManager.INSTANCE.getProxyType(parameters[i]));
				else
					jgen.writeNull();
			}
			jgen.writeEndArray();
		}
		
		jgen.writeEndObject();
	}
	
	/**
	 * Called to write the property definition
	 * @param cw
	 * @param type
	 */
	public void write(ClassWriter cw) {
		Class<?> clazz = cw.getProxyType().getClazz();
		boolean isInterface = clazz != null && clazz.isInterface();

		if (isInterface) {
			cw.member(method.getName(), new Function(""));
	        if (clientAnno != null)
	        	cw.member("@" + method.getName(), clientAnno);
	        
		} else if (staticMethod) {
			cw.statics(method.getName(), new Function("return com.zenesis.qx.remote.ProxyManager._callServer(" + clazz.getName()
	                  + ", \"" + method.getName() + "\", qx.lang.Array.fromArguments(arguments));"));
	        if (clientAnno != null)
	        	cw.statics("@" + method.getName(), clientAnno);
	        
		} else {
			cw.member(method.getName(), new Function("return this._callServer(\"" + 
            		method.getName() + "\", qx.lang.Array.fromArguments(arguments));"));
            
			cw.member(method.getName() + "Async", 
            		new Function(
                    "var args = qx.lang.Array.fromArguments(arguments);\n" +
                    "return new qx.Promise(function(resolve, reject) {\n" +
                    "  args.push(function() {\n" +
                    "    resolve.apply(this, qx.lang.Array.fromArguments(arguments));\n" +
                    "  });\n" +
                    "  this._callServer(\"" + method.getName() + "\", args);\n" +
                    "}, this);"));
	        if (clientAnno != null)
	        	cw.member("@" + method.getName(), clientAnno);
		}
		if (!cw.isInterface()) {
    		HashMap<String, Object> meta = new HashMap<>();
    		meta.put("isServer", true);
    		Class tmp = arrayType != null ? arrayType : method.getReturnType();
    		if (Proxied.class.isAssignableFrom(tmp)) {
    			ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(tmp);
    			meta.put("returnType", type.getClassName());
    			cw.use(type);
    		} else if (isMap) {
    			meta.put("map", true);
    		}
    		if (cacheResult)
    			meta.put("cacheResult", true);
    		if (array != null)
    			meta.put("returnArray", array.toString().toLowerCase());
    		cw.method("defer").code += "clazz.$$methodMeta." + method.getName() + " = " + cw.objectToString(meta) + ";\n";
		}
	}
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonSerializable#serializeWithType(com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider, com.fasterxml.jackson.databind.jsontype.TypeSerializer)
	 */
	@Override
	public void serializeWithType(JsonGenerator gen, SerializerProvider sp, TypeSerializer ts) throws IOException, JsonProcessingException {
		serialize(gen, sp);
	}

	/**
	 * Gets the prefetch value
	 * @param self
	 * @return
	 */
	public Object getPrefetchValue(Object self) {
		try {
			return method.invoke(self);
		}catch(InvocationTargetException e) {
			throw new IllegalStateException("Error while invoking " + method + " on " + self + ": " + e.getCause().getMessage(), e.getCause());
		}catch(IllegalAccessException e) {
			throw new IllegalStateException("Error while invoking " + method + " on " + self + ": " + e.getMessage(), e);
		}
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return method.getName();
	}

	/**
	 * @return the method
	 */
	public Method getMethod() {
		return method;
	}

	/**
	 * @return the prefetchResult
	 */
	public boolean isPrefetchResult() {
		return prefetchResult;
	}

	/**
	 * @return the cacheResult
	 */
	public boolean isCacheResult() {
		return cacheResult;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return method.toString();
	}
	
}
