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
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Simple wrapper for Jackson ObjectMapper that uses our custom de/serialisation
 * factories and adds a few helper methods.
 *
 * @author <a href="mailto:john.spackman@zenesis.com">John Spackman</a>
 */
public class ProxyObjectMapper extends BasicObjectMapper {

  @SuppressWarnings("unused")
  private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ProxyObjectMapper.class);

  private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
  private static final DateTimeFormatter DF_LOCALDATETIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);

  /*
   * Serialises a Date as the JS equivalent
   */
  private static final class DateSerializer extends JsonSerializer<Date> {

    /*
     * (non-Javadoc)
     *
     * @see
     * com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object,
     * com.fasterxml.jackson.core.JsonGenerator,
     * com.fasterxml.jackson.databind.SerializerProvider)
     */
    @Override
    public void serialize(Date value, JsonGenerator gen, SerializerProvider sp)
        throws IOException, JsonProcessingException {
      if (value == null)
        gen.writeNull();
      else {
        String str;
        try {
          str = "new Date(\"" + DF.format(value) + "\")";
        } catch(Exception e) {
          String strValue = "(exception)";
          try {
            strValue = value.toString();
          } catch(Exception e2) {
            //
          }
          log.fatal("Exception serializing date, value=" + strValue + ", exception=" + e.getMessage());
          str = "null";
        }
        gen.writeRawValue(str);
      }
    }
  }

  /*
   * Serialises a Date as the JS equivalent
   */
  private static final class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

    /*
     * (non-Javadoc)
     *
     * @see
     * com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object,
     * com.fasterxml.jackson.core.JsonGenerator,
     * com.fasterxml.jackson.databind.SerializerProvider)
     */
    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider sp)
        throws IOException, JsonProcessingException {
      if (value == null)
        gen.writeNull();
      else {
        String str;
        try {
          str = "com.zenesis.qx.remote.ProxyManager.parseDate(\"" + DF_LOCALDATETIME.format(value) + "\")";
        } catch(Exception e) {
          String strValue = "(exception)";
          try {
            strValue = value.toString();
          } catch(Exception e2) {
            //
          }
          log.fatal("Exception serializing date, value=" + strValue + ", exception=" + e.getMessage());
          str = "null";
        }
        gen.writeRawValue(str);
      }
    }
  }

  private static final class BsonSerializer extends JsonSerializer<Document> {

    @Override
    public void serialize(Document value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      String str = value.toJson();
      gen.writeRawValue(str);
    }
  }

  /*
   * Serialises a BigDecimal as the JS equivalent
   */
  private static final class BigDecimalSerializer extends JsonSerializer<BigDecimal> {

    /*
     * (non-Javadoc)
     *
     * @see
     * com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object,
     * com.fasterxml.jackson.core.JsonGenerator,
     * com.fasterxml.jackson.databind.SerializerProvider)
     */
    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider sp)
        throws IOException, JsonProcessingException {
      if (value == null)
        gen.writeNull();
      else
        gen.writeRawValue("new BigNumber(\"" + value + "\")");
    }
  }

  /** the tracker */
  private final ProxySessionTracker tracker;

  /**
   * Constructor
   *
   * @param tracker
   */
  public ProxyObjectMapper(ProxySessionTracker tracker) {
    this(tracker, true, new File("."));
  }

  /**
   * Constructor
   *
   * @param tracker
   * @param indent  whether to indent JSON
   */
  public ProxyObjectMapper(ProxySessionTracker tracker, boolean indent) {
    this(tracker, indent, null);
  }

  /**
   * Constructor
   *
   * @param tracker
   * @param indent  whether to indent JSON
   * @param rootDir root directory to serialise all File's as relative to
   */
  public ProxyObjectMapper(ProxySessionTracker tracker, boolean indent, File rootDir) {
    super(indent, rootDir == null ? new File(".") : null);
    this.tracker = tracker;
  }

  @Override
  protected void addToModule(SimpleModule module) {
    module.addSerializer(Document.class, new BsonSerializer());
    module.addSerializer(Proxied.class, new ProxiedSerializer());
    module.addDeserializer(Proxied.class, new ProxiedDeserializer());
    module.addSerializer(Date.class, new DateSerializer());
    module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
    module.addSerializer(BigDecimal.class, new BigDecimalSerializer());
  }

  /**
   * @return the tracker
   */
  public ProxySessionTracker getTracker() {
    return tracker;
  }
}
