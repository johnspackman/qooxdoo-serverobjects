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

import java.io.IOException;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.zenesis.qx.remote.CommandId.CommandType;

/**
 * Simplistic but functional QueueWriter; all property values are sent first,
 * followed by events. Duplicates are merged.
 *
 * @author John Spackman [john.spackman@zenesis.com]
 */
public class SimpleQueue implements CommandQueue {

  private boolean needsFlush;
  private LinkedHashMap<CommandId, Object> values = new LinkedHashMap<CommandId, Object>();

  /*
   * (non-Javadoc)
   *
   * @see com.zenesis.qx.remote.CommandQueue#queueCommand(com.zenesis.qx.remote.
   * CommandId, java.lang.Object)
   */
  @Override
  public synchronized void queueCommand(CommandType type, Object object, String propertyName, Object data) {
    CommandId id = new CommandId(type, object, propertyName);
    queueCommand(id, data);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.zenesis.qx.remote.CommandQueue#queueCommand(com.zenesis.qx.remote.
   * CommandId, java.lang.Object)
   */
  @Override
  public synchronized void queueCommand(CommandId id, Object data) {
    if (id.type == CommandType.BOOTSTRAP && !values.isEmpty()) {
      LinkedHashMap<CommandId, Object> tmp = new LinkedHashMap<CommandId, Object>();
      tmp.put(id, data);
      tmp.putAll(values);
      values = tmp;
    } else {
      values.put(id, data);
    }
  }

  @Override
  public synchronized Object getCommand(CommandType type, Object object, String propertyName) {
    CommandId id = new CommandId(type, object, propertyName);
    return values.get(id);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.zenesis.qx.remote.Queue#hasDataToFlush()
   */
  @Override
  public synchronized boolean hasDataToFlush() {
    return !values.isEmpty();
  }

  @Override
  public synchronized JsonSerializable getDataToFlush() {
    if (values.isEmpty())
      return null;
    needsFlush = false;
    return new QueueSerializable(values);
  }

  private static final class QueueSerializable implements JsonSerializable {

    private final LinkedHashMap<CommandId, Object> values;

    public QueueSerializable(LinkedHashMap<CommandId, Object> values) {
      this.values = new LinkedHashMap<CommandId, Object>();
      this.values.putAll(values);
      values.clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.codehaus.jackson.map.JsonSerializable#serialize(org.codehaus.jackson.
     * JsonGenerator, org.codehaus.jackson.map.SerializerProvider)
     */
    @Override
    public synchronized void serialize(JsonGenerator gen, SerializerProvider sp)
        throws IOException, JsonProcessingException {
      gen.writeStartArray();
      while (!values.isEmpty()) {
        CommandId id = values.keySet().iterator().next();
        Object data = values.remove(id);

        if (id.type == CommandType.DEFINE) {
          ProxyType type = (ProxyType) id.object;
          ProxySessionTracker tracker = ((ProxyObjectMapper) gen.getCodec()).getTracker();
          if (tracker.isTypeDelivered(type))
            continue;
        }

        gen.writeStartObject();
        gen.writeStringField("type", id.type.remoteId);
        if (id.object != null)
          gen.writeObjectField("object", id.object);
        if (id.name != null)
          gen.writeObjectField("name", id.name);
        if (data != null)
          gen.writeObjectField("data", data);
        gen.writeEndObject();
      }
      gen.writeEndArray();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.fasterxml.jackson.databind.JsonSerializable#serializeWithType(com.
     * fasterxml.jackson.core.JsonGenerator,
     * com.fasterxml.jackson.databind.SerializerProvider,
     * com.fasterxml.jackson.databind.jsontype.TypeSerializer)
     */
    @Override
    public synchronized void serializeWithType(JsonGenerator gen, SerializerProvider sp, TypeSerializer ts)
        throws IOException, JsonProcessingException {
      serialize(gen, sp);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see com.zenesis.qx.remote.Queue#needsFlush()
   */
  @Override
  public synchronized boolean needsFlush() {
    return needsFlush;
  }

}
