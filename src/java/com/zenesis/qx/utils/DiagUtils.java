package com.zenesis.qx.utils;

import java.lang.reflect.Array;
import java.util.Map;

public class DiagUtils {

	public static String mapToString(Map map) {
		String str = "";
		if (map != null) {
			for (Object key : map.keySet()) {
				if (str.length() > 0)
					str += ", ";
				str += String.valueOf(key) + "=" + String.valueOf(map.get(key));
			}
		} else
			str = "null";
		return str;
	}

	public static String arrayToString(Object items) {
		int itemsLength = Array.getLength(items);
		String str = "";
		if (items != null) {
			for (int i = 0; i < itemsLength; i++) {
				if (str.length() != 0)
					str += ", ";
				str += Array.get(items, i);
			}
		} else
			str = "null";
		return str;
	}
}
