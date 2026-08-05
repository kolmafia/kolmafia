package net.sourceforge.kolmafia.textui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.sourceforge.kolmafia.KoLConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class DataFileCacheTest {
  @AfterEach
  void tearDown() {
    DataFileCache.clearCache();
  }

  @Test
  void printBytesOverwritesByDefault() throws Exception {
    String filename = "DataFileCacheTest_overwrite.txt";
    File file = new File(KoLConstants.DATA_LOCATION, filename);
    try {
      DataFileCache.printBytes(filename, "first".getBytes(StandardCharsets.UTF_8));
      DataFileCache.printBytes(filename, "second".getBytes(StandardCharsets.UTF_8));

      assertThat(Files.readString(file.toPath()), equalTo("second"));
      assertThat(
          new String(DataFileCache.getBytes(filename), StandardCharsets.UTF_8), equalTo("second"));
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
      assertThat(
          new String(DataFileCache.getBytes(filename), StandardCharsets.UTF_8),
          equalTo("firstsecond"));
    } finally {
      Files.deleteIfExists(file.toPath());
    }
  }

  @Test
  void printBytesAppendsExistingFileWhenRequested() throws Exception {
    String filename = "DataFileCacheTest_appendWithoutCache.txt";
    File file = new File(KoLConstants.DATA_LOCATION, filename);
    try {
      // Write the data that will exist without kolmafia's knowledge
      Files.writeString(file.toPath(), "first");
      // Ensure we're not working off a known cache
      DataFileCache.clearCache();

      // Append
      DataFileCache.printBytes(filename, "second".getBytes(StandardCharsets.UTF_8), true);

      // Assert both exist
      assertThat(Files.readString(file.toPath()), equalTo("firstsecond"));
      // Assert cache knows both
      assertThat(
          new String(DataFileCache.getBytes(filename), StandardCharsets.UTF_8),
          equalTo("firstsecond"));
    } finally {
      Files.deleteIfExists(file.toPath());
    }
  }

  @Test
  void printBytesCachesUnderSameKeyAsGetBytes() throws Exception {
    String filename = "DataFileCacheTest_appendCacheKey.txt";
    File file = new File(KoLConstants.DATA_LOCATION, filename);
    try {
      DataFileCache.printBytes(filename, "first".getBytes(StandardCharsets.UTF_8));

      // Write to disk directly, but preserve the modified time.
      // So we can test if the served content is from cache or disk
      long cachedModifiedTime = file.lastModified();
      Files.writeString(file.toPath(), "notserved");
      // May as well throw the modified time into an assert
      assertThat(file.setLastModified(cachedModifiedTime), is(true));
      // Cache should see unchanged file, and serve data from cache
      assertThat(
          new String(DataFileCache.getBytes(filename), StandardCharsets.UTF_8), equalTo("first"));
    } finally {
      Files.deleteIfExists(file.toPath());
    }
  }
}
