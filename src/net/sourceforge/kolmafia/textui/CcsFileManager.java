package net.sourceforge.kolmafia.textui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.sourceforge.kolmafia.KoLConstants;

public class CcsFileManager {

  private CcsFileManager() {}

  public static byte[] getBytes(String name) {
    Path path = getPath(name);

    if (path == null) {
      return new byte[0];
    }

    if (!Files.exists(path)) {
      return new byte[0];
    }

    try {
      return Files.readAllBytes(path);
    } catch (IOException e) {
      return new byte[0];
    }
  }

  public static boolean printBytes(String name, byte[] bytes) {
    Path path = getPath(name);

    if (path == null) {
      return false;
    }

    if (!KoLConstants.CCS_LOCATION.exists() && !KoLConstants.CCS_LOCATION.mkdir()) {
      return false;
    }

    try {
      Files.write(path, bytes);
    } catch (IOException e) {
      return false;
    }

    return true;
  }

  private static Path getPath(String name) {
    if (name.contains("..") || name.contains("/") || name.contains("\\")) {
      // forbid both .. and folders
      return null;
    }

    return KoLConstants.CCS_LOCATION.toPath().resolve(name + ".ccs");
  }
}
