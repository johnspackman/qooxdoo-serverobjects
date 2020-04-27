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
import java.lang.reflect.Field;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.json.JsonWriteContext;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.zenesis.qx.remote.BasicObjectMapper.EnumDeserializer;
import com.zenesis.qx.remote.BasicObjectMapper.EnumSerializer;

/**
 * Simple wrapper for Jackson ObjectMapper that uses our custom de/serialisation factories and adds a few helper
 * methods.
 * 
 * @author <a href="mailto:john.spackman@zenesis.com">John Spackman</a>
 */
@SuppressWarnings("serial")
public class BasicObjectMapper extends ObjectMapper {
	
	@SuppressWarnings("unused")
	private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(BasicObjectMapper.class);
	
	/*
	 * Serialises a string, encoding unicode characters
	 */
	protected static final class StringSerializer extends JsonSerializer<String> {

		final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
		final int[] ESCAPE_CODES = CharTypes.get7BitOutputEscapes();

		/* (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
		 */
		@Override
		public void serialize(String value, JsonGenerator gen, SerializerProvider sp) throws IOException, JsonProcessingException {
			int status = ((JsonWriteContext) gen.getOutputContext()).writeValue();
			switch (status) {
			case JsonWriteContext.STATUS_OK_AFTER_COLON:
				gen.writeRaw(':');
				break;
				
			case JsonWriteContext.STATUS_OK_AFTER_COMMA:
				gen.writeRaw(',');
				break;
				
			case JsonWriteContext.STATUS_EXPECT_NAME:
				throw new JsonGenerationException("Can not write string value here");
			}
			gen.writeRaw('"');
			for (char c : value.toCharArray()) {
				if (c >= 0x80)
					writeUnicodeEscape(gen, c); // use generic escaping for all
												// non US-ASCII characters
				else {
					// use escape table for first 128 characters
					int code = (c < ESCAPE_CODES.length ? ESCAPE_CODES[c] : 0);
					if (code == 0)
						gen.writeRaw(c); // no escaping
					else if (code < 0)
						writeUnicodeEscape(gen, (char) (-code - 1)); // generic escaping
					else
						writeShortEscape(gen, (char) code); // short escaping (\n \t ...)
				}
			}
			gen.writeRaw('"');
		}
		
		private void writeUnicodeEscape(JsonGenerator gen, char c)
				throws IOException {
			gen.writeRaw('\\');
			gen.writeRaw('u');
			gen.writeRaw(HEX_CHARS[(c >> 12) & 0xF]);
			gen.writeRaw(HEX_CHARS[(c >> 8) & 0xF]);
			gen.writeRaw(HEX_CHARS[(c >> 4) & 0xF]);
			gen.writeRaw(HEX_CHARS[c & 0xF]);
		}

		private void writeShortEscape(JsonGenerator gen, char c)
				throws IOException {
			gen.writeRaw('\\');
			gen.writeRaw(c);
		}
	}

	/*
	 * Serialises enums in camelCase
	 */
	public static final class EnumSerializer extends JsonSerializer<Enum> {
		/* (non-Javadoc)
		 * @see org.codehaus.jackson.map.JsonSerializer#serialize(java.lang.Object, org.codehaus.jackson.JsonGenerator, org.codehaus.jackson.map.SerializerProvider)
		 */
		@Override
		public void serialize(Enum value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
			if (value == null)
				jgen.writeNull();
			else
				jgen.writeString(Helpers.serialiseEnum(value));
		}

	};
	
	public static final class EnumDeserializer extends JsonDeserializer<Enum> {

		@Override
		public Enum deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			String value = jp.getText();
			if (value == null || value.length() == 0)
				return null;
			JsonStreamContext parsingContext = jp.getParsingContext();
			Object currentValue = parsingContext.getCurrentValue();
			String name = parsingContext.getCurrentName();
			Field field = null;
	        try {
	            field= currentValue.getClass().getField(name);
	        }catch(NoSuchFieldException e) {
	            // Nothing
	        }
			if (field != null) {
	            String str = Helpers.deserialiseEnum(value);
	            try {
	                Enum result = Enum.valueOf((Class)field.getType(), str);
	                return result;
	            }catch(IllegalArgumentException e) {
	                log.fatal("Cannot decode enum value " + value + " for " + field.getType() + " in " + currentValue);
	            }
			} else {
                log.fatal("Cannot find enum type for " + value + " in " + currentValue);
			}
			return null;
		}
		
	}
	
	/*
	 * Serialises Maps
	 */
	protected static final class MapSerializer extends JsonSerializer<Map> {
		
		/* (non-Javadoc)
		 * @see org.codehaus.jackson.map.JsonSerializer#serialize(java.lang.Object, org.codehaus.jackson.JsonGenerator, org.codehaus.jackson.map.SerializerProvider)
		 */
		@Override
		public void serialize(Map map, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
			if (map == null)
				jgen.writeNull();
			else {
				jgen.writeStartObject();
				for (Object key : map.keySet()) {
					if (key == null)
						continue;
					if (key instanceof Enum)
						jgen.writeFieldName(Helpers.serialiseEnum((Enum)key));
					else
						jgen.writeFieldName(key.toString());
					Object value = map.get(key);
					if (value == null)
						jgen.writeNull();
					else
						jgen.writeObject(value);
				}
				jgen.writeEndObject();
			}
		}

	};
	
	/*
	 * Serialises a file, but only showing the part of the path relative to the "root" dir of
	 * the web application
	 */
	protected static final class FileSerializer extends JsonSerializer<File> {
		
		private final String localPrefix;
		
		public FileSerializer(File localDir) {
			this.localPrefix = localDir == null ? null : localDir.getAbsolutePath() + File.separatorChar;
		}

		/* (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
		 */
		@Override
		public void serialize(File value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
			if (value == null)
				jgen.writeNull();
			else {
				String str = value.getAbsolutePath();
				if (localPrefix == null) {
					jgen.writeString(str);
					log.warn("Serialising absolute path to client: " + str);
				} else {
					int len = localPrefix.length();
					if (len < str.length() && str.substring(0, len).equalsIgnoreCase(localPrefix))
						jgen.writeString(str.substring(len));
					else
						jgen.writeString(str);
				}
			}
		}
	}
	
	/*
	 * Deserialises a file, keeping it relative to the root of the web application
	 */
	protected static final class FileDeserializer extends JsonDeserializer<File> {
		
		private final File localDir;
		
		public FileDeserializer(File localDir) {
			this.localDir = localDir;
		}

		/* (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
		 */
		@Override
		public File deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			String value = jp.getText();
			if (value == null || value.length() == 0)
				return null;
			if (localDir == null) {
                log.error("Refusing to Deserialise absolute path from client: " + value);
				throw new IOException("Refusing to Deserialise absolute path from client: " + value);
			}
			if (File.separatorChar == '/' && value.charAt(0) == '/')
				return new File(value);
			if (File.separatorChar == '\\' && value.length() > 1 && value.charAt(1) == ':')
				return new File(value);
			File file = new File(localDir, value);
			return file;
		}

	}
	
	/**
	 * Constructor
	 * @param tracker
	 */
	public BasicObjectMapper() {
		this(true, null);
	}
	
	/**
	 * Constructor
	 * @param tracker
	 * @param indent whether to indent JSON
	 */
	public BasicObjectMapper(boolean indent) {
		this(indent, null);
	}
		
	/**
	 * Constructor
	 * @param tracker
	 * @param indent whether to indent JSON
	 * @param rootDir root directory to serialise all File's as relative to
	 */
	public BasicObjectMapper(boolean indent, File rootDir) {
		super();
		if (indent)
			this.enable(SerializationFeature.INDENT_OUTPUT);
		
		createEnumModule();
		
		SimpleModule module = new SimpleModule("ProxyObjectMapper2", Version.unknownVersion());
		module.addSerializer(String.class, new StringSerializer());
		module.addSerializer(File.class, new FileSerializer(rootDir));
		module.addDeserializer(File.class, new FileDeserializer(rootDir));
		module.addSerializer(Map.class, new MapSerializer());
		module.addSerializer(Proxied.class, new ProxiedSerializer());
		module.addDeserializer(Proxied.class, new ProxiedDeserializer());
		addToModule(module);
		registerModule(module);
	}
    
    protected void createEnumModule() {
        SimpleModule module = new SimpleModule("ProxyObjectMapper1", Version.unknownVersion());
        module.addSerializer(Enum.class, new EnumSerializer());
        
        // This DeSerializer is beta because of difficulties finding out what enum to deserialise
        //    as; that problem may have been solved, but RequestHandler still has the code that
        //    figures it out, so it is not on by default here.
        //module.addDeserializer(Enum.class, new EnumDeserializer());
        registerModule(module);
    }
    
    /**
     * Called to add to the module
     * @param module
     */
    protected void addToModule(SimpleModule module) {
        // Nothing
    }
    
	/**
	 * Enables or disabled quoted field names
	 * @param set
	 */
	public void setQuoteFieldNames(boolean set) {
		if (set)
			getJsonFactory().enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
		else
			getJsonFactory().disable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
	}
	
	/**
	 * Whether field names will be quoted
	 * @return
	 */
	public boolean isQuoteFieldNames() {
		return getJsonFactory().isEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
	}

}
