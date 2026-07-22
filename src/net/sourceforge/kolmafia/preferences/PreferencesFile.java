package net.sourceforge.kolmafia.preferences;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.utilities.FileUtilities;

/**
 * Controls a preference snapshot/backup/journal file ([user]_prefs.txt/.bak/.journal) and it's IO.
 * Changes are appended to the journal instead of rewriting the whole snapshot, and the journal is
 * trimmed when needed. It's removed when logging out.
 */
class PreferencesFile {
  private static final long TRIM_JOURNAL_BYTE_THRESHOLD = 10_000_000; // 10MB
  static final long JOURNAL_MAX_AGE = TimeUnit.DAYS.toMillis(1);
  // Cache of escaped-string representation per character.
  private static final String[] characterMap = new String[65536];

  private final File propertiesFile;
  private final File backupFile;
  private final File journalFile;
  // Also doubles as this instance's lock, per Collections.synchronizedSortedMap's contract.
  private final Map<String, byte[]> encodedData;

  // Exposed for tests
  long prefsFileLastSave = System.currentTimeMillis();

  private long journalBytes;

  PreferencesFile(String baseName, Map<String, byte[]> encodedData) {
    this.encodedData = encodedData;
    this.propertiesFile = new File(KoLConstants.SETTINGS_LOCATION, baseName + "_prefs.txt");
    this.backupFile = new File(KoLConstants.SETTINGS_LOCATION, baseName + "_prefs.bak");
    this.journalFile = new File(KoLConstants.SETTINGS_LOCATION, baseName + "_prefs.journal");
    this.journalBytes = journalFile.length();
  }

  boolean prefsDoesNotExist() {
    return !propertiesFile.exists();
  }

  Properties loadWithBackup() {
    synchronized (encodedData) {
      if (!propertiesFile.exists() && !backupFile.exists()) {
        return new Properties();
      }

      Properties p = PreferencesFile.loadProperties(propertiesFile);

      if (!PreferencesFile.isValidPreferencesFile(propertiesFile, p)) {
        // Something went wrong reading the preferences.
        if (backupFile.exists()) {
          KoLmafia.updateDisplay(
              propertiesFile
                  + " could not be read, loading backup. "
                  + "This will restore the last successfully opened preferences");
          // also tell system out, in case things are really fubar
          System.out.println("Prefs could not be read and backup exists, trying backup. ");

          p = PreferencesFile.loadProperties(backupFile);

          if (PreferencesFile.isValidPreferencesFile(backupFile, p)) {
            try {
              Files.copy(
                  backupFile.toPath(),
                  propertiesFile.toPath(),
                  StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {

              KoLmafia.updateDisplay(
                  "Error when restoring preferences from backup,  see session log for details");
              RequestLogger.updateSessionLog(
                  propertiesFile
                      + " could not be read and backup was used. KoLmafia was unable to copy your backup file to "
                      + "your preferences file and received error message:"
                      + ex.getMessage()
                      + "\nIf this is unexpected, please manually review your preferences and backup and repair any problems."
                      + " If you have a damaged preferences file, "
                      + "please consider creating a bug report on the forum, noting any special circumstances around "
                      + "the failure, and attaching the preferences.");
            }
          }
        } else {
          // No backup to fall back on, recover whatever complete lines were written before the
          // corruption point instead of loading a malformed line.
          try {
            byte[] safeBytes =
                FileUtilities.truncateToLastGoodLineBeforeNullByte(
                    Files.readAllBytes(propertiesFile.toPath()));
            Properties recovered = new Properties();
            try (InputStream istream = new ByteArrayInputStream(safeBytes)) {
              recovered.load(istream);
            }
            p = recovered;
            KoLmafia.updateDisplay(
                "Preferences was partially recovered from corruption, no backup exists.");
          } catch (IOException e) {
            p = new Properties();
            KoLmafia.updateDisplay("Preferences could not be read and no backup exists.");
          }
          RequestLogger.updateSessionLog(
              propertiesFile
                  + " could not be read and backup there is no backup file found. "
                  + "If this is unexpected, please manually inspect "
                  + "your preferences file and repair any problems.  If you have a damaged preferences file, "
                  + "please consider creating a bug report on the forum, noting any special circumstances around "
                  + "the failure, and attaching the preferences.");
        }
      } else {
        try {
          Files.copy(
              propertiesFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
          System.out.println("I/O Error when creating backup preferences file: " + ex.getMessage());
          RequestLogger.updateSessionLog(
              propertiesFile
                  + " backup creation failed. Please manually inspect "
                  + "your preferences and backup files and repair any problems.  If you have a damaged preferences file, "
                  + "please consider creating a bug report on the forum, noting any special circumstances around "
                  + "the failure, and attaching the preferences.");
        }
      }

      return p;
    }
  }

  private static Properties loadProperties(File file) {
    Properties properties = new Properties();
    try (InputStream istream = DataUtilities.getInputStream(file)) {
      properties.load(istream);
    } catch (IOException e) {
      System.out.println(e.getMessage() + " trying to load preferences from file.");
    }

    return properties;
  }

  /** A file is currently considered as invalid if it contains null bytes, or is empty */
  private static boolean isValidPreferencesFile(File file, Properties p) {
    if (p.isEmpty()) {
      return false;
    }
    try {
      return !FileUtilities.containsNullBytes(file);
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Applies the journal's lines, in order, onto a freshly loaded prefs.
   *
   * <p>A normal line sets a key, a line starting with "#" removes one. {@link #encodeCharacter}
   * always escapes a literal '#' as "\#", so a raw "#" is unambiguous as the removal marker.
   *
   * @return true if a journal file existed and was fully applied
   */
  boolean applyJournal(Properties properties) {
    synchronized (encodedData) {
      if (journalFile.length() == 0) {
        return false;
      }

      try {
        byte[] bytes = Files.readAllBytes(journalFile.toPath());
        byte[] safeBytes =
            FileUtilities.truncateTrailingPartialLine(
                FileUtilities.truncateToLastGoodLineBeforeNullByte(bytes));
        if (safeBytes.length != bytes.length) {
          System.out.println(journalFile + " was truncated after a corrupt trailing entry.");
        }
        try (BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(
                    new ByteArrayInputStream(safeBytes), StandardCharsets.ISO_8859_1))) {
          // We can't really define a line as "deleted", without it being possibly a real key
          // So we load each line one by one
          Properties scratch = new Properties();
          String line;
          while ((line = reader.readLine()) != null) {
            PreferencesFile.applyJournalLine(properties, scratch, line);
          }
        }
      } catch (IOException e) {
        System.out.println(e.getMessage() + " trying to load preferences journal.");
        return false;
      }
      return true;
    }
  }

  /**
   * A "#" prefixed line removes the key it encodes, otherwise the line is a normal key=value entry.
   * Reuses Properties.load() to unescape the line rather than reimplementing the escaping rules.
   */
  private static void applyJournalLine(Properties properties, Properties scratch, String line)
      throws IOException {
    if (line.isEmpty()) {
      return;
    }

    boolean removal = line.charAt(0) == '#';
    String encoded = removal ? line.substring(1) : line;

    scratch.clear();
    scratch.load(new StringReader(encoded + "\n"));

    for (String key : scratch.stringPropertyNames()) {
      if (removal) {
        properties.remove(key);
      } else {
        properties.setProperty(key, scratch.getProperty(key));
      }
    }
  }

  /**
   * Appends a changed key's line to the journal instead of rewriting the whole prefs file, then
   * trims if the journal crosses the size/age threshold.
   */
  void appendChange(String propertyName) {
    byte[] lineBytes = encodedData.get(propertyName);

    if (lineBytes == null) {
      // Removed, prepend with # to indicate it's removed.
      lineBytes = ("#" + encodeProperty(propertyName, null)).getBytes(StandardCharsets.UTF_8);
    }

    boolean shouldTrim;
    synchronized (encodedData) {
      try (OutputStream fstream = DataUtilities.getOutputStream(journalFile, true)) {
        fstream.write(lineBytes);
        journalBytes += lineBytes.length;
      } catch (IOException e) {
        System.out.println(e.getMessage() + " trying to append to preferences journal.");
      }
      shouldTrim = shouldTrimJournal();
    }

    if (shouldTrim) {
      savePrefsFile(false);
    }
  }

  /** Trim once the journal has grown too large, or it's been too long since the last one. */
  private boolean shouldTrimJournal() {
    return journalBytes >= PreferencesFile.TRIM_JOURNAL_BYTE_THRESHOLD
        || (System.currentTimeMillis() - prefsFileLastSave) >= PreferencesFile.JOURNAL_MAX_AGE;
  }

  /** Saves the current prefs to file */
  void savePrefsFile(boolean loggingOut) {
    synchronized (encodedData) {
      try (OutputStream fstream =
          new BufferedOutputStream(DataUtilities.getOutputStream(propertiesFile))) {
        for (Entry<String, byte[]> current : encodedData.entrySet()) {
          fstream.write(current.getValue());
        }
      } catch (IOException e) {
        System.out.println(e.getMessage() + " trying to write preferences as byte array.");
        // We early exit as saving filed
        return;
      }

      if (!writeLooksValid(propertiesFile)) {
        // Bad write - leave the journal alone, it's the only record of the unsaved changes.
        System.out.println(propertiesFile + " failed validation after saving, backup left as-is.");
        return;
      }

      try {
        Files.copy(
            propertiesFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        System.out.println(e.getMessage() + " trying to refresh preferences backup.");
      }

      try {
        if (loggingOut) {
          Files.deleteIfExists(journalFile.toPath());
        } else if (journalFile.exists()) {
          try (OutputStream fstream = DataUtilities.getOutputStream(journalFile)) {
            // Truncate the file to zero length.
            fstream.write(new byte[0]);
          }
        }
        journalBytes = 0;
      } catch (IOException e) {
        System.out.println(e.getMessage() + " trying to clear preferences journal.");
      }
      // We still update this, because why fail rapidly?
      prefsFileLastSave = System.currentTimeMillis();
    }
  }

  /** Same corruption check as {@link #isValidPreferencesFile}, without reparsing what we wrote. */
  private boolean writeLooksValid(File file) {
    if (encodedData.isEmpty()) {
      return false;
    }
    try {
      return !FileUtilities.containsNullBytes(file);
    } catch (IOException e) {
      return false;
    }
  }

  static String encodeProperty(String name, String value) {
    StringBuffer buffer = new StringBuffer();

    encodeString(buffer, name);

    if (value != null && !value.isEmpty()) {
      buffer.append("=");
      encodeString(buffer, value);
    }
    buffer.append(KoLConstants.LINE_BREAK);

    return buffer.toString();
  }

  private static void encodeString(StringBuffer buffer, String string) {
    int length = string.length();

    for (int i = 0; i < length; ++i) {
      char ch = string.charAt(i);
      encodeCharacter(ch);
      buffer.append(characterMap[ch]);
    }
  }

  private static void encodeCharacter(char ch) {
    if (characterMap[ch] != null) {
      return;
    }

    switch (ch) {
      case '\t' -> {
        characterMap[ch] = "\\t";
        return;
      }
      case '\n' -> {
        characterMap[ch] = "\\n";
        return;
      }
      case '\f' -> {
        characterMap[ch] = "\\f";
        return;
      }
      case '\r' -> {
        characterMap[ch] = "\\r";
        return;
      }
      case '\\', '=', ':', '#', '!' -> {
        characterMap[ch] = "\\" + ch;
        return;
      }
    }

    characterMap[ch] =
        (ch > 0x0019 && ch < 0x007f)
            ? String.valueOf(ch)
            : (ch < 0x0010)
                ? "\\u000" + Integer.toHexString(ch)
                : (ch < 0x0100)
                    ? "\\u00" + Integer.toHexString(ch)
                    : (ch < 0x1000)
                        ? "\\u0" + Integer.toHexString(ch)
                        : "\\u" + Integer.toHexString(ch);
  }
}
