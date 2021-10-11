package net.sourceforge.kolmafia.textui;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.utilities.ByteBufferUtilities;
import net.sourceforge.kolmafia.utilities.RollingLinkedList;

public class DataFileCache {
  private static final RollingLinkedList<String> recentlyUsedList = new RollingLinkedList<>(500);
  private static final Map<String, Long> dataFileTimestampCache =
      Collections.synchronizedMap(new HashMap<>());
  private static final Map<String, byte[]> dataFileDataCache =
      Collections.synchronizedMap(new HashMap<>());

  public static void clearCache() {
    DataFileCache.recentlyUsedList.clear();
    DataFileCache.dataFileTimestampCache.clear();
    DataFileCache.dataFileDataCache.clear();
  }

  public static File getFile(String filename, boolean readOnly) {
    if (filename.startsWith("http://")) {
      return null;
    }

    filename = filename.substring(filename.lastIndexOf("\\") + 1);

    File[] parents;

    if (!readOnly && filename.endsWith(".ash")) {
      parents = new File[] {KoLConstants.DATA_LOCATION};
    } else {
      parents =
          new File[] {
            KoLConstants.SCRIPT_LOCATION,
            KoLConstants.RELAY_LOCATION,
            KoLConstants.DATA_LOCATION,
            KoLConstants.SESSIONS_LOCATION,
          };
    }

    for (File parent : parents) {
      File file = new File(parent, filename);
      if (checkFile(parents, file, true)) {
        try {
          if (file.getCanonicalPath().startsWith(parent.getCanonicalPath())) {
            return file;
          } else {
            KoLmafia.updateDisplay(
                KoLConstants.MafiaState.ERROR, filename + " is not within KoLmafia's directories.");
            return null;
          }
        } catch (IOException e) {
          return null;
        }
      }
    }

    File file = new File(KoLConstants.ROOT_LOCATION, filename);
    if (file.exists() && file.getParent().equals(KoLConstants.ROOT_LOCATION.getAbsolutePath())) {
      return file;
    }

    try {
      boolean withinAllowedParent = false;

      for (File parent : parents) {
        if (file.getCanonicalPath().startsWith(parent.getCanonicalPath())) {
          withinAllowedParent = true;
        }
      }

      if (withinAllowedParent) {
        return file;
      }

      if (readOnly) {
        return null;
      }

      file = new File(KoLConstants.DATA_LOCATION, filename);
      if (!file.getCanonicalPath().startsWith(KoLConstants.ROOT_LOCATION.getCanonicalPath())) {
        KoLmafia.updateDisplay(
            KoLConstants.MafiaState.ERROR, filename + " is not within KoLmafia's directories.");
        return null;
      }

      return file;
    } catch (IOException e) {
      return null;
    }
  }

  private static boolean checkFile(File[] parents, File file, boolean checkExists) {
    if (checkExists && !file.exists()) {
      return false;
    }

    file = file.getAbsoluteFile();

    try {
      File settings = KoLConstants.SETTINGS_LOCATION.getCanonicalFile();

      if (settings.equals(file.getCanonicalFile().getParentFile())) {
        return false;
      }

      while (file != null) {
        for (File parent : parents) {
          if (file.equals(parent)) {
            return true;
          }
        }

        file = file.getParentFile();
      }
    } catch (Exception e) {
    }

    return false;
  }

  public static BufferedReader getReader(final String filename) {
    if (filename.startsWith("http://") || filename.startsWith("https://")) {
      return DataUtilities.getReader("", filename);
    }
    byte[] data = DataFileCache.getBytes(filename);

    return DataUtilities.getReader(new ByteArrayInputStream(data));
  }

  public static byte[] getBytes(final String filename) {
    File input = DataFileCache.getFile(filename, true);

    if (input == null) {
      return new byte[0];
    }

    String sanitizedFilename =
        input.getPath().substring(KoLConstants.ROOT_LOCATION.getPath().length() + 1);

    long modifiedTime = input.lastModified();

    Long cacheModifiedTime = dataFileTimestampCache.get(sanitizedFilename);

    if (cacheModifiedTime != null && cacheModifiedTime == modifiedTime) {
      return dataFileDataCache.get(sanitizedFilename);
    }

    InputStream istream = null;

    if (input.exists()) {
      try {
        istream = new FileInputStream(input);
      } catch (IOException e) {
      }
    }

    if (istream == null) {
      istream = DataUtilities.getInputStream("", sanitizedFilename);
    }

    byte[] data = ByteBufferUtilities.read(istream);
    if (data == null) {
      // This check is here because a NPE was being thrown intermittently and data was the most
      // likely candidate.
      // If the cause was a lack of synchronization then this message should never be displayed.  If
      // it is displayed
      // then the hypothesis about synchronization (or the fix) was incorrect.  In any event we want
      // to see this
      // message and the NPE if data is null.
      RequestLogger.printLine("getBytes returning null for file " + sanitizedFilename + ".");
    }
    DataFileCache.updateCache(sanitizedFilename, modifiedTime, data);
    return data;
  }

  public static Value printBytes(final String filename, final byte[] data) {
    File output = DataFileCache.getFile(filename, false);

    if (output == null) {
      return DataTypes.FALSE_VALUE;
    }

    if (!output.exists()) {
      try {
        File parent = output.getParentFile();
        if (parent != null) {
          parent.mkdirs();
        }

        output.createNewFile();
      } catch (Exception e) {
        return DataTypes.FALSE_VALUE;
      }
    }

    try {
      FileOutputStream ostream = new FileOutputStream(output, false);
      ostream.write(data);
      ostream.close();
    } catch (Exception e) {
      return DataTypes.FALSE_VALUE;
    }

    DataFileCache.updateCache(filename, output.lastModified(), data);
    return DataTypes.TRUE_VALUE;
  }

  private static void updateCache(String filename, long modifiedTime, byte[] data) {
    String recentlyUsedCheck = DataFileCache.recentlyUsedList.update(filename);

    if (recentlyUsedCheck != null) {
      DataFileCache.dataFileTimestampCache.remove(filename);
      DataFileCache.dataFileDataCache.remove(filename);
    }

    DataFileCache.dataFileTimestampCache.put(filename, modifiedTime);
    DataFileCache.dataFileDataCache.put(filename, data);
  }
}
