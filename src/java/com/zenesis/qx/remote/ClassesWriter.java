package com.zenesis.qx.remote;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.clapper.util.classutil.ClassFilter;
import org.clapper.util.classutil.ClassFinder;
import org.clapper.util.classutil.ClassInfo;
import org.clapper.util.classutil.SubclassClassFilter;

/**
 * Utility class to write Proxied classes
 */
public class ClassesWriter {

  public static void writeAll(File classesRoot, File outputTo) {
    ClassFinder cf = new ClassFinder();
    cf.add(classesRoot);
    ClassFilter filter = new SubclassClassFilter(Proxied.class);
    ArrayList<ClassInfo> arr = new ArrayList<>();
    cf.findClasses(arr, filter);

    for (ClassInfo info : arr) {
      String name = info.getClassName();
      System.out.println("Writing Qooxdoo class for " + name);
      File file = new File(outputTo, classToPath(name));
      file.getParentFile().mkdirs();
      Class clazz;
      try {
        clazz = Class.forName(name);
      } catch (ClassNotFoundException e) {
        System.err.println(e.toString());
        continue;
      }
      AbstractProxyType type = (AbstractProxyType) ProxyTypeManager.INSTANCE.getProxyType(clazz);
      ClassWriter cw = type.write();
      try {
        String code = cw.getClassCode();
        try (FileWriter fw = new FileWriter(file)) {
          fw.write(code);
        }
      } catch (IOException e) {
        System.err.println("Error while processing " + clazz + ": " + e.getMessage());
      }
    }
  }

  private static String classToPath(String classname) {
    String[] segs = classname.split("\\.");
    String str = "";
    for (int i = 0; i < segs.length; i++) {
      if (i != 0)
        str += "/";
      str += segs[i];
    }
    str += ".js";
    return str;
  }

  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: ClassesWriter classes-directory output-to");
      return;
    }
    File classesRoot = new File(args[0]);
    File outputTo = new File(args[1]);

    ClassesWriter.writeAll(classesRoot, outputTo);
  }

}
