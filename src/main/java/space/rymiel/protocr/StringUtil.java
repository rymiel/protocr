package space.rymiel.protocr;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class StringUtil {
  private StringUtil() {
  }

  /**
   * Converts a Protobuf namespaced name to a Crystal namespaced name.
   *
   * @param input Protobuf name (e.g., "foo.bar_baz.Message")
   * @return Crystal name (e.g., "Foo::BarBaz::Message")
   */
  public static String crystalTypeName(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }

    String[] segments = input.split("\\.", -1);
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < segments.length; i++) {
      if (i > 0) {
        result.append("::");
      }
      result.append(snakeToPascalCase(segments[i]));
    }

    return result.toString();
  }

  /**
   * Converts a snake_case string to PascalCase.
   *
   * @param input snake_case string (e.g., "foo_bar")
   * @return PascalCase string (e.g., "FooBar")
   */
  private static String snakeToPascalCase(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }

    StringBuilder result = new StringBuilder();
    boolean capitalizeNext = true;

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);

      if (c == '_') {
        capitalizeNext = true;
      } else if (capitalizeNext) {
        result.append(Character.toUpperCase(c));
        capitalizeNext = false;
      } else {
        result.append(c);
      }
    }

    return result.toString();
  }

  static String crystalFilename(String name) {
    name = name.substring(0, name.lastIndexOf('.'));
    name += ".pb.cr";
    return name;
  }
}
