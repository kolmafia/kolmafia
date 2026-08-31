package net.sourceforge.kolmafia.preferences;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
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
 * Controls a preference snapshot/backup/journal file ([user]_prefs.txt/.bak/.journal) and its IO.
 * Changes are appended to the journal instead of rewriting the whole snapshot, and the journal is
 * trimmed when needed. It's removed when logging out.
 */
class PreferencesFile {
  private static final long TRIM_JOURNAL_BYTE_THRESHOLD = 10_000_000; // 10MB
  static final long JOURNAL_MAX_AGE = TimeUnit.DAYS.toMillis(1);

  private final File
      propertiesFile; // Our live preferences file, what each new session is loading from
  private final File backupFile; // The previous version of our preferences file
  private final File journalFile; // A record of our preferences that's appended as they change
  private final File tempFile; // The file that is atomically saved
  private final Map<String, byte[]> encodedData;

  // Exposed for tests
  long prefsFileLastSave = System.currentTimeMillis();

  private long journalBytes;

  PreferencesFile(String baseName, Map<String, byte[]> encodedData) {
    this.encodedData = encodedData;
    this.propertiesFile = new File(KoLConstants.SETTINGS_LOCATION, baseName + "_prefs.txt");
    this.backupFile = new File(KoLConstants.SETTINGS_LOCATION, baseName + "_prefs.bak");
    this.journalFile = new File(KoLConstants.SETTINGS_LOCATION, baseName + "_prefs.journal");
    this.tempFile = new File(propertiesFile.getParentFile(), propertiesFile.getName() + ".tmp");
    this.journalBytes = journalFile.length();
  }

  boolean prefsDoesNotExist() {
    return !propertiesFile.exists();
  }

  Properties loadWithBackup() {
    if (!propertiesFile.exists() && !backupFile.exists() && !tempFile.exists()) {
      return new Properties();
    }

    Properties p = loadProperties(propertiesFile);

    if (!isValidPreferencesFile(propertiesFile, p)) {
      // Something went wrong reading the preferences.
      boolean recoveredData = false;

      if (backupFile.exists()) {
        KoLmafia.updateDisplay(
            propertiesFile
                + " could not be read, loading backup. "
                + "This will restore the last successfully opened preferences");
        // also tell system out, in case things are really fubar
        System.out.println("Prefs could not be read and backup exists, trying backup. ");

        p = loadProperties(backupFile);

        if (isValidPreferencesFile(backupFile, p)) {
          recoveredData = true;
          try {
            Files.copy(
                backupFile.toPath(), propertiesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
      }

      // Our prefs.txt is bad, and so is our .bak
      // We're trying our .tmp now, this is unlikely to ever occur
      if (!recoveredData && tempFile.exists()) {
        KoLmafia.updateDisplay(
            propertiesFile
                + " and backup could not be read, loading temporary file. This will restore the last saved preferences");
        System.out.println(
            "Prefs and backup could not be read and temp exists, trying temporary file. ");

        p = loadProperties(tempFile);

        if (isValidPreferencesFile(tempFile, p)) {
          recoveredData = true;
          try {
            Files.copy(
                tempFile.toPath(), propertiesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
          } catch (IOException ex) {
            KoLmafia.updateDisplay(
                "Error when restoring preferences from temp file, see session log for details");
            RequestLogger.updateSessionLog(
                propertiesFile
                    + " and backup could not be read. KoLmafia was unable to restore your preferences and received error message:"
                    + ex.getMessage());
          }
        }
      }

      if (!recoveredData) {
        // No valid backup or temp to fall back on, recover whatever complete lines were written
        // before the corruption point instead of loading a malformed line.
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
              "Preferences was partially recovered from corruption, no valid backup exists.");
        } catch (IOException e) {
          p = new Properties();
          KoLmafia.updateDisplay("Preferences could not be read and no valid backup exists.");
        }
        RequestLogger.updateSessionLog(
            propertiesFile
                + " could not be read and no valid backup file was found. "
                + "If this is unexpected, please manually inspect "
                + "your preferences file and repair any problems.  If you have a damaged preferences file, "
                + "please consider creating a bug report on the forum, noting any special circumstances around "
                + "the failure, and attaching the preferences.");
      }
    }

    return p;
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
   * <p>A normal line sets a key, a line starting with "#" removes one. Preferences#encodeCharacter
   * always escapes a literal '#' as "\#", so a raw "#" is unambiguous as the removal marker.
   *
   * @return true if a journal file existed and was fully applied
   */
  boolean applyJournal(Properties properties) {
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
          applyJournalLine(properties, scratch, line);
        }
      }
    } catch (IOException e) {
      System.out.println(e.getMessage() + " trying to load preferences journal.");
      return false;
    }
    return true;
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
      lineBytes =
          ("#" + Preferences.encodeProperty(propertyName, null)).getBytes(StandardCharsets.UTF_8);
    }

    try (OutputStream fstream = DataUtilities.getOutputStream(journalFile, true)) {
      fstream.write(lineBytes);
      journalBytes += lineBytes.length;
    } catch (IOException e) {
      System.out.println(e.getMessage() + " trying to append to preferences journal.");
    }

    if (shouldTrimJournal()) {
      savePrefsFile(false);
    }
  }

  /** Trim once the journal has grown too large, or it's been too long since the last one. */
  private boolean shouldTrimJournal() {
    return journalBytes >= TRIM_JOURNAL_BYTE_THRESHOLD
        || (System.currentTimeMillis() - prefsFileLastSave) >= JOURNAL_MAX_AGE;
  }

  /** Saves the current prefs to file. Callers must already hold Preferences.prefsLock. */
  void savePrefsFile(boolean loggingOut) {
    tempFile.getParentFile().mkdirs(); // Guarantee directory presence before using FileOutputStream

    try (FileOutputStream fos = new FileOutputStream(tempFile);
        OutputStream fstream = new BufferedOutputStream(fos)) {

      for (Entry<String, byte[]> current : encodedData.entrySet()) {
        fstream.write(current.getValue());
      }

      // Force hardware sync before rename: Flush Java buffer, then force OS flush
      fstream.flush();
      fos.getFD().sync();
    } catch (IOException e) {
      System.out.println(e.getMessage() + " trying to write preferences as byte array.");
      // We early exit as saving failed
      return;
    }

    if (!writeLooksValid(tempFile)) {
      // Bad write - leave the journal alone, it's the only record of the unsaved changes.
      System.out.println(tempFile + " failed validation after saving, backup left as-is.");
      return;
    }

    // Rotate existing valid prefs to backup
    if (propertiesFile.exists()) {
      try {
        if (propertiesFile.length() > 0 && !FileUtilities.containsNullBytes(propertiesFile)) {
          safeMove(propertiesFile, backupFile);
        }
      } catch (IOException e) {
        System.out.println(e.getMessage() + " trying to rotate old preferences to backup.");
      }
    }

    try {
      safeMove(tempFile, propertiesFile);
    } catch (IOException e) {
      System.out.println(e.getMessage() + " trying to atomically move preferences file.");
      return; // Early exit since main file failed to save
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

  /**
   * Attempts to atomically move a file, falling back to a non-atomic move if unsupported by the
   * OS/filesystem.
   */
  private static void safeMove(File source, File dest) throws IOException {
    try {
      Files.move(
          source.toPath(),
          dest.toPath(),
          StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
