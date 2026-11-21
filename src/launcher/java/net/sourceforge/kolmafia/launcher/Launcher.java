package net.sourceforge.kolmafia.launcher;

import java.awt.GraphicsEnvironment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import javax.swing.JOptionPane;

public class Launcher {
  static final int MINIMUM_JAVA_VERSION = 25;
  static final Integer MAXIMUM_JAVA_VERSION = null;

  public static void main(String[] args) throws Throwable {
    int version = javaVersion(System.getProperty("java.specification.version"));
    if (!isSupported(version, MINIMUM_JAVA_VERSION, MAXIMUM_JAVA_VERSION)) {
      reportUnsupportedJava(args);
      System.exit(1);
    }

    MethodHandle main =
        MethodHandles.publicLookup()
            .findStatic(
                Class.forName("net.sourceforge.kolmafia.KoLmafia"),
                "main",
                MethodType.methodType(void.class, String[].class));
    main.invokeExact(args);
  }

  static int javaVersion(String specificationVersion) {
    if (specificationVersion.startsWith("1.")) {
      specificationVersion = specificationVersion.substring(2);
    }
    return Integer.parseInt(specificationVersion);
  }

  static boolean isSupported(int version, int minimum, Integer maximum) {
    return version >= minimum && (maximum == null || version <= maximum);
  }

  static String supportedVersions(int minimum, Integer maximum) {
    if (maximum == null) {
      return "Java " + minimum + " or newer";
    }
    if (maximum == minimum) {
      return "Java " + minimum;
    }
    return "Java " + minimum + " to " + maximum;
  }

  private static void reportUnsupportedJava(String[] args) {
    String message =
        "This version of KoLmafia requires "
            + supportedVersions(MINIMUM_JAVA_VERSION, MAXIMUM_JAVA_VERSION)
            + ", but you are running Java "
            + System.getProperty("java.version")
            + ".\nDownload a supported version from https://adoptium.net/";
    System.err.println(message);

    if (GraphicsEnvironment.isHeadless()
        || Arrays.stream(args).anyMatch(arg -> arg.equalsIgnoreCase("--CLI"))) {
      return;
    }

    JOptionPane.showMessageDialog(null, message, "KoLmafia", JOptionPane.ERROR_MESSAGE);
  }
}
