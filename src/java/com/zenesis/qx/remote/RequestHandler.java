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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenesis.qx.event.EventManager;
import com.zenesis.qx.remote.CommandId.CommandType;
import com.zenesis.qx.utils.DiagUtils;
import com.zenesis.qx.utils.ArrayUtils;

/**
 * Handles the request and responses for a client.
 * 
 * This uses the Jackson JSON parser to pull data incrementally from the request; this makes the 
 * code harder to read/write and means that we expect the JSON data to occur in a particular 
 * order even though the JSON specification does not allow ordering to be enforced.  However,
 * by dealing with data incrementally in the way we are able to delay deciding what type of
 * data to instantiate until we have worked out where it is going - i.e. we look at the types
 * of a method's parameters and use that type information to change the way we parse.  In this
 * way, we can support any arbitrary mapping between JSON and Java thanks to Jackson.
 * 
 * @author "John Spackman <john.spackman@zenesis.com>"
 *
 */
public class RequestHandler {
	
	public static final Logger log = org.apache.logging.log4j.LogManager.getLogger(RequestHandler.class);
	
	// Command type strings received from the client
	private static final String CMD_BOOTSTRAP = "bootstrap";	// Reset application session and get bootstrap
	private static final String CMD_CALL = "call";				// Call server object method 
	private static final String CMD_DISPOSE = "dispose";		// The client has disposed of a Proxied object 
	private static final String CMD_EDIT_ARRAY = "edit-array";	// Changes to an array 
	private static final String CMD_EXPIRE = "expire";			// Expires a flushed property value 
	private static final String CMD_LISTEN = "listen";			// Add an event listener 
	private static final String CMD_NEW = "new";				// Create a new object 
	private static final String CMD_POLL = "poll";				// Poll for changes (ie do nothing) 
	private static final String CMD_SET = "set";				// Set a property value 
	private static final String CMD_UNLISTEN = "unlisten";		// Remove an event listener
	
	// The request header sent by the client to validate the session
	public static final String HEADER_SESSION_ID = "X-ProxyManager-SessionId";
	public static final String HEADER_SHA1 = "X-ProxyManager-SHA1";
	public static final String HEADER_INDEX = "X-ProxyManager-RequestIndex";
	public static final String HEADER_CLIENT_TIME = "X-ProxyManager-ClientTime";
	
	// Maximum time to wait for a lock on the response
	private static int s_requestLockTimeout = 2 * 60 * 1000;

	// This class is sent as data by cmdBootstrap
	public static final class Bootstrap {
		public final Proxied bootstrap;
		public final String sessionId;
		
		public Bootstrap(Proxied bootstrap, String sessionId) {
			super();
			this.bootstrap = bootstrap;
			this.sessionId = sessionId;
		}
	}
	
	// This class is sent as data by cmdNewObject to change a client ID into a server ID
	public static final class MapClientId {
		public final int serverId;
		public final int clientId;

		public MapClientId(int serverId, int clientId) {
			super();
			this.serverId = serverId;
			this.clientId = clientId;
		}
	}
	
	// This class is thrown to provide Exception information to the client
	public static class ExceptionDetails {
		public final String exceptionClass;
		public final String message;
		
		/**
		 * @param exceptionClass
		 * @param message
		 */
		public ExceptionDetails(String exceptionClass, String message) {
			super();
			this.exceptionClass = exceptionClass;
			this.message = message;
		}
		
	}
	
	// Sent when a function returns
	public static final class FunctionReturn {
		public final int asyncId;
		public final Object result;
		public FunctionReturn(int asyncId, Object result) {
			super();
			this.asyncId = asyncId;
			this.result = result;
		}
		
	}

	// This class is sent as data when an exception is thrown while setting a property value
	public static final class PropertyReset extends ExceptionDetails {
		public final Object oldValue;
		
		/**
		 * @param oldValue
		 * @param exceptionClass
		 * @param message
		 */
		public PropertyReset(Object oldValue, String exceptionClass, String message) {
			super(exceptionClass, message);
			this.oldValue = oldValue;
		}		
	}

	// RequestHandler for the current thread
	private static ThreadLocal<RequestHandler> s_currentHandler = new ThreadLocal<RequestHandler>();
	
	// Tracker for the session
	private final ProxySessionTracker tracker;
	
	// Unique request identifier (debug only)
	private String requestId = "(unnamed request)";

	// Where I/O log files go to, null means that they are disabled
	public static File s_traceLogDir = null;
	
	/**
	 * @param tracker
	 */
	public RequestHandler(ProxySessionTracker tracker) {
		super();
		this.tracker = tracker;
	}
	
	/**
	 * Returns the time to wait for an exclusive lock on the request, in milliseconds
	 * @return
	 */
	public static int getRequestLockTimeout() {
		return s_requestLockTimeout;
	}

	/**
	 * Sets the time to wait for an exclusive lock on the request, in milliseconds.  
	 * @param requestLockTimeout
	 */
	public static void setRequestLockTimeout(int requestLockTimeout) {
		RequestHandler.s_requestLockTimeout = requestLockTimeout;
	}

	/**
	 * Handles the callback from the client; expects either an object or an array of objects
	 * 
	 * This method needs to be synchronized because if there are multiple requests (where one or more are 
	 * probably asynchronous) then we could serialise serverObjects in a slow response and the the faster
	 * response only gets a server object ID ... except that the slow response has not completed yet and
	 * therefore the fast response has not told the client about the server object. 
	 * 
	 * The same is true for client IDs
	 * 
	 * @param request
	 * @param response
	 * @param sessionId session id passed from the client for validation, ignored if null
	 * @throws ServletException
	 * @throws IOException
	 */
	public void processRequestDebug(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (!log.isDebugEnabled()) {
			processRequest(request.getReader(), response.getWriter());
			return;
		}
		
		String sessionId = request.getHeader(RequestHandler.HEADER_SESSION_ID);
		String expectedSha = request.getHeader(RequestHandler.HEADER_SHA1);
		String strIndex = request.getHeader(RequestHandler.HEADER_INDEX);
		String strClientTime = request.getHeader(RequestHandler.HEADER_CLIENT_TIME);
		try {
			tracker.setLastClientTime(new Date(Long.parseLong(strClientTime)));
		} catch(NumberFormatException e) {
			log.error("Cannot parse client time " + strClientTime + " for " + requestId);
		}
		int index = Integer.parseInt(strIndex);
		int actualIndex = tracker.getNextRequestIndex();
		requestId = tracker.getSessionId().replace(':', '_') + "/" + new SimpleDateFormat("dd-HHmm.ss.SSS").format(new Date()) + "-" + 
				DiagUtils.zeroPad(index) + "-" + DiagUtils.zeroPad(actualIndex);
		
		StringWriter sw = null;
		sw = new StringWriter();
		Reader reader = request.getReader();
		char[] buffer = new char[32 * 1024];
		int length;
		while ((length = reader.read(buffer)) > 0) {
			sw.write(buffer, 0, length);
		}
		
		if (sessionId != null && !tracker.getSessionId().equals(sessionId)) {
			log.debug("Wrong session id sent from client, expected " + tracker.getSessionId() + " found " + sessionId + ", data=" + sw.toString());
			throw new IllegalArgumentException("Wrong session id sent from client, expected " + tracker.getSessionId() + " found " + sessionId);
		}
		
		Writer writer = new StringWriter();
		if (log.isTraceEnabled() && s_traceLogDir != null) {
			Object obj = tracker.getObjectMapper().readValue(sw.toString(), Object.class);
			String out = tracker.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
			DiagUtils.writeFile(new File(s_traceLogDir, requestId + "-in.txt"), out);
		}

		if (expectedSha != null) {
	        String hash = DiagUtils.getSha1(sw.toString());
	        if (!hash.equals(expectedSha))
	        	throw new IllegalArgumentException("SHA1 mismatch for " + requestId + ", found " + hash + " expected " + expectedSha);
		}

		processRequest(new StringReader(sw.toString()), writer);
		
		String out = writer.toString();
		if (log.isTraceEnabled() && s_traceLogDir != null) {
			DiagUtils.writeFile(new File(s_traceLogDir, requestId + "-out.txt"), out);
		}
		String hash = DiagUtils.getSha1(out);
		response.setHeader(HEADER_SHA1, hash);
		response.setHeader(HEADER_INDEX, Integer.toString(index));
		response.getWriter().write(out);
	}
	
	public void processRequest(Reader request, Writer response) throws ServletException, IOException {
		try {
			if (!tracker.getRequestLock().tryLock(s_requestLockTimeout, TimeUnit.MILLISECONDS))
				throw new ServletException("Timeout while waiting for request lock for " + requestId);
		}catch(InterruptedException e) {
			throw new ServletException("Exception while waiting for request lock for " + requestId + ": " + e.getMessage());
		}
		try {
			s_currentHandler.set(this);
			ObjectMapper objectMapper = tracker.getObjectMapper();
			try {
				JsonParser jp = objectMapper.getJsonFactory().createJsonParser(request);
				if (jp.nextToken() == JsonToken.START_ARRAY) {
					while(jp.nextToken() != JsonToken.END_ARRAY)
						processCommand(jp);
				} else if (jp.getCurrentToken() == JsonToken.START_OBJECT)
					processCommand(jp);
		
				CommandQueue queue = tracker.getQueue();
				JsonSerializable data = null;
				synchronized(queue) {
					data = queue.getDataToFlush();
				}
				if (data != null) {
					Writer actualResponse = response;
					objectMapper.writeValue(actualResponse, data);
				}
				
			} catch(ProxyTypeSerialisationException e) {
				log.fatal("Unable to serialise type information to client for " + requestId + ": " + e.getMessage(), e);
				
			} catch(ProxyException e) {
				handleException(response, objectMapper, e);
				
			} catch(Exception e) {
				log.error("Exception during callback for " + requestId + ": " + e.getMessage(), e);
				tracker.getQueue().queueCommand(CommandType.EXCEPTION, null, null, new ExceptionDetails(e.getClass().getName(), e.getMessage()));
				objectMapper.writeValue(response, tracker.getQueue());
				
			} finally {
				s_currentHandler.set(null);
			}
		} finally {
			tracker.getRequestLock().unlock();
		}
	}
	
	/**
	 * Handles the callback from the client; expects either an object or an array of objects
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	public void processRequest(Reader request, OutputStream response) throws ServletException, IOException {
		processRequest(request, new OutputStreamWriter(response));
	}

	/**
	 * Called to handle exceptions during processRequest
	 * @param response
	 * @param objectMapper
	 * @param e
	 * @throws IOException
	 */
	protected void handleException(Writer response, ObjectMapper objectMapper, ProxyException e) throws IOException {
		Throwable cause = e.getCause();
		CommandQueue queue = tracker.getQueue();
		queue.queueCommand(CommandType.EXCEPTION, e.getServerObject(), null, 
				new ExceptionDetails(cause.getClass().getName(), cause.getMessage()));
		
		JsonSerializable data = null;
		synchronized(queue) {
			data = queue.getDataToFlush();
		}
		objectMapper.writeValue(response, data);
	}
	
	/**
	 * Returns the request handler for the current thread
	 * @return
	 */
	public static RequestHandler getCurrentHandler() {
		return s_currentHandler.get();
	}
	
	/**
	 * Handles an object from the client; expects the object to have a property "cmd" which is
	 * the type of command
	 * @param jp
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void processCommand(JsonParser jp) throws ServletException, IOException {
		String cmd = getFieldValue(jp, "cmd", String.class);
		
		if (cmd.equals(CMD_BOOTSTRAP))
			cmdBootstrap(jp);
		
		else if (cmd.equals(CMD_CALL))
			cmdCallServerMethod(jp);
		
		else if (cmd.equals(CMD_DISPOSE))
			cmdDispose(jp);
		
		else if (cmd.equals(CMD_EDIT_ARRAY))
			cmdEditArray(jp);
		
		else if (cmd.equals(CMD_EXPIRE))
			cmdExpire(jp);
		
		else if (cmd.equals(CMD_LISTEN))
			cmdAddListener(jp);
		
		else if (cmd.equals(CMD_NEW))
			cmdNewObject(jp);
		
		else if (cmd.equals(CMD_POLL))
			cmdPoll(jp);
		
		else if (cmd.equals(CMD_SET))
			cmdSetProperty(jp);
		
		else if (cmd.equals(CMD_UNLISTEN))
			cmdRemoveListener(jp);
		
		else
			throw new ServletException("Unrecognised command from client: " + cmd);
	}
	
	/**
	 * Resets the application session and returns the bootstrap object to the client
	 * @param jp
	 */
	protected void cmdBootstrap(JsonParser jp) throws ServletException, IOException {
		tracker.resetSession();
		tracker.getQueue().queueCommand(CommandId.CommandType.BOOTSTRAP, null, null, new Bootstrap(tracker.getBootstrap(), tracker.getSessionId()));
		jp.nextToken();
	}
	
	/**
	 * Handles a server method call from the client; expects a serverId, methodName, and an optional
	 * array of parameters
	 * @param jp
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void cmdCallServerMethod(JsonParser jp) throws ServletException, IOException {
		// Get the basics
		Object obj = getFieldValue(jp, "serverId", Object.class);
		String methodName = getFieldValue(jp, "methodName", String.class);
		int asyncId = getFieldValue(jp, "asyncId", Integer.class);
		Class serverClass = null;
		Proxied serverObject = null;
		if (obj instanceof Integer) {
			int serverId = (Integer)obj;
			serverObject = getProxied(serverId);
			serverClass = serverObject.getClass();
		} else if (obj != null) {
			try {
				serverClass = Class.forName(obj.toString());
			} catch(ClassNotFoundException e) {
				log.error("Cannot find server class " + obj + ": " + e.getMessage());
			}
		}
		
		
		// Onto what should be parameters
		jp.nextToken();
		
		// Find the method by hand - we have already guaranteed that there will not be conflicting
		//	method names (ie no overridden methods) but Java needs a list of parameter types
		//	so we do it ourselves.
		boolean found = false;
		
		// Check for property accessors; if serverObject is null then it's static method call and
		//	properties are not supported
		if (serverObject != null && methodName.length() > 3 && (methodName.startsWith("get") || methodName.startsWith("set"))) {
			String name = methodName.substring(3, 4).toLowerCase();
			if (methodName.length() > 4)
				name += methodName.substring(4);
			ProxyProperty property = null;
			for (ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(serverClass); type != null; type = type.getSuperType()) {
				property = type.getProperties().get(name);
				if (property != null) {
					found = true;
					break;
				}
			}
			if (found) {
				Object result = null;
				if (methodName.startsWith("get")) {
					readParameters(jp, null);
					result = property.getValue(serverObject);
				} else {
					Object[] values = readParameters(jp, new Class[] { property.getPropertyClass().getJavaType() });
					property.setValue(serverObject, values[0]);
				}
				if (property.getGroup() != null) {
					for (ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(serverClass); type != null; type = type.getSuperType()) {
						for (ProxyProperty tmp : type.getProperties().values()) {
							if (tmp.getGroup() != null && tmp.getGroup().equals(property.getGroup())) {
								if (!tracker.doesClientHaveValue(serverObject, tmp)) {
									Object value = tmp.getValue(serverObject);
									tracker.setClientHasValue(serverObject, tmp);
									tracker.getQueue().queueCommand(CommandId.CommandType.SET_VALUE, serverObject, 
											tmp.getName(), tmp.serialize(serverObject, value));
								}
							}
						}
					}
				}
				if (property.isOnDemand())
					tracker.setClientHasValue(serverObject, property);
				CommandId id = new CommandId(CommandId.CommandType.FUNCTION_RETURN, serverObject, null) {
					@Override
					public boolean equals(Object obj) {
						return false;
					}
				};
				tracker.getQueue().queueCommand(id, new FunctionReturn(asyncId, result));
			}
		}

		if (!found) {
			for (ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(serverClass); type != null && !found; type = type.getSuperType()) {
				ProxyMethod[] methods = type.getMethods();
				for (int i = 0; i < methods.length; i++)
					if (methods[i].getName().equals(methodName)) {
						Method method = methods[i].getMethod();
						
						// Call the method
						Object[] values = null;
						try {
							values = readParameters(jp, method.getParameterTypes());
							Object result = method.invoke(serverObject, values);
							CommandId id = new CommandId(CommandId.CommandType.FUNCTION_RETURN, serverObject, null) {
								@Override
								public boolean equals(Object obj) {
									return false;
								}
							};
							tracker.getQueue().queueCommand(id, new FunctionReturn(asyncId, result));
						}catch(InvocationTargetException e) {
							Throwable t = e.getCause();
							log.error("Exception while invoking " + method + "(" + Helpers.toString(values) + ") on " + serverObject + " (" + requestId + "): " + t.getMessage(), t);
							throw new ProxyException(serverObject, "Exception while invoking " + method + " on " + serverObject + " for " + requestId + ": " + t.getMessage(), t);
						}catch(RuntimeException e) {
							log.error("Exception while invoking " + method + "(" + Helpers.toString(values) + ") on " + serverObject + " (" + requestId + "): " + e.getMessage(), e);
							throw new ProxyException(serverObject, "Exception while invoking " + method + " on " + serverObject + " for " + requestId + ": " + e.getMessage(), e);
						}catch(IllegalAccessException e) {
							throw new ServletException("Exception while running " + method + "(" + Helpers.toString(values) + " for " + requestId + "): " + e.getMessage(), e);
						}
						found = true;
						break;
					}
			}
		}

		if (!found)
			throw new ServletException("Cannot find method called " + methodName + " in " + (serverObject != null ? serverObject : serverClass));
		
		jp.nextToken();
	}
	
	private Object[] readParameters(JsonParser jp, Class[] types) throws IOException {
		if (types == null) {
			// Check for parameters
			if (jp.getCurrentToken() == JsonToken.FIELD_NAME &&
					jp.getCurrentName().equals("parameters") &&
					jp.nextToken() == JsonToken.START_ARRAY) {
				while (jp.nextToken() != JsonToken.END_ARRAY)
					;
			}
			return null;
		}
		Object[] values = new Object[types.length];
		Object[] params = null;
		
		// Check for parameters
		if (jp.getCurrentToken() == JsonToken.FIELD_NAME &&
				jp.getCurrentName().equals("parameters") &&
				jp.nextToken() == JsonToken.START_ARRAY) {
			
			params = readArray(jp, types);
		}
		
		for (int i = 0; i < values.length; i++)
			if (i < params.length)
				values[i] = params[i];
			else
				values[i] = null;
		
		return values;
	}
	
	/**
	 * Called when the client has disposed of
	 * @param jp
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void cmdDispose(JsonParser jp) throws ServletException, IOException {
		skipFieldName(jp, "serverIds");
		while (jp.nextToken() != JsonToken.END_ARRAY) {
			int serverId = jp.readValueAs(Integer.class);
			tracker.forget(serverId);
		}
		
		jp.nextToken();
	}
	
	/**
	 * Handles setting a server object property from the client; expects a serverId, propertyName, and a value
	 * @param jp
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void cmdSetProperty(JsonParser jp) throws ServletException, IOException {
		// Get the basics
		int serverId = getFieldValue(jp, "serverId", Integer.class);
		String propertyName = getFieldValue(jp, "propertyName", String.class);
		Object value = null;

		Proxied serverObject = getProxied(serverId);
		ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(serverObject.getClass());
		ProxyProperty prop = getProperty(type, propertyName);
		
		skipFieldName(jp, "value");
		MetaClass propClass = prop.getPropertyClass();
		if (propClass.isSubclassOf(Proxied.class)) {
			
			if (propClass.isArray() || propClass.isCollection()) {
				value = readArray(jp, propClass.getCollectionClass(), propClass.getJavaType());
				
			} else if (propClass.isMap()) {
				value = readMap(jp, propClass.getCollectionClass(), propClass.getKeyClass(), propClass.getJavaType());
				
			} else {
				Integer id = jp.readValueAs(Integer.class);
				if (id != null)
					value = getProxied(id);
			}
		} else {
			if (propClass.isArray() || propClass.isCollection()) {
				value = readArray(jp, propClass.getCollectionClass(), propClass.getJavaType());
				
			} else if (propClass.isMap()) {
				value = readMap(jp, propClass.getCollectionClass(), propClass.getKeyClass(), propClass.getJavaType());
				
			} else {
				value = jp.readValueAs(Object.class);
				if (value != null && Enum.class.isAssignableFrom(propClass.getJavaType())) {
					String str = Helpers.deserialiseEnum(value.toString());
					value = Enum.valueOf(propClass.getJavaType(), str);
				}
			}
		}
		
		setPropertyValue(type, serverObject, propertyName, value);
		jp.nextToken();
	}
	
	/**
	 * Sent when the client expires a cached property value, allowing the server property 
	 * to also its flush caches; expects a serverId and propertyName
	 * @param jp
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void cmdExpire(JsonParser jp) throws ServletException, IOException {
		// Get the basics
		int serverId = getFieldValue(jp, "serverId", Integer.class);
		String propertyName = getFieldValue(jp, "propertyName", String.class);

		Proxied serverObject = getProxied(serverId);
		ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(serverObject.getClass());
		ProxyProperty prop = getProperty(type, propertyName);
		prop.expire(serverObject);
		
		jp.nextToken();
	}
	
	/**
	 * Handles dynamic changes to a qa.data.Array instance without having a complete replacement; expects a 
	 * serverId, propertyName, type (one of "add", "remove", "order"), start, end, and optional array of items 
	 * @param jp
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void cmdEditArray(JsonParser jp) throws ServletException, IOException {
		// Get the basics
		int serverId = getFieldValue(jp, "serverId", Integer.class);
		Proxied serverObject = getProxied(serverId);
		String propertyName = getFieldValue(jp, "propertyName", String.class);
		String action = getFieldValue(jp, "type", String.class);

		tracker.beginMutate(serverObject, propertyName);
		try {
			if (action.equals("replaceAll"))
				arrayReplaceAll(jp, serverId, propertyName);
			else
				arrayUpdate(jp, serverId, propertyName);
		}finally {
			tracker.endMutate(serverObject, propertyName);
		}
	}
	
	private void arrayUpdate(JsonParser jp, int serverId, String propertyName) throws ServletException, IOException {
		// Get our info
		Proxied serverObject = getProxied(serverId);
		ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(serverObject.getClass());
		ProxyProperty prop = getProperty(type, propertyName);
		
		if (prop.getPropertyClass().isMap()) {
			Object removed = readOptionalArray(jp, ArrayList.class, "removed", prop.getPropertyClass().getKeyClass());
			Map put = readOptionalMap(jp, prop.getPropertyClass().getCollectionClass(), "put", prop.getPropertyClass().getKeyClass(), prop.getPropertyClass().getJavaType());
			
			// Quick logging
			if (log.isInfoEnabled())
				log.info("edit-array: update map: property=" + prop + ", removed=" + DiagUtils.arrayToString(removed) + ", put=" + DiagUtils.mapToString(put));
			
			Map map = ArrayUtils.getMap(serverObject, prop);

			Proxied mutating = null;
			try {
				if (map instanceof Proxied)
					tracker.beginMutate(mutating = (Proxied)map, null);
				
				ArrayUtils.removeAll(map, removed);
				if (put != null)
					map.putAll(put);
				
				// Because collection properties are objects and we change them without the serverObject's
				//	knowledge, we have to make sure we notify other trackers ourselves
				if (mutating == null)
					ProxyManager.propertyChanged(serverObject, propertyName, map, null);
			}finally {
				if (mutating != null)
					tracker.endMutate(mutating, null);
			}
			
			jp.nextToken();
		} else {
			Class clazz = prop.getPropertyClass().getJavaType();
			Object removed = readOptionalArray(jp, ArrayList.class, "removed", clazz);
			Object added = readOptionalArray(jp, ArrayList.class, "added", clazz);
			Object array = readOptionalArray(jp, ArrayList.class, "array", clazz);
			if (log.isInfoEnabled())
				log.info("edit-array: update array: property=" + prop + 
						", removed=" + DiagUtils.arrayToString(removed) + 
						", added=" + DiagUtils.arrayToString(added) + 
						", array=" + DiagUtils.arrayToString(array));
			
			Collection list;
			Object currentArray = null;
			if (prop.getPropertyClass().isCollection()) {
				list = ArrayUtils.getCollection(serverObject, prop);
			} else {
				currentArray = prop.getValue(serverObject);
				list = new ArrayList();
				ArrayUtils.addAll(list, currentArray);
			}
			Proxied mutating = null;
			try {
				if (list instanceof Proxied)
					tracker.beginMutate(mutating = (Proxied)list, null);
				
				ArrayUtils.removeAll(list, removed);
				ArrayUtils.addAll(list, added);
				if (!ArrayUtils.sameArray(list, array))
					ArrayUtils.matchOrder(list, array);
				
				if (!prop.getPropertyClass().isCollection()) {
					prop.setValue(serverObject, ArrayUtils.toArray(list, clazz));
				}
					
				// Because collection properties are objects and we change them without the serverObject's
				//	knowledge, we have to make sure we notify other trackers ourselves
				if (mutating == null)
					ProxyManager.propertyChanged(serverObject, propertyName, list, null);
				
				jp.nextToken();
			} finally {
				if (mutating != null)
					tracker.endMutate(mutating, null);
			}
		}
		
	}
	
	private void arrayReplaceAll(JsonParser jp, int serverId, String propertyName) throws ServletException, IOException {
		// Get our info
		Proxied serverObject = getProxied(serverId);
		ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(serverObject.getClass());
		ProxyProperty prop = getProperty(type, propertyName);
		
		if (prop.getPropertyClass().isMap()) {
			Map items = readOptionalMap(jp, HashMap.class, "items", prop.getPropertyClass().getKeyClass(), prop.getPropertyClass().getJavaType());
			if (log.isInfoEnabled())
				log.info("edit-array: replaceAll map: property=" + prop + ", items=" + DiagUtils.mapToString(items));
			
			Map map = ArrayUtils.getMap(serverObject, prop);
			map.clear();
			map.putAll(items);
			
			// Because collection properties are objects and we change them without the serverObject's
			//	knowledge, we have to make sure we notify other trackers ourselves
			if (!(map instanceof Proxied))
				ProxyManager.propertyChanged(serverObject, propertyName, items, null);
			
			jp.nextToken();
		} else {
			// NOTE: items is an Array!!  But because it may be an array of primitive types, we have
			//	to use java.lang.reflect.Array to access members because we cannot cast arrays of
			//	primitives to Object[]
			Object items = readOptionalArray(jp, ArrayList.class, "items", prop.getPropertyClass().getJavaType());
			if (log.isInfoEnabled())
				log.info("edit-array: replaceAll array: property=" + prop + ", items=" + DiagUtils.arrayToString(items));
			
			if (prop.getPropertyClass().isCollection()) {
				Collection list = ArrayUtils.getCollection(serverObject, prop);
				list.clear();
				ArrayUtils.addAll(list, items);
				
				// Because collection properties are objects and we change them without the serverObject's
				//	knowledge, we have to make sure we notify other trackers ourselves
				if (!(list instanceof Proxied))
					ProxyManager.propertyChanged(serverObject, propertyName, list, null);
			} else {
				prop.setValue(serverObject, items);
			}
			
			jp.nextToken();
		}
	}
	
	/**
	 * Handles creating a server object to match one created on the client; expects className,
	 * clientId, properties
	 * @param jp
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void cmdNewObject(JsonParser jp) throws ServletException, IOException {
		// Get the basics
		String className = getFieldValue(jp, "className", String.class);
		int clientId = getFieldValue(jp, "clientId", Integer.class);

		// Get the class
		Class<? extends Proxied> clazz;
		try {
			clazz = (Class<? extends Proxied>)Class.forName(className);
		} catch(ClassNotFoundException e) {
			throw new ServletException("Unknown class " + className);
		}
		ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(clazz);
		
		// Create the instance
		Proxied proxied;
		try {
			proxied = type.newInstance(clazz);
		} catch(InstantiationException e) {
			throw new ServletException("Cannot create class " + className + ": " + e.getMessage(), e);
		} catch(InvocationTargetException e) {
			throw new ServletException("Cannot create class " + className + ": " + e.getMessage(), e);
		} catch(IllegalAccessException e) {
			throw new ServletException("Cannot create class " + className + ": " + e.getMessage(), e);
		}
		
		// Get the server ID
		int serverId = tracker.addClientObject(proxied);
		
		// Remember the client ID, in case there are subsequent commands which refer to it
		tracker.registerClientObject(clientId, proxied);
		
		// Tell the client about the new ID - do this before changing properties
		tracker.invalidateCache(proxied);
		tracker.getQueue().queueCommand(CommandId.CommandType.MAP_CLIENT_ID, proxied, null, new MapClientId(serverId, clientId));
		
		// Set property values
		if (jp.nextToken() == JsonToken.FIELD_NAME) {
			if (jp.nextToken() != JsonToken.START_OBJECT)
				throw new ServletException("Unexpected properties definiton for 'new' command");
			while (jp.nextToken() != JsonToken.END_OBJECT) {
				String propertyName = jp.getCurrentName();
				jp.nextToken();
				
				// Read a Proxied object?  
				ProxyProperty prop = getProperty(type, propertyName);
				MetaClass propClass = prop.getPropertyClass();
				Object value = null;
				
				if (propClass.isArray() || propClass.isCollection()) {
					value = readArray(jp, propClass.getCollectionClass(), propClass.getJavaType());
					
				} else if (propClass.isMap()) {
					value = readMap(jp, propClass.getCollectionClass(), propClass.getKeyClass(), propClass.getJavaType());
					
				} else if (propClass.isSubclassOf(Proxied.class)) {
					Integer id = jp.readValueAs(Integer.class);
					if (id != null)
						value = getProxied(id);
					
				} else {
					value = jp.readValueAs(Object.class);
					if (value != null && Enum.class.isAssignableFrom(propClass.getJavaType())) {
						String str = Helpers.deserialiseEnum(value.toString());
						value = Enum.valueOf(propClass.getJavaType(), str);
					}
				}
				setPropertyValue(type, proxied, propertyName, value);
			}
		}
		
		// Done
		jp.nextToken();
	}
	
	/**
	 * Handles creating a server object to match one created on the client; expects className,
	 * clientId, properties
	 * @param jp
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void cmdPoll(JsonParser jp) throws ServletException, IOException {
		jp.nextToken();
	}
	
	/**
	 * Handles adding an event listener; expects serverId, eventName
	 * @param jp
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void cmdAddListener(JsonParser jp) throws ServletException, IOException {
		int serverId = getFieldValue(jp, "serverId", Integer.class);
		String eventName = getFieldValue(jp, "eventName", String.class);
		
		Proxied serverObject = getProxied(serverId);
		EventManager.addListener(serverObject, eventName, ProxyManager.getInstance());
		jp.nextToken();
	}
	
	/**
	 * Handles removing an event listener; expects serverId, eventName
	 * @param jp
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void cmdRemoveListener(JsonParser jp) throws ServletException, IOException {
		int serverId = getFieldValue(jp, "serverId", Integer.class);
		String eventName = getFieldValue(jp, "eventName", String.class);
		
		Proxied serverObject = getProxied(serverId);
		EventManager.removeListener(serverObject, eventName, ProxyManager.getInstance());
		jp.nextToken();
	}
	
	/**
	 * Returns the proxied object, by serverID or client ID
	 * @param id
	 * @return
	 */
	protected Proxied getProxied(int id) {
		Proxied proxied = tracker.getProxied(id);
		if (proxied == null)
			throw new NullPointerException("Cannot find server object with id=" + id);
		return proxied;
	}
	
	/**
	 * Finds a property in a type, recursing up the class hierarchy
	 * @param type
	 * @param name
	 * @return
	 */
	protected ProxyProperty getProperty(ProxyType type, String name) {
		while (type != null) {
			ProxyProperty prop = type.getProperties().get(name);
			if (prop != null)
				return prop;
			type = type.getSuperType();
		}
		return null;
	}
	
	/**
	 * Sets a property value, tracking which property is being set so that isSettingProperty can
	 * detect recursive sets
	 * @param type
	 * @param proxied
	 * @param propertyName
	 * @param value
	 */
	protected void setPropertyValue(ProxyType type, Proxied proxied, String propertyName, Object value) throws ProxyException {
		tracker.beginMutate(proxied, propertyName);
		try {
			ProxyProperty property = getProperty(type, propertyName);
			
			value = coerce(property.getPropertyClass().getJavaType(), value);
			
			if (!property.isSendExceptions())
				property.setValue(proxied, value);
			else {
				Object oldValue = property.getValue(proxied);
				try {
					property.setValue(proxied, value);
				} catch(Exception e) {
					tracker.getQueue().queueCommand(CommandId.CommandType.RESTORE_VALUE, proxied, propertyName, new PropertyReset(oldValue, e.getClass().getName(), e.getMessage()));
				}
			}
		}finally {
			tracker.endMutate(proxied, propertyName);
		}
	}
	
	/**
	 * Attempts to convert a native type - Jackson will interpret floating point numbers as
	 * Double, which will cause an exception if the destination only accepts float.
	 * @param clazz
	 * @param value
	 * @return
	 */
	protected Object coerce(Class clazz, Object value) {
		if (value == null)
			return null;
		if (value.getClass() == clazz)
			return value;
		
		if (value.getClass() == Double.class) {
			double val = (Double)value;
			if (clazz == float.class)
				value = (float)val;
			else if (clazz == int.class)
				value = (int)val;
			else if (clazz == long.class)
				value = (long)val;
			
		} else if (value.getClass() == Long.class) {
			long val = (Long)value;
			if (clazz == float.class)
				value = (float)val;
			else if (clazz == int.class)
				value = (int)val;
			else if (clazz == long.class)
				value = (long)val;
		} 
		
		return value;
	}
	
	/**
	 * Reads an array from JSON, where each value is of the listed in types; EG the first element
	 * is class type[0], the second element is class type[1] etc
	 * @param jp
	 * @param types
	 * @return
	 * @throws IOException
	 */
	private Object[] readArray(JsonParser jp, Class[] types) throws IOException {
		if (jp.getCurrentToken() == JsonToken.VALUE_NULL)
			return null;
		
		ArrayList result = new ArrayList();
		for (int paramIndex = 0; jp.nextToken() != JsonToken.END_ARRAY; paramIndex++) {
			Class type = null;
			if (types != null && paramIndex < types.length)
				type = types[paramIndex];
			
			if (type != null && type.isArray()) {
				if (jp.getCurrentToken() == JsonToken.VALUE_NULL)
					result.add(null);
				else if (jp.getCurrentToken() == JsonToken.START_ARRAY) {
					Object obj = readArray(jp, ArrayList.class, type.getComponentType());
					result.add(obj);
				} else
					throw new IllegalStateException("Expected array but found " + jp.getCurrentToken());
				
			} else if (type != null && Proxied.class.isAssignableFrom(type)) {
				Integer id = jp.readValueAs(Integer.class);
				if (id != null) {
					Proxied obj = getProxied(id);
					result.add(obj);
				} else
					result.add(null);
				
			} else if (type != null && Enum.class.isAssignableFrom(type)) {
				Object obj = jp.readValueAs(Object.class);
				if (obj != null) {
					String str = Helpers.deserialiseEnum(obj.toString());
					obj = Enum.valueOf(type, str);
					result.add(obj);
				}
			} else {
				Object obj = jp.readValueAs(type != null ? type : Object.class);
				result.add(obj);
			}
		}
		return result.toArray(new Object[result.size()]);
	}
	
	/**
	 * Reads an array from JSON, where each value is of the class clazz.  Note that while the result
	 * is an array, you cannot assume that it is an array of Object, or use generics because generics
	 * are always Objects - this is because arrays of primitive types are not arrays of Objects
	 * @param jp
	 * @param clazz
	 * @return
	 * @throws IOException
	 */
	private Object readArray(JsonParser jp, Class arrayClass, Class clazz) throws IOException {
		if (jp.getCurrentToken() == JsonToken.VALUE_NULL)
			return null;
		
		if (clazz == null)
			clazz = Object.class;
		boolean isProxyClass = Proxied.class.isAssignableFrom(clazz);
		ArrayList result;
		try {
			result = (ArrayList)arrayClass.newInstance();
		}catch(InstantiationException e) {
			throw new IOException("Cannot create instance of " + arrayClass + ": " + e.getMessage(), e);
		}catch(IllegalAccessException e) {
			throw new IOException("Cannot create instance of " + arrayClass + ": " + e.getMessage(), e);
		}
		for (; jp.nextToken() != JsonToken.END_ARRAY;) {
			if (isProxyClass) {
				Integer id = jp.readValueAs(Integer.class);
				if (id != null) {
					Proxied obj = getProxied(id);
					if (obj == null)
						log.fatal("Cannot read object of class " + clazz + " from id=" + id);
					else if (!clazz.isInstance(obj))
						throw new ClassCastException("Cannot cast " + obj + " class " + obj.getClass() + " to " + clazz);
					else
						result.add(obj);
				} else
					result.add(null);
			} else {
				Object obj = readSimpleValue(jp, clazz);
				result.add(obj);
			}
		}
		
		Object arr = Array.newInstance(clazz, result.size());
		for (int i = 0; i < result.size(); i++)
			Array.set(arr, i, result.get(i));
		return arr;
		//return result.toArray(Array.newInstance(clazz, result.size()));
	}
	
	/**
	 * Reads an array from JSON, where each value is of the class clazz; only if the property
	 * exists
	 * @param jp parser
	 * @param name name of the property
	 * @param clazz class of each instance
	 * @return
	 * @throws IOException
	 */
	private Object readOptionalArray(JsonParser jp, Class arrayClass, String name, Class clazz) throws IOException {
		if (jp.nextToken() == JsonToken.FIELD_NAME &&
				jp.getCurrentName().equals(name) &&
				jp.nextToken() == JsonToken.START_ARRAY) {
			return readArray(jp, arrayClass, clazz);
		}
		return null;
	}
	
	/**
	 * Reads an array from JSON, where each value is of the class clazz.  Note that while the result
	 * is an array, you cannot assume that it is an array of Object, or use generics because generics
	 * are always Objects - this is because arrays of primitive types are not arrays of Objects
	 * @param jp
	 * @param clazz
	 * @return
	 * @throws IOException
	 */
	private Map readMap(JsonParser jp, Class mapClass, Class keyClazz, Class clazz) throws IOException {
		if (jp.getCurrentToken() == JsonToken.VALUE_NULL)
			return null;
		
		if (clazz == null)
			clazz = Object.class;
		boolean isProxyClass = Proxied.class.isAssignableFrom(clazz);
		if (keyClazz == null)
			keyClazz = String.class;
		Map result;
		try {
			result = (Map)mapClass.newInstance();
		}catch(IllegalAccessException e) {
			throw new IOException("Cannot create instance of " + mapClass + ": " + e.getMessage(), e);
		}catch(InstantiationException e) {
			throw new IOException("Cannot create instance of " + mapClass + ": " + e.getMessage(), e);
		}
		for (; jp.nextToken() != JsonToken.END_OBJECT;) {
			Object key = readSimpleValue(jp, keyClazz);
			
			jp.nextToken();
			
			if (isProxyClass) {
				Integer id = jp.readValueAs(Integer.class);
				if (id != null) {
					Proxied obj = getProxied(id);
					if (!clazz.isInstance(obj))
						throw new ClassCastException("Cannot cast " + obj + " class " + obj.getClass() + " to " + clazz);
					result.put(key, obj);
				} else
					result.put(key, null);
			} else {
				Object obj = readSimpleValue(jp, clazz);
				result.put(key, obj);
			}
		}
		
		return result;
	}
	
	/**
	 * Reads a map, if the property exists
	 * @param jp parser
	 * @param name name of the property
	 * @param keyClazz class of keys
	 * @param valueClazz class of values
	 * @return
	 * @throws IOException
	 */
	private Map readOptionalMap(JsonParser jp, Class mapClass, String name, Class keyClazz, Class valueClazz) throws IOException {
		if (jp.nextToken() == JsonToken.FIELD_NAME &&
				jp.getCurrentName().equals(name) &&
				jp.nextToken() == JsonToken.START_OBJECT) {
			return readMap(jp, mapClass, keyClazz, valueClazz);
		}
		return null;
	}
	
	/**
	 * Reads the current token value, with special consideration for enums
	 * @param jp
	 * @param clazz
	 * @return
	 * @throws IOException
	 */
	private Object readSimpleValue(JsonParser jp, Class clazz) throws IOException {
		if (jp.getCurrentToken() == JsonToken.VALUE_NULL)
			return null;
		
		Object obj = null;
		if (Enum.class.isAssignableFrom(clazz)) {
			if (jp.getCurrentToken() == JsonToken.FIELD_NAME)
				obj = jp.getCurrentName();
			else
				obj = jp.readValueAs(Object.class);
			if (obj != null) {
				String str = Helpers.deserialiseEnum(obj.toString());
				obj = Enum.valueOf(clazz, str);
			}
		} else {
			if (jp.getCurrentToken() == JsonToken.FIELD_NAME)
				obj = jp.getCurrentName();
			else
				obj = jp.readValueAs(clazz);
		}
		return obj;
	}
	
	/**
	 * Gets a field value from the parser, checking that it is the type expected
	 * @param <T> The desired type of object returned
	 * @param jp the parser
	 * @param fieldName the name of the field to get
	 * @param clazz the class of the type to get 
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	private <T> T getFieldValue(JsonParser jp, String fieldName, Class<T> clazz) throws ServletException, IOException {
		skipFieldName(jp, fieldName);

		T obj = (T)jp.readValueAs(clazz);
		return obj;
	}

	/**
	 * Reads the next token and ensures that it is a field name called <code>fieldName</code>; leaves the current
	 * token on the start of the field value
	 * @param jp
	 * @param fieldName
	 * @throws ServletException
	 * @throws IOException
	 */
	private void skipFieldName(JsonParser jp, String fieldName) throws ServletException, IOException {
		if (jp.nextToken() != JsonToken.FIELD_NAME)
			throw new ServletException("Cannot find field name - looking for " + fieldName + " found " + jp.getCurrentToken() + ":" + jp.getText());
		String str = jp.getText();
		if (!fieldName.equals(str))
			throw new ServletException("Cannot find field called " + fieldName + " found " + str);
		jp.nextToken();
	}

}
