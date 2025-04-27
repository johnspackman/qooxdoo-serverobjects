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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenesis.qx.event.EventManager;
import com.zenesis.qx.remote.CommandId.CommandType;
import com.zenesis.qx.remote.annotations.EnclosingThisMethod;
import com.zenesis.qx.utils.ArrayUtils;
import com.zenesis.qx.utils.DiagUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles the request and responses for a client.
 *
 * This uses the Jackson JSON parser to pull data incrementally from the
 * request; this makes the code harder to read/write and means that we expect
 * the JSON data to occur in a particular order even though the JSON
 * specification does not allow ordering to be enforced. However, by dealing
 * with data incrementally in the way we are able to delay deciding what type of
 * data to instantiate until we have worked out where it is going - i.e. we look
 * at the types of a method's parameters and use that type information to change
 * the way we parse. In this way, we can support any arbitrary mapping between
 * JSON and Java thanks to Jackson.
 *
 * @author "John Spackman <john.spackman@zenesis.com>"
 *
 */
public class RequestHandler {

  public static final Logger log = org.apache.logging.log4j.LogManager.getLogger(RequestHandler.class);

  // Command type strings received from the client
  private static final String CMD_BOOTSTRAP = "bootstrap"; // Reset application session and get bootstrap
  private static final String CMD_CALL = "call"; // Call server object method
  private static final String CMD_DISPOSE = "dispose"; // The client has disposed of a Proxied object
  private static final String CMD_EDIT_ARRAY = "edit-array"; // Changes to an array
  private static final String CMD_EXPIRE = "expire"; // Expires a flushed property value
  private static final String CMD_LISTEN = "listen"; // Add an event listener
  private static final String CMD_NEW = "new"; // Create a new object
  private static final String CMD_POLL = "poll"; // Poll for changes (ie do nothing)
  private static final String CMD_SET = "set"; // Set a property value
  private static final String CMD_UNLISTEN = "unlisten"; // Remove an event listener

  // The request header sent by the client to validate the session
  public static final String HEADER_SESSION_ID = "x-proxymanager-sessionid";
  public static final String HEADER_SHA1 = "x-proxymanager-sha1";
  public static final String HEADER_INDEX = "x-proxymanager-requestindex";
  public static final String HEADER_CLIENT_TIME = "x-proxymanager-clienttime";
  public static final String HEADER_RETRY = "x-proxymanager-retry";

  // The response codes
  public static final int RESP_NOT_YET_READY = 100;

  // Maximum time to wait for a lock on the response
  private static int s_requestLockTimeout = 2 * 60 * 1000;

  // This class is sent as data by cmdBootstrap
  public static final class Bootstrap {
    public final Proxied bootstrap;
    public final String sessionId;
    public final int asyncId;

    public Bootstrap(Proxied bootstrap, String sessionId, int asyncId) {
      super();
      this.bootstrap = bootstrap;
      this.sessionId = sessionId;
      this.asyncId = asyncId;
    }
  }

  // This class is sent as data by cmdNewObject to change a client ID into a
  // server ID
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

  // This class is sent as data when an exception is thrown while setting a
  // property value
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

  // Where I/O log files go to, null means that they are disabled
  private static File s_temporaryDir = null;

  /**
   * @param tracker
   */
  public RequestHandler(ProxySessionTracker tracker) {
    super();
    this.tracker = tracker;
  }

  /**
   * Sets the temporary output directory (if null, disables logging and repeatable
   * requests)
   *
   * @param traceLogDir
   */
  public static void setTemporaryDir(File temporaryDir) {
    s_temporaryDir = temporaryDir;
  }

  /**
   * Returns the temporary output directory
   *
   * @return
   */
  public static File getTemporaryDir() {
    return s_temporaryDir;
  }

  /**
   * Returns the time to wait for an exclusive lock on the request, in
   * milliseconds
   *
   * @return
   */
  public static int getRequestLockTimeout() {
    return s_requestLockTimeout;
  }

  /**
   * Sets the time to wait for an exclusive lock on the request, in milliseconds.
   *
   * @param requestLockTimeout
   */
  public static void setRequestLockTimeout(int requestLockTimeout) {
    RequestHandler.s_requestLockTimeout = requestLockTimeout;
  }

  /**
   * Returns the headers
   *
   * @param request
   * @return
   */
  protected HashMap<String, String> getHeaders(HttpServletRequest request) {
    HashMap<String, String> headers = new HashMap<String, String>();
    Enumeration<String> e = request.getHeaderNames();
    while (e.hasMoreElements()) {
      String name = e.nextElement();
      String value = request.getHeader(name);
      headers.put(name.toLowerCase(), value);
    }
    return headers;
  }

  /**
   * Returns the body
   *
   * @param request
   * @return
   * @throws IOException
   */
  protected String getBody(HttpServletRequest request) throws IOException {
    StringWriter sw = null;
    sw = new StringWriter();
    Reader reader = request.getReader();
    char[] buffer = new char[32 * 1024];
    int length;
    while ((length = reader.read(buffer)) > 0) {
      sw.write(buffer, 0, length);
    }

    return sw.toString();
  }

  /**
   * Writes the reponse
   *
   * @param response
   * @param headers
   * @param body
   * @throws IOException
   */
  protected void writeResponse(HttpServletResponse response, HashMap<String, String> headers, String body,
      String acceptEncoding)
      throws IOException {
    for (String key : headers.keySet())
      response.setHeader(key, headers.get(key));

    OutputStream os = response.getOutputStream();
    /*
     * if (acceptEncoding != null && body.length() > 25 * 1024) {
     * if (acceptEncoding.indexOf("gzip") != -1) {
     * acceptEncoding = acceptEncoding.indexOf("x-gzip") != -1 ? "x-gzip" : "gzip";
     * response.addHeader("Content-Encoding", acceptEncoding);
     * os = new GZIPOutputStream(os);
     * }
     * }
     */

    Writer outputWriter = new OutputStreamWriter(os);

    outputWriter.write(body);
    outputWriter.flush();
  }

  protected void checkSessionId(String sessionId) {
    if (sessionId != null && !tracker.getSessionId().equals(sessionId))
      onWrongSessionId(tracker.getSessionId(), sessionId);
  }

  protected void onWrongSessionId(String expectedSessionId, String actualSessionId) {
    log.error("Wrong session id sent from client, expected " + expectedSessionId + " found " + actualSessionId);
    throw new IllegalArgumentException(
        "Wrong session id sent from client, expected " + expectedSessionId + " found " + actualSessionId);
  }

  protected void onInvalidRequestIndex(String strRequestIndex) {
    log.error("Invalid requestIndex sent from client, found " + strRequestIndex);
    throw new IllegalArgumentException(
        "Invalid requestIndex sent from client, found " + strRequestIndex + ", sessionId=" + tracker.getSessionId());
  }

  protected void onDuplicateRequestIndex(int requestIndex) {
    log.info("Duplicate request sent from client, requestIndex=" + requestIndex);
  }

  protected void onRequestIndexTooOld(int requestIndex) {
    log.error("Request sent from client is too old, requestIndex=" + requestIndex);
    throw new IllegalArgumentException(
        "Request sent from client is too old, requestIndex=" + requestIndex + ", sessionId=" + tracker.getSessionId());
  }

  protected void onShaMismatch(String expectedSha, String actualSha) {
    throw new IllegalArgumentException("SHA1 mismatch, found " + actualSha + " expected " + expectedSha);
  }

  protected void checkSha(String expectedSha, String body) {
    if (expectedSha != null) {
      try {
        String hash = DiagUtils.getSha1(body);
        if (!hash.equals(expectedSha))
          onShaMismatch(expectedSha, hash);
      } catch (IOException e) {
        throw new IllegalArgumentException("Unable to check SHA1 mismatch: " + e.getMessage());
      }
    }
  }

  protected String calcRequestId(int requestIndex) {
    int actualIndex = tracker.getNextRequestIndex();
    String str = tracker.getSessionId().replace(':', '_') + "/" +
        new SimpleDateFormat("dd-HHmm.ss.SSS").format(new Date()) + "-" + DiagUtils.zeroPad(requestIndex) + "-" +
        DiagUtils.zeroPad(actualIndex);
    return str;
  }

  /**
   * Handles the callback from the client; expects either an object or an array of
   * objects
   *
   * This method needs to be synchronized because if there are multiple requests
   * (where one or more are probably asynchronous) then we could serialise
   * serverObjects in a slow response and the the faster response only gets a
   * server object ID ... except that the slow response has not completed yet and
   * therefore the fast response has not told the client about the server object.
   *
   * The same is true for client IDs
   *
   * @param request
   * @param response
   * @param sessionId session id passed from the client for validation, ignored if
   *                  null
   * @throws ServletException
   * @throws IOException
   */
  public void processRequest(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    HashMap<String, String> headers = getHeaders(request);

    String str = headers.get(RequestHandler.HEADER_INDEX);
    int requestIndex = -1;
    try {
      requestIndex = Integer.parseInt(str);
    } catch (NumberFormatException e) {
      // Nothing
    }
    if (requestIndex < 0) {
      onInvalidRequestIndex(str);
      return;
    // Being sent out of order shouldn't happen
    } else if (requestIndex < tracker.getHighestRequestIndex() - 2) {
      onRequestIndexTooOld(requestIndex);
      return;
    }
    
    int retryIndex = -1;
    try {
      retryIndex = Integer.parseInt(headers.get(HEADER_RETRY));
    } catch (NumberFormatException e) {
      // Nothing
    }
    String sessionId = headers.get(HEADER_SESSION_ID);
    String expectedSha = headers.get(HEADER_SHA1);
    String strClientTime = headers.get(HEADER_CLIENT_TIME);
    String requestId = calcRequestId(requestIndex);
    try {
      tracker.setLastClientTime(new Date(Long.parseLong(strClientTime)));
    } catch (NumberFormatException e) {
      log.error("Cannot parse client time " + strClientTime + " for " + requestId);
    }

    String body = getBody(request);

    log.trace("Received sessionId=" + sessionId + ", tracker.sessionId=" + tracker.getSessionId() + ", requestIndex="
        + requestIndex);
    checkSessionId(sessionId);

    Writer writer = new StringWriter();
    if (s_temporaryDir != null && log.isTraceEnabled()) {
      Object obj = tracker.getObjectMapper().readValue(body, Object.class);
      String out = tracker.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
      DiagUtils.writeFile(new File(s_temporaryDir, "trace-logs/" + requestId + "-in.txt"), out);
    }

    if (expectedSha != null)
      checkSha(expectedSha, body);

    processRequestImpl(new StringReader(body), writer, requestId);
    String out = writer.toString();

    HashMap<String, String> respHeaders = new HashMap<String, String>();

    if (s_temporaryDir != null && log.isTraceEnabled()) {
      DiagUtils.writeFile(new File(s_temporaryDir, "trace-logs/" + requestId + "-out.txt"), out);
    }
    respHeaders.put(HEADER_INDEX, Integer.toString(requestIndex));
    if (sessionId != null && !tracker.getSessionId().equals(sessionId))
      respHeaders.put(HEADER_SESSION_ID, tracker.getSessionId());
    if (retryIndex > -1)
      respHeaders.put(HEADER_RETRY, Integer.toString(retryIndex));

    String hash = DiagUtils.getSha1(out);
    if (expectedSha != null)
      respHeaders.put(HEADER_SHA1, hash);

    try {
      writeResponse(response, respHeaders, out, request.getHeader("Accept-Encoding"));
    } catch (IOException e) {
      log.fatal("Failed to write back to client: " + e.getMessage());
    }
  }

  public void processRequestImpl(Reader request, Writer response, String requestId)
      throws ServletException, IOException {
    try {
      if (!tracker.getRequestLock().tryLock(s_requestLockTimeout, TimeUnit.MILLISECONDS))
        throw new ServletException("Timeout while waiting for request lock for " + requestId);
    } catch (InterruptedException e) {
      throw new ServletException("Exception while waiting for request lock for " + requestId + ": " + e.getMessage());
    }
    try {
      s_currentHandler.set(this);
      ObjectMapper objectMapper = tracker.getObjectMapper();
      try {
        JsonParser jp = objectMapper.getJsonFactory().createJsonParser(request);
        if (jp.nextToken() == JsonToken.START_ARRAY) {
          while (jp.nextToken() != JsonToken.END_ARRAY)
            processCommand(jp);
        } else if (jp.getCurrentToken() == JsonToken.START_OBJECT)
          processCommand(jp);

        CommandQueue queue = tracker.getQueue();
        JsonSerializable data = null;
        synchronized (queue) {
          data = queue.getDataToFlush();
        }
        if (data != null)
          objectMapper.writeValue(response, data);

      } catch (ProxyTypeSerialisationException e) {
        log.fatal("Unable to serialise type information to client for " + requestId + ": " + e.getMessage(), e);

      } catch (ProxyException e) {
        handleException(response, objectMapper, e);

      } catch (Exception e) {
        log.error("Exception during callback for " + requestId + ": " + e.getMessage(), e);
        tracker.getQueue().queueCommand(CommandType.EXCEPTION, null, null,
            new ExceptionDetails(e.getClass().getName(), e.getMessage()));
        CommandQueue queue = tracker.getQueue();
        JsonSerializable data = null;
        synchronized (queue) {
          data = queue.getDataToFlush();
        }
        if (data != null)
          objectMapper.writeValue(response, data);

      } finally {
        s_currentHandler.set(null);
      }
    } finally {
      tracker.getRequestLock().unlock();
    }
  }

  /**
   * Called to handle exceptions during processRequest
   *
   * @param response
   * @param objectMapper
   * @param e
   * @throws IOException
   */
  protected void handleException(Writer response, ObjectMapper objectMapper, ProxyException e) throws IOException {
    Throwable cause = e.getCause();
    tracker.getQueue().queueCommand(CommandType.EXCEPTION, e.getServerObject(), null,
        new ExceptionDetails(cause.getClass().getName(), cause.getMessage()));
    CommandQueue queue = tracker.getQueue();
    JsonSerializable data = null;
    synchronized (queue) {
      data = queue.getDataToFlush();
    }
    if (data != null)
      objectMapper.writeValue(response, data);
  }

  /**
   * Returns the request handler for the current thread
   *
   * @return
   */
  public static RequestHandler getCurrentHandler() {
    return s_currentHandler.get();
  }

  /**
   * Handles an object from the client; expects the object to have a property
   * "cmd" which is the type of command
   *
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
   *
   * @param jp
   */
  protected void cmdBootstrap(JsonParser jp) throws ServletException, IOException {
    tracker.resetSession();
    int asyncId = 0;
    if (jp.getCurrentToken() != JsonToken.END_OBJECT)
      asyncId = getFieldValue(jp, "asyncId", Integer.class);
    tracker.getQueue().queueCommand(CommandId.CommandType.BOOTSTRAP, null, null,
        new Bootstrap(tracker.getBootstrap(), tracker.getSessionId(), asyncId));
    jp.nextToken();
  }

  /**
   * Handles a server method call from the client; expects a serverId, methodName,
   * and an optional array of parameters
   *
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
      int serverId = (Integer) obj;
      serverObject = getProxied(serverId);
      serverClass = serverObject.getClass();
    } else if (obj != null) {
      try {
        serverClass = Class.forName(obj.toString());
      } catch (ClassNotFoundException e) {
        log.error("Cannot find server class " + obj + ": " + e.getMessage());
      }
    }

    // Onto what should be parameters
    jp.nextToken();

    // Find the method by hand - we have already guaranteed that there will not be
    // conflicting
    // method names (ie no overridden methods) but Java needs a list of parameter
    // types
    // so we do it ourselves.
    boolean found = false;

    // Check for property accessors; if serverObject is null then it's static method
    // call and
    // properties are not supported
    if (serverObject != null && methodName.length() > 3 &&
        (methodName.startsWith("get") || methodName.startsWith("set"))) {
      String name = methodName.substring(3, 4).toLowerCase();
      if (methodName.length() > 4)
        name += methodName.substring(4);
      ProxyProperty property = null;
      for (ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(serverClass); type != null; type = type
          .getSuperType()) {
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
          for (ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(serverClass); type != null; type = type
              .getSuperType()) {
            for (ProxyProperty tmp : type.getProperties().values()) {
              if (tmp.getGroup() != null && tmp.getGroup().equals(property.getGroup())) {
                if (!tracker.doesClientHaveValue(serverObject, tmp)) {
                  Object value = tmp.getValue(serverObject);
                  tracker.setClientHasValue(serverObject, tmp);
                  tracker.getQueue().queueCommand(CommandId.CommandType.SET_VALUE, serverObject, tmp.getName(),
                      tmp.serialize(serverObject, value));
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
      for (ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(serverClass); type != null &&
          !found; type = type.getSuperType()) {
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
            } catch (InvocationTargetException e) {
              Throwable t = e.getCause();
              log.error("Exception while invoking " + method + "(" + Helpers.toString(values) + ") on " + serverObject +
                  ": " + t.getMessage(), t);
              throw new ProxyException(serverObject,
                  "Exception while invoking " + method + " on " + serverObject + ": " + t.getMessage(), t);
            } catch (RuntimeException e) {
              log.error("Exception while invoking " + method + "(" + Helpers.toString(values) + ") on " + serverObject +
                  ": " + e.getMessage(), e);
              throw new ProxyException(serverObject,
                  "Exception while invoking " + method + " on " + serverObject + ": " + e.getMessage(), e);
            } catch (IllegalAccessException e) {
              throw new ServletException(
                  "Exception while running " + method + "(" + Helpers.toString(values) + "): " + e.getMessage(), e);
            }
            found = true;
            break;
          }
      }
    }

    if (!found)
      throw new ServletException(
          "Cannot find method called " + methodName + " in " + (serverObject != null ? serverObject : serverClass));

    jp.nextToken();
  }

  private Object[] readParameters(JsonParser jp, Class[] types) throws IOException {
    if (types == null) {
      // Check for parameters
      if (jp.getCurrentToken() == JsonToken.FIELD_NAME && jp.getCurrentName().equals("parameters") &&
          jp.nextToken() == JsonToken.START_ARRAY) {
        while (jp.nextToken() != JsonToken.END_ARRAY)
          ;
      }
      return null;
    }
    Object[] values = new Object[types.length];
    Object[] params = null;

    // Check for parameters
    if (jp.getCurrentToken() == JsonToken.FIELD_NAME && jp.getCurrentName().equals("parameters") &&
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
   *
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
   * Finds the observer for a Proxied object, if the object does not implement
   * ProxiedObserver then it looks for enclosing classes which do. Static
   * enclosing classes are located by looking for methods named in the form
   * "getXxxx" which have no parameters and return an instance of Proxied, or
   * which have the EnclosingThisMethod annotation.
   *
   * @param proxied
   * @return null if not found
   */
  private ProxiedObserver getObserver(Proxied proxied) {
    while (proxied != null) {
      if (proxied instanceof ProxiedObserver)
        return (ProxiedObserver) proxied;

      Class clazz = proxied.getClass();
      Class outerClazz = clazz.getEnclosingClass();
      if (outerClazz == null)
        return null;

      Object nextObject = null;
      if (Modifier.isStatic(clazz.getModifiers())) {
        Method matched = null;
        for (Method method : clazz.getMethods()) {
          if (method.getAnnotationsByType(EnclosingThisMethod.class).length > 0) {
            if (matched != null)
              throw new IllegalStateException("Too many methods marked as EnclosingThisMethod in " + clazz +
                  " (found " + matched + " and " + method + ")");
            matched = method;
          }
        }
        if (matched == null) {
          for (Method method : clazz.getMethods()) {
            String name = method.getName();
            if (method.getParameterTypes().length == 0 && name.length() > 3 && name.startsWith("get") &&
                Character.isUpperCase(name.charAt(3)) && outerClazz.isAssignableFrom(method.getReturnType())) {
              if (matched != null)
                throw new IllegalStateException("Too many methods which could provide the enclosing this in " + clazz +
                    " (found " + matched + " and " + method + ")");
              matched = method;
            }
          }
        }
        if (matched != null) {
          try {
            nextObject = matched.invoke(proxied, new Object[0]);
          } catch (InvocationTargetException e) {
            throw new IllegalStateException("Cannot get enclosing instance from " + matched + ": " + e.getMessage(), e);
          } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot get enclosing instance from " + matched + ": " + e.getMessage(), e);
          }
        }
      } else {
        Field field = null;
        Class fieldClass = clazz;
        while (field == null && fieldClass != null) {
          try {
            field = fieldClass.getDeclaredField("this$0");
          } catch(NoSuchFieldException e) {
            fieldClass = fieldClass.getSuperclass();
          }
        }
        if (field == null) {
          throw new IllegalStateException("Cannot find enclosing instance in this$0 of " + clazz);
        }
        try {
          field.setAccessible(true);
          nextObject = field.get(proxied);
        } catch (IllegalAccessException e) {
          throw new IllegalStateException(
              "Cannot get enclosing instance from this$0 of " + clazz + ": " + e.getMessage(), e);
        }
      }

      if (nextObject != null && nextObject instanceof Proxied)
        proxied = (Proxied) nextObject;
      else
        break;
    }

    return null;
  }

  /**
   * Handles setting a server object property from the client; expects a serverId,
   * propertyName, and a value
   *
   * @param jp
   * @throws ServletException
   * @throws IOException
   */
  protected void cmdSetProperty(JsonParser jp) throws ServletException, IOException {
    // Get the basics
    int serverId = getFieldValue(jp, "serverId", Integer.class);
    String propertyName = getFieldValue(jp, "propertyName", String.class);
    Object value = null;
    
    if (propertyName.equals("requiredAttachments")) {
      System.out.println("requiredAttachments");
    }

    Proxied serverObject = getProxied(serverId);
    ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(serverObject.getClass());
    ProxyProperty prop = getProperty(type, propertyName);

    skipFieldName(jp, "value");
    MetaClass propClass = prop.getPropertyClass();
    if (propClass.isSubclassOf(Proxied.class)) {
      
      if (propClass.getCollectionClass() != null && com.zenesis.qx.remote.collections.ArrayList.class.isAssignableFrom(propClass.getCollectionClass())) {
        Integer id = jp.readValueAs(Integer.class);
        if (id != null)
          value = getProxied(id);
        
      } else if (propClass.isArray() || propClass.isCollection()) {
        value = readArray(jp, propClass.getCollectionClass(), propClass.getJavaType());

      } else if (propClass.isMap()) {
        value = readMap(jp, propClass.getCollectionClass(), propClass.getKeyClass(), propClass.getJavaType());

      } else {
        Integer id = jp.readValueAs(Integer.class);
        if (id != null)
          value = getProxied(id);
      }
    } else {
      if (propClass.getCollectionClass() != null && com.zenesis.qx.remote.collections.ArrayList.class.isAssignableFrom(propClass.getCollectionClass())) {
        Integer id = jp.readValueAs(Integer.class);
        if (id != null)
          value = getProxied(id);
        
      } else if (propClass.isArray() || propClass.isCollection()) {
        value = readArray(jp, propClass.getCollectionClass(), propClass.getJavaType());

      } else if (propClass.isMap()) {
        value = readMap(jp, propClass.getCollectionClass(), propClass.getKeyClass(), propClass.getJavaType());

      } else {
        value = readSimpleValue(jp, propClass.getJavaType());
      }
    }

    setPropertyValue(type, serverObject, propertyName, value);
    jp.nextToken();
  }

  /**
   * Sent when the client expires a cached property value, allowing the server
   * property to also its flush caches; expects a serverId and propertyName
   *
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
   * Handles dynamic changes to a qa.data.Array instance without having a complete
   * replacement; expects a serverId, propertyName, type (one of "add", "remove",
   * "order"), start, end, and optional array of items
   *
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
    } finally {
      tracker.endMutate(serverObject, propertyName);
    }
  }

  private void arrayUpdate(JsonParser jp, int serverId, String propertyName) throws ServletException, IOException {
    // Get our info
    Proxied serverObject = getProxied(serverId);
    ProxiedObserver observer = getObserver(serverObject);
    ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(serverObject.getClass());
    ProxyProperty prop = getProperty(type, propertyName);

    if (prop.getPropertyClass().isMap()) {
      Object removed = readOptionalArray(jp, ArrayList.class, "removed", prop.getPropertyClass().getKeyClass());
      Map put = readOptionalExpandedMap(jp, "put", prop.getPropertyClass().getKeyClass(),
          prop.getPropertyClass().getJavaType());

      // Quick logging
      if (log.isDebugEnabled())
        log.debug("edit-array: update map: property=" + prop + ", removed=" + DiagUtils.arrayToString(removed) +
            ", put=" + DiagUtils.mapToString(put));

      Map map = ArrayUtils.getMap(serverObject, prop);

      Proxied mutating = null;
      try {
        if (map instanceof Proxied)
          tracker.beginMutate(mutating = (Proxied) map, null);

        ArrayUtils.removeAll(map, removed);
        if (put != null) {
          map.putAll(put);
        }

        // Because collection properties are objects and we change them without the
        // serverObject's
        // knowledge, we have to make sure we notify other trackers ourselves
        if (mutating == null)
          ProxyManager.propertyChanged(serverObject, propertyName, map, null);
      } finally {
        if (mutating != null)
          tracker.endMutate(mutating, null);
      }

      if (observer != null)
        observer.observeEditArray(serverObject, prop, map);

      jp.nextToken();
    } else {
      Object removed = null;
      Object added = null;
      Object order = null;
      Class clazz = prop.getPropertyClass().getJavaType();
      while (jp.nextToken() != JsonToken.END_OBJECT) {
        if (jp.currentToken() == JsonToken.FIELD_NAME) {
          String fieldName = jp.getText();
          jp.nextToken();
          if (fieldName.equals("removed"))
            removed = readArray(jp, ArrayList.class, clazz);
          else if (fieldName.equals("added"))
            added = readArray(jp, ArrayList.class, clazz);
          else if (fieldName.equals("order"))
            order = readArray(jp, ArrayList.class, clazz);
          else
            throw new IllegalArgumentException("Unexpected field name in arrayUpdate: " + fieldName);
        }
      }

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
          tracker.beginMutate(mutating = (Proxied) list, null);
        if (list instanceof com.zenesis.qx.remote.collections.ArrayList &&
            ((com.zenesis.qx.remote.collections.ArrayList) list).isDetectDuplicates() &&
            ((com.zenesis.qx.remote.collections.ArrayList) list).detectDuplicates()) {
          System.out.println("Duplicates detected before updating array");
        }

        ArrayUtils.removeAll(list, removed);
        ArrayUtils.addAll(list, added);
        
        if (order != null && !ArrayUtils.sameArray(list, order))
          ArrayUtils.matchOrder(list, order);

        if (!prop.getPropertyClass().isCollection()) {
          prop.setValue(serverObject, ArrayUtils.toArray(list, clazz));
        }

        if (log.isTraceEnabled()) {
          log.debug("edit-array: update array: property=" + prop + 
              ",\n   removed=" + DiagUtils.arrayToString(removed) +
              ",\n   added=" + DiagUtils.arrayToString(added) + 
              //",\n   array=" + DiagUtils.arrayToString(array) +
              ",\n   actual=" + DiagUtils.arrayToString(list));
        } else if (log.isDebugEnabled()) {
          log.debug("edit-array: update array: property=" + prop + 
              ", removed=" + DiagUtils.arrayToString(removed) +
              ", added=" + DiagUtils.arrayToString(added)/* + 
              ", array=" + DiagUtils.arrayToString(array)*/);
        }

        // Because collection properties are objects and we change them without the
        // serverObject's
        // knowledge, we have to make sure we notify other trackers ourselves
        if (mutating == null)
          ProxyManager.propertyChanged(serverObject, propertyName, list, null);

        if (observer != null)
          observer.observeEditArray(serverObject, prop, list);

        if (list instanceof com.zenesis.qx.remote.collections.ArrayList &&
            ((com.zenesis.qx.remote.collections.ArrayList) list).isDetectDuplicates() &&
            ((com.zenesis.qx.remote.collections.ArrayList) list).detectDuplicates()) {
          System.out.println("Duplicates detected after updating array");
        }

      } finally {
        if (mutating != null)
          tracker.endMutate(mutating, null);
      }
    }

  }

  private void arrayReplaceAll(JsonParser jp, int serverId, String propertyName) throws ServletException, IOException {
    // Get our info
    Proxied serverObject = getProxied(serverId);
    ProxiedObserver observer = getObserver(serverObject);
    ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(serverObject.getClass());
    ProxyProperty prop = getProperty(type, propertyName);

    if (prop.getPropertyClass().isMap()) {
      Map items = readOptionalMap(jp, HashMap.class, "items", prop.getPropertyClass().getKeyClass(),
          prop.getPropertyClass().getJavaType());
      if (log.isDebugEnabled())
        log.debug("edit-array: replaceAll map: property=" + prop + ", items=" + DiagUtils.mapToString(items));

      Map map = ArrayUtils.getMap(serverObject, prop);
      map.clear();
      map.putAll(items);

      // Because collection properties are objects and we change them without the
      // serverObject's
      // knowledge, we have to make sure we notify other trackers ourselves
      if (!(map instanceof Proxied))
        ProxyManager.propertyChanged(serverObject, propertyName, items, null);
      if (observer != null)
        observer.observeEditArray(serverObject, prop, map);

      jp.nextToken();
    } else {
      // NOTE: items is an Array!! But because it may be an array of primitive types,
      // we have
      // to use java.lang.reflect.Array to access members because we cannot cast
      // arrays of
      // primitives to Object[]
      Object items = readOptionalArray(jp, ArrayList.class, "items", prop.getPropertyClass().getJavaType());
      if (log.isDebugEnabled())
        log.debug("edit-array: replaceAll array: property=" + prop + ", items=" + DiagUtils.arrayToString(items));

      if (prop.getPropertyClass().isCollection()) {
        Collection list = ArrayUtils.getCollection(serverObject, prop);
        list.clear();
        ArrayUtils.addAll(list, items);

        // Because collection properties are objects and we change them without the
        // serverObject's
        // knowledge, we have to make sure we notify other trackers ourselves
        if (!(list instanceof Proxied))
          ProxyManager.propertyChanged(serverObject, propertyName, list, null);
        if (observer != null)
          observer.observeEditArray(serverObject, prop, list);
      } else {
        prop.setValue(serverObject, items);
        if (observer != null)
          observer.observeEditArray(serverObject, prop, items);
      }

      jp.nextToken();
    }
  }

  /**
   * Handles creating a server object to match one created on the client; expects
   * className, clientId, properties
   *
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
      clazz = (Class<? extends Proxied>) Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new ServletException("Unknown class " + className);
    }
    ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(clazz);

    // Create the instance
    Proxied proxied;
    try {
      proxied = type.newInstance(clazz);
    } catch (InstantiationException e) {
      throw new ServletException("Cannot create class " + className + ": " + e.getMessage(), e);
    } catch (InvocationTargetException e) {
      throw new ServletException("Cannot create class " + className + ": " + e.getMessage(), e);
    } catch (IllegalAccessException e) {
      throw new ServletException("Cannot create class " + className + ": " + e.getMessage(), e);
    }
    
    // Get the server ID
    int serverId = tracker.addClientObject(proxied);

    // Remember the client ID, in case there are subsequent commands which refer to
    // it
    tracker.registerClientObject(clientId, proxied);
    tracker.getProxy(proxied); 

    // Tell the client about the new ID - do this before changing properties
    tracker.invalidateCache(proxied);
    tracker.getQueue().queueCommand(CommandId.CommandType.MAP_CLIENT_ID, proxied, null,
        new MapClientId(serverId, clientId));

    // Set property values
    jp.nextToken();
    ArrayList<Boolean> valueTypes = null;
    while (jp.getCurrentToken() == JsonToken.FIELD_NAME) {
      jp.nextToken();
      if (jp.getCurrentName().equals("properties")) {
        if (jp.getCurrentToken() != JsonToken.START_OBJECT)
          throw new ServletException("Unexpected properties definiton for 'new' command");
        while (jp.nextToken() != JsonToken.END_OBJECT) {
          String propertyName = jp.getCurrentName();
          jp.nextToken();
  
          // Read a Proxied object?
          ProxyProperty prop = getProperty(type, propertyName);
          MetaClass propClass = prop.getPropertyClass();
          Object value = null;
          
          /*
          if (com.zenesis.qx.remote.collections.ArrayList.class.isAssignableFrom(propClass.getCollectionClass())) {
            value = getPropertyValue(type, proxied, propertyName);
            if (value == null) {
              Class arrayClass = propClass.getCollectionClass();
              try {
                value = (Collection) arrayClass.newInstance();
              } catch (InstantiationException e) {
                throw new IOException("Cannot create instance of " + arrayClass + ": " + e.getMessage(), e);
              } catch (IllegalAccessException e) {
                throw new IOException("Cannot create instance of " + arrayClass + ": " + e.getMessage(), e);
              }
            }
            com.zenesis.qx.remote.collections.ArrayList arr = (com.zenesis.qx.remote.collections.ArrayList)value;
            arr.clear();
            boolean isProxyClass = Proxied.class.isAssignableFrom(propClass.getJavaType());
            if (jp.nextToken() != JsonToken.START_OBJECT)
              throw new ServletException("Unexpected properties definiton for ArrayList in 'new' command");
            int arrayClientId = getFieldValue(jp, "serverId", Integer.class);
            String kind = getFieldValue(jp, "kind", String.class);
            if (!kind.equals("ArrayList"))
              throw new ServletException("Unexpected array kind for ArrayList in 'new' command");
            int arrayServerId = tracker.addClientObject(arr);
            tracker.invalidateCache(arr);
            tracker.getQueue().queueCommand(CommandId.CommandType.MAP_CLIENT_ID, arr, null, new MapClientId(arrayServerId, arrayClientId));
  
            // Remember the client ID, in case there are subsequent commands which refer to
            // it
            tracker.registerClientObject(arrayClientId, arr);
            
            skipFieldName(jp, "values");
            if (jp.currentToken() != JsonToken.START_ARRAY)
              throw new ServletException("Unexpected token when expecting an array of values: " + jp.currentToken());
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
                    arr.add(obj);
                } else
                  arr.add(null);
              } else {
                Object obj = readSimpleValue(jp, clazz);
                arr.add(obj);
              }
            }
          } else */
          
          if (propClass.isSubclassOf(Proxied.class)) {
            Integer id = jp.readValueAs(Integer.class);
            if (id != null)
              value = getProxied(id);
  
          } else if (propClass.isArray() || propClass.isCollection()) {
            value = readArray(jp, propClass.getCollectionClass(), propClass.getJavaType());
  
          } else if (propClass.isMap()) {
            value = readMap(jp, propClass.getCollectionClass(), propClass.getKeyClass(), propClass.getJavaType());
  
          } else {
            value = readSimpleValue(jp, propClass.getJavaType());
          }
          setPropertyValue(type, proxied, propertyName, value);
        }
        jp.nextToken();
        
      } else if (jp.getCurrentName().equals("valueTypes")) {
        jp.nextToken();
        valueTypes = new ArrayList<>();
        while (jp.getCurrentToken() != JsonToken.END_ARRAY) {
          Boolean value = jp.readValueAs(Boolean.class);
          valueTypes.add(value);
          jp.nextToken();
        }
        jp.nextToken();
        
      } else if (jp.getCurrentName().equals("values")) {
        jp.nextToken();
        ArrayList tmp = new ArrayList();
        for (int index = 0; jp.getCurrentToken() != JsonToken.END_ARRAY; index++) {
          boolean isProxyClass = valueTypes.get(index);
          if (isProxyClass) {
            Integer id = jp.readValueAs(Integer.class);
            if (id != null) {
              Proxied obj = getProxied(id);
              if (obj == null)
                log.fatal("Cannot read object of class " + clazz + " from id=" + id);
              else
                tmp.add(obj);
            } else
              tmp.add(null);
          } else {
            Object obj = readSimpleValue(jp, Object.class);
            tmp.add(obj);
          }
          jp.nextToken();
        }
        jp.nextToken();
        com.zenesis.qx.remote.collections.ArrayList arr = (com.zenesis.qx.remote.collections.ArrayList)proxied;
        arr.replace(tmp);
      }
    }
  }

  /**
   * Handles creating a server object to match one created on the client; expects
   * className, clientId, properties
   *
   * @param jp
   * @throws ServletException
   * @throws IOException
   */
  protected void cmdPoll(JsonParser jp) throws ServletException, IOException {
    jp.nextToken();
  }

  /**
   * Handles adding an event listener; expects serverId, eventName
   *
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
   *
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
   *
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
   *
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
   * Sets a property value, tracking which property is being set so that
   * isSettingProperty can detect recursive sets
   *
   * @param type
   * @param proxied
   * @param propertyName
   * @param value
   */
  protected void setPropertyValue(ProxyType type, Proxied proxied, String propertyName, Object value)
      throws ProxyException {
    tracker.beginMutate(proxied, propertyName);
    try {
      ProxyProperty property = getProperty(type, propertyName);
      MetaClass propClass = property.getPropertyClass();
      if (propertyName.equals("questionStatuses")) {
        System.out.println("questionStatuses");
      }
      
      if (propClass.isArray() || propClass.isCollection()) {
        if (com.zenesis.qx.remote.collections.ArrayList.class.isAssignableFrom(value.getClass())) {
          com.zenesis.qx.remote.collections.ArrayList arr = (com.zenesis.qx.remote.collections.ArrayList)value;
          for (int i = 0; i < arr.size(); i++) {
            Object tmp = arr.get(i);
            Object newTmp = coerce(propClass.getJavaType(), tmp);
            arr.set(i, newTmp);
          }
        } else if (ArrayList.class.isAssignableFrom(value.getClass())) {
          ArrayList arr = (ArrayList)value;
          for (int i = 0; i < arr.size(); i++) {
            Object tmp = arr.get(i);
            Object newTmp = coerce(propClass.getJavaType(), tmp);
            arr.set(i, newTmp);
          }
          
        } else {
          Collection result;
          Class arrayClass = propClass.getCollectionClass();
          try {
            result = (Collection) arrayClass.newInstance();
          } catch (InstantiationException e) {
            throw new ProxyException(proxied, "Cannot create instance of " + arrayClass + ": " + e.getMessage(), e);
          } catch (IllegalAccessException e) {
            throw new ProxyException(proxied, "Cannot create instance of " + arrayClass + ": " + e.getMessage(), e);
          }
          if (value instanceof Collection) {
            Collection src = (Collection)value;
            for (Object tmp : src) {
              Object newTmp = coerce(propClass.getJavaType(), tmp);
              result.add(newTmp);
            }
          } else {
            Object[] src = (Object[])value;
            for (Object tmp : src) {
              Object newTmp = coerce(propClass.getJavaType(), tmp);
              result.add(newTmp);
            }
          }
          value = result;
        }
        
      } else if (propClass.isMap()) {
        HashMap src = (HashMap)value;
        ArrayList keys = new ArrayList(src.keySet());
        for (Object key : keys) {
          Object newKey = coerce(propClass.getKeyClass(), key);
          Object tmpValue = src.get(key);
          Object newValue = coerce(propClass.getJavaType(), tmpValue);
          if (!newKey.equals(key)) {
            src.remove(key);
            src.put(newKey, newValue);
          } else if (newValue == null) {
            if (tmpValue != null)
              src.put(newKey, null);
          } else if (!newValue.equals(tmpValue)) {
            src.put(newKey, newValue);
          }
        }
      } else {
        value = coerce(property.getPropertyClass().getJavaType(), value);
      }

      Object oldValue = property.getValue(proxied);
      if (!property.isSendExceptions())
        property.setValue(proxied, value);
      else {
        try {
          property.setValue(proxied, value);
        } catch (Exception e) {
          tracker.getQueue().queueCommand(CommandId.CommandType.RESTORE_VALUE, proxied, propertyName,
              new PropertyReset(oldValue, e.getClass().getName(), e.getMessage()));
        }
      }
      ProxiedObserver observer = getObserver(proxied);
      if (observer != null) {
        Object setValue = property.getValue(proxied);
        if ((setValue != null && oldValue == null) || (setValue == null && oldValue != null) ||
            (setValue != null && oldValue != null && !setValue.equals(oldValue))) {
          observer.observeSetProperty(proxied, property, value, oldValue);
        }
      }
    } finally {
      tracker.endMutate(proxied, propertyName);
    }
  }
  
  protected Object getPropertyValue(ProxyType type, Proxied proxied, String propertyName) throws ProxyException {
    ProxyProperty property = getProperty(type, propertyName);
    Object value = property.getValue(proxied);
    return value;
  }

  /**
   * Attempts to convert a native type - Jackson will interpret floating point
   * numbers as Double, which will cause an exception if the destination only
   * accepts float.
   *
   * @param clazz
   * @param value
   * @return
   */
  protected Object coerce(Class targetClass, Object value) {
    if (value == null)
      return null;

    Class vClazz = value.getClass();
    if (vClazz == targetClass)
      return value;

    if (Enum.class.isAssignableFrom(targetClass)) {
        String str = Helpers.deserialiseEnum(value.toString());
        value = Enum.valueOf(targetClass, str);

    } else if (vClazz == double.class || vClazz == Double.class) {
      double val = (Double) value;
      if (targetClass == float.class || targetClass == Float.class)
        value = (float) val;
      else if (targetClass == int.class || targetClass == Integer.class)
        value = (int) val;
      else if (targetClass == long.class || targetClass == Long.class)
        value = (long) val;
      else if (targetClass == BigDecimal.class)
        value = new BigDecimal(val);
      else if (targetClass == BigInteger.class)
        value = BigInteger.valueOf((int) val);

    } else if (vClazz == float.class || vClazz == Float.class) {
      float val = (Float) value;
      if (targetClass == double.class || targetClass == Double.class)
        value = (double) val;
      else if (targetClass == int.class || targetClass == Integer.class)
        value = (int) Math.round(val);
      else if (targetClass == long.class || targetClass == Long.class)
        value = (long) Math.round(val);
      else if (targetClass == BigDecimal.class)
        value = new BigDecimal(val);
      else if (targetClass == BigInteger.class)
        value = BigInteger.valueOf((int) val);

    } else if (vClazz == long.class || vClazz == Long.class) {
      long val = (Long) value;
      if (targetClass == float.class || targetClass == Float.class)
        value = (float) val;
      else if (targetClass == double.class || targetClass == Double.class)
        value = (double) val;
      else if (targetClass == int.class || targetClass == Integer.class)
        value = (int) val;
      else if (targetClass == BigDecimal.class)
        value = new BigDecimal(val);
      else if (targetClass == BigInteger.class)
        value = BigInteger.valueOf((int) val);

    } else if (vClazz == int.class || vClazz == Integer.class) {
      int val = (Integer) value;
      if (targetClass == float.class || targetClass == Float.class)
        value = (float) val;
      else if (targetClass == double.class || targetClass == Double.class)
        value = (double) val;
      else if (targetClass == long.class || targetClass == Long.class)
        value = (long) val;
      else if (targetClass == BigDecimal.class)
        value = new BigDecimal(val);
      else if (targetClass == BigInteger.class)
        value = BigInteger.valueOf((int) val);
    }

    return value;
  }

  /**
   * Reads an array from JSON, where each value is of the listed in types; EG the
   * first element is class type[0], the second element is class type[1] etc
   *
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

      if (type != null && Document.class.isAssignableFrom(type)) {
        Document doc = readBsonDocument(jp);
        result.add(doc);

      } else if (type != null && type.isArray()) {
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

      } else {
        Object obj = readSimpleValue(jp, type != null ? type : Object.class);
        result.add(obj);
      }
    }
    return result.toArray(new Object[result.size()]);
  }

  protected Document readBsonDocument(JsonParser jp) throws IOException {
    if (jp.getCurrentToken() == JsonToken.VALUE_NULL)
      return null;
    if (jp.getCurrentToken() != JsonToken.START_OBJECT)
      throw new IllegalArgumentException("Cannot read org.bson.Document without an object to read from");
    Document result = new Document();
    while (jp.nextToken() != JsonToken.END_OBJECT) {
      String key = jp.getCurrentName();
      JsonToken token = jp.nextToken();
      Object value = null;
      if (token == JsonToken.START_OBJECT)
        value = readBsonDocument(jp);
      else if (token == JsonToken.START_ARRAY) {
        value = readArray(jp, null);
        if (value != null) {
          ArrayList arr = new ArrayList<>();
          for (Object obj : (Object[]) value) {
            arr.add(obj);
          }
          value = arr;
        }
      } else if (token != JsonToken.VALUE_NULL)
        value = readSimpleValue(jp, Object.class);
      result.append(key, value);
    }
    return result;
  }

  /**
   * Reads an array from JSON, where each value is of the class clazz. Note that
   * while the result is an array, you cannot assume that it is an array of
   * Object, or use generics because generics are always Objects - this is because
   * arrays of primitive types are not arrays of Objects
   *
   * @param jp
   * @param clazz
   * @return
   * @throws IOException
   */
  protected Object readArray(JsonParser jp, Class arrayClass, Class clazz) throws IOException {
    if (jp.getCurrentToken() == JsonToken.VALUE_NULL)
      return null;

    if (clazz == null)
      clazz = Object.class;
    boolean isProxyClass = Proxied.class.isAssignableFrom(clazz);
    Collection result;
    try {
      result = (Collection) arrayClass.newInstance();
    } catch (InstantiationException e) {
      throw new IOException("Cannot create instance of " + arrayClass + ": " + e.getMessage(), e);
    } catch (IllegalAccessException e) {
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
    int index = 0;
    for (Object obj : result)
      Array.set(arr, index++, obj);
    return arr;
    // return result.toArray(Array.newInstance(clazz, result.size()));
  }

  /**
   * Reads an array from JSON, where each value is of the class clazz; only if the
   * property exists
   *
   * @param jp    parser
   * @param name  name of the property
   * @param clazz class of each instance
   * @return
   * @throws IOException
   */
  protected Object readOptionalArray(JsonParser jp, Class arrayClass, String name, Class clazz) throws IOException {
    if (jp.nextToken() == JsonToken.FIELD_NAME && jp.getCurrentName().equals(name) &&
        jp.nextToken() == JsonToken.START_ARRAY) {
      return readArray(jp, arrayClass, clazz);
    }
    return null;
  }

  protected Map readOptionalExpandedMap(JsonParser jp, String name, Class keyClazz, Class valueClazz)
      throws IOException {
    if (jp.nextToken() == JsonToken.FIELD_NAME && jp.getCurrentName().equals(name) &&
        jp.nextToken() == JsonToken.START_OBJECT) {
      return readExpandedMap(jp, keyClazz, valueClazz);
    }
    return null;
  }

  protected Map readExpandedMap(JsonParser jp, Class keyClazz, Class clazz) throws IOException {
    if (jp.getCurrentToken() == JsonToken.VALUE_NULL)
      return null;

    if (clazz == null)
      clazz = Object.class;
    if (keyClazz == null)
      keyClazz = String.class;
    Map result = new HashMap<>();
    for (; jp.nextToken() != JsonToken.END_OBJECT;) {
      @SuppressWarnings("unused")
      Object entryId = readSimpleValue(jp, keyClazz);
      jp.nextToken();
      Map<String, Object> entryMap = readExpandedMapEntry(jp, keyClazz, clazz);
      Object key = entryMap.get("key");
      Object value = entryMap.get("value");
      result.put(key, value);
    }

    return result;
  }

  protected Map<String, Object> readExpandedMapEntry(JsonParser jp, Class keyType, Class valueType) throws IOException {
    HashMap<String, Object> map = new HashMap<>();
    for (; jp.nextToken() != JsonToken.END_OBJECT;) {
      String key = jp.getCurrentName();
      jp.nextToken();
      Class expectedType = null;
      if (key.equals("key"))
        expectedType = keyType;
      else if (key.equals("value"))
        expectedType = valueType;
      if (expectedType != null) {
        Object value = readComplexValue(jp, expectedType);
        map.put(key, value);
      }
    }

    return map;
  }

  /**
   * Reads an array from JSON, where each value is of the class clazz. Note that
   * while the result is an array, you cannot assume that it is an array of
   * Object, or use generics because generics are always Objects - this is because
   * arrays of primitive types are not arrays of Objects
   *
   * @param jp
   * @param clazz
   * @return
   * @throws IOException
   */
  protected Map readMap(JsonParser jp, Class mapClass, Class keyClazz, Class clazz) throws IOException {
    if (jp.getCurrentToken() == JsonToken.VALUE_NULL)
      return null;

    if (clazz == null)
      clazz = Object.class;
    boolean isProxyClass = Proxied.class.isAssignableFrom(clazz);
    if (keyClazz == null)
      keyClazz = String.class;
    Map result;
    try {
      result = (Map) mapClass.newInstance();
    } catch (IllegalAccessException e) {
      throw new IOException("Cannot create instance of " + mapClass + ": " + e.getMessage(), e);
    } catch (InstantiationException e) {
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
   *
   * @param jp         parser
   * @param name       name of the property
   * @param keyClazz   class of keys
   * @param valueClazz class of values
   * @return
   * @throws IOException
   */
  protected Map readOptionalMap(JsonParser jp, Class mapClass, String name, Class keyClazz, Class valueClazz)
      throws IOException {
    if (jp.nextToken() == JsonToken.FIELD_NAME && jp.getCurrentName().equals(name) &&
        jp.nextToken() == JsonToken.START_OBJECT) {
      return readMap(jp, mapClass, keyClazz, valueClazz);
    }
    return null;
  }

  /**
   * Reads the current token value, with special consideration for enums
   *
   * @param jp
   * @param clazz
   * @return
   * @throws IOException
   */
  protected Object readSimpleValue(JsonParser jp, Class clazz) throws IOException {
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

    } else if (Date.class.isAssignableFrom(clazz) || BigDecimal.class.isAssignableFrom(clazz)) {
      if (jp.getCurrentToken() == JsonToken.FIELD_NAME)
        obj = jp.getCurrentName();
      else
        obj = jp.readValueAs(Object.class);

    } else {
      if (jp.getCurrentToken() == JsonToken.FIELD_NAME)
        obj = jp.getCurrentName();
      else
        obj = jp.readValueAs(clazz);
    }

    if (obj instanceof String) {
      String str = (String) obj;
      if (str.startsWith(PREFIX) && str.endsWith(SUFFIX)) {
        str = str.substring(PREFIX.length(), str.length() - SUFFIX.length());

        if (str.startsWith("Date(") && str.endsWith(")")) {
          str = str.substring(5, str.length() - 1);
          try {
            Instant instant = Instant.parse(str);
            Date dt = Date.from(instant);
            return dt;
          } catch (Throwable e) {
            log.error("Invalid date: " + str);
            return null;
          }

        } else if (str.startsWith("BigNumber(") && str.endsWith(")")) {
          str = str.substring(10, str.length() - 1);
          try {
            return new BigDecimal(str);
          } catch (Throwable e) {
            log.error("Invalid BigNumber: " + str);
            return new BigDecimal(0);
          }

        }
      }
    }

    return obj;
  }

  public static final String PREFIX = "[__QOOXDOO_SERVER_OBJECTS__[";
  public static final String SUFFIX = "]]";

  protected Object readComplexValue(JsonParser jp, Class clazz) throws IOException {
    if (Proxied.class.isAssignableFrom(clazz)) {
      Integer id = jp.readValueAs(Integer.class);
      if (id != null) {
        Proxied obj = getProxied(id);
        if (!clazz.isInstance(obj))
          throw new ClassCastException("Cannot cast " + obj + " class " + obj.getClass() + " to " + clazz);
        return obj;
      } else
        return null;
    } else {
      Object result = readSimpleValue(jp, clazz);
      return result;
    }
  }

  /**
   * Gets a field value from the parser, checking that it is the type expected
   *
   * @param <T>       The desired type of object returned
   * @param jp        the parser
   * @param fieldName the name of the field to get
   * @param clazz     the class of the type to get
   * @return
   * @throws ServletException
   * @throws IOException
   */
  protected <T> T getFieldValue(JsonParser jp, String fieldName, Class<T> clazz) throws ServletException, IOException {
    skipFieldName(jp, fieldName);

    T obj = (T) jp.readValueAs(clazz);
    return obj;
  }

  /**
   * Reads the next token and ensures that it is a field name called
   * <code>fieldName</code>; leaves the current token on the start of the field
   * value
   *
   * @param jp
   * @param fieldName
   * @throws ServletException
   * @throws IOException
   */
  protected void skipFieldName(JsonParser jp, String fieldName) throws ServletException, IOException {
    if (jp.nextToken() != JsonToken.FIELD_NAME)
      throw new ServletException(
          "Cannot find field name - looking for " + fieldName + " found " + jp.getCurrentToken() + ":" + jp.getText());
    String str = jp.getText();
    if (!fieldName.equals(str))
      throw new ServletException("Cannot find field called " + fieldName + " found " + str);
    jp.nextToken();
  }

}
