package com.zenesis.qx.remote;

import java.util.Collection;
import java.util.Map;

import org.apache.logging.log4j.Logger;

/**
 * Misc functions to manipulate values
 * 
 * @author john
 *
 */
public class Helpers {
	
	private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(Helpers.class); 
	
	/**
	 * Converts an enum to camel case string, ie MY_ENUM_VALUE -> myEnumValue.
	 * Inverse of camelCaseToEnum
	 * @param e
	 * @return
	 */
	public static String enumToCamelCase(Enum e) {
		if (e == null)
			return null;
		StringBuilder sb = new StringBuilder(e.toString());
		char lastC = 0;
		for (int i = 0; i < sb.length(); i++) {
			char c = sb.charAt(i);
			if (c == '_') {
				sb.deleteCharAt(i);
				i--;
			} else if ((Character.isDigit(c) || Character.isUpperCase(c)) && lastC != '_')
				sb.setCharAt(i, Character.toLowerCase(c));
			else if (Character.isLowerCase(c) && lastC == '_')
				sb.setCharAt(i, Character.toUpperCase(c));
			lastC = c;
		}
		return sb.toString();
	}
	
	/**
	 * Converts a camel case string into an enum-style string, ie myEnumValue -> MY_ENUM_VALUE.
	 * Inverse of enumToCamelCase
	 * @param str
	 * @return
	 */
	public static String camelCaseToEnum(String str) {
		if (str == null)
			return null;
		StringBuilder sb = new StringBuilder(str);
		char lastC = 0;
		for (int i = 0; i < sb.length(); i++) {
			char c = sb.charAt(i);
			boolean wordBreak = false;
			if (Character.isLetter(c)) {
				if (Character.isUpperCase(c)) {
					wordBreak = true;
				}
			} else if (Character.isDigit(c)) {
				if (Character.isLetter(lastC))
					wordBreak = true;
			}
			if (wordBreak) {
				sb.insert(i, '_');
				i++; 
			}
			if (Character.isLowerCase(c))
				sb.setCharAt(i, Character.toUpperCase(c));
			lastC = c;
		}
		return sb.toString();
	}

	/**
	 * Converts the object to a string, expanding arrays and collections
	 * @param obj
	 * @return
	 */
	public static String toString(Object obj) {
		StringBuilder sb = new StringBuilder();
		toString(sb, obj);
		return sb.toString();
	}
	
	/**
	 * Converts an object to a string, expanding arrays and collections
	 * @param sb
	 * @param obj
	 */
	private static void toString(StringBuilder sb, Object obj) {
		try {
			boolean first = true;
			if (obj == null)
				sb.append("null");
			
			else if (obj instanceof Collection) {
				Collection list = (Collection)obj;
				sb.append("[ ");
				for (Object inner : list) {
					if (!first)
						sb.append(", ");
					else
						first = false;
					toString(sb, inner);
				}
				sb.append(" ]");
				
			} else if (obj instanceof Map) {
				Map map = (Map)obj;
				sb.append("{ ");
				for (Object key : map.keySet()) {
					if (!first)
						sb.append(", ");
					else
						first = false;
					sb.append('"').append(key.toString()).append("\" : ");
					toString(sb, map.get(key));
				}
				sb.append(" }");
				
			} else if (obj.getClass().isArray()) {
				Object[] list = (Object[])obj;
				sb.append("[ ");
				for (Object inner : list) {
					if (!first)
						sb.append(", ");
					else
						first = false;
					toString(sb, inner);
				}
				sb.append(" ]");
				
			} else if (obj instanceof String)
				sb.append('"').append((String)obj).append('"');
			
			else
				sb.append(obj.toString());
		}catch(Exception e) {
			log.fatal("Error in toString(sb, Object): " + e.getClass() + ": " + e.getMessage(), e);
		}
	}


}
