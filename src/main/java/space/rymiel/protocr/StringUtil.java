package space.rymiel.protocr;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class StringUtil {
  private StringUtil() {
  }

  static String titlecase(String s) {
    if (s.isEmpty()) return "";
    return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
  }

  public static String nsCrystal(String ns) {
    return Arrays.stream(ns.split("\\.")).map(StringUtil::titlecase).collect(Collectors.joining("::"));
  }

  public static String crystalTypeName(String s) {
    String ns = nsCrystal(s.substring(0, s.lastIndexOf('.')));
    return ns + "::" + s.substring(s.lastIndexOf('.') + 1);
  }

  static String crystalFilename(String name) {
    name = name.substring(0, name.lastIndexOf('.'));
    name += ".pb.cr";
    return name;
  }
}
