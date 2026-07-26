package net.sourceforge.kolmafia.textui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.sourceforge.kolmafia.KoLConstants;
import org.junit.jupiter.api.Test;

public class DataFileCacheTest {
  @Test
  void printBytesOverwritesByDefault() throws Exception {
    String filename = "DataFileCacheTest_overwrite.txt";
    File file = new File(KoLConstants.DATA_LOCATION, filename);
    try {
      DataFileCache.printBytes(filename, "first".getBytes(StandardCharsets.UTF_8));
      DataFileCache.printBytes(filename, "second".getBytes(StandardCharsets.UTF_8));

      assertThat(Files.readString(file.toPath()), equalTo("second"));
    } finally {
      Files.deleteIfExists(file.toPath());
    }
  }

  @Test
  void printBytesAppendsWhenRequested() throws Exception {
    String filename = "DataFileCacheTest_append.txt";
    File file = new File(KoLConstants.DATA_LOCATION, filename);
    try {
      DataFileCache.printBytes(filename, "first".getBytes(StandardCharsets.UTF_8), false);
      DataFileCache.printBytes(filename, "second".getBytes(StandardCharsets.UTF_8), true);

      assertThat(Files.readString(file.toPath()), equalTo("firstsecond"));
    } finally {
      Files.deleteIfExists(file.toPath());
    }
  }
}
