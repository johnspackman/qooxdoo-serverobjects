package com.zenesis.qx.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
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
    if (items instanceof Collection)
      items = ((Collection)items).toArray();
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

  public static String bytesToHex(byte[] data) {
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < data.length; i++) {
      int halfbyte = (data[i] >>> 4) & 0x0F;
      int two_halfs = 0;
      do {
        if ((0 <= halfbyte) && (halfbyte <= 9))
          buf.append((char) ('0' + halfbyte));
        else
          buf.append((char) ('a' + (halfbyte - 10)));
        halfbyte = data[i] & 0x0F;
      } while(two_halfs++ < 1);
    }
    return buf.toString();
  }

  public static String getSha1(String str) throws IOException {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] bytes = str.getBytes("UTF-8");
      md.update(bytes);
      byte[] sha1hash = md.digest();
      String hash = DiagUtils.bytesToHex(sha1hash);
      return hash;
    }catch(NoSuchAlgorithmException e) {
      throw new IOException(e.getMessage(), e);
    }
  }

  public static void writeFile(File file, String str) throws IOException {
    file.getParentFile().mkdirs();
    FileWriter fw = new FileWriter(file);
    fw.write(str);
    fw.close();
  }

  public static String zeroPad(int number) {
    if (number < 10)
      return "00" + number;
    if (number < 100)
      return "0" + number;
    return Integer.toString(number);
  }
  
}
