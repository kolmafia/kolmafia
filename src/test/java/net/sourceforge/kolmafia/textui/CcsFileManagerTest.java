package net.sourceforge.kolmafia.textui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class CcsFileManagerTest {

  @Test
  public void readsExtantFile() {
    var data = CcsFileManager.getBytes("default");
    assertThat(new String(data, StandardCharsets.UTF_8), startsWith("[ default ]"));
  }

  @Test
  public void absentFileReadsAsEmpty() {
    var data = CcsFileManager.getBytes("absent");
    assertThat(data.length, equalTo(0));
  }

  @Test
  public void writesFileContent() throws IOException {
    var text = "[ default ]\nabort";
    var data = text.getBytes(StandardCharsets.UTF_8);
    var newFile = Paths.get("ccs", "new_file.ccs");
    try {
      assertTrue(CcsFileManager.printBytes("new_file", data));
      // overwrite it
      assertTrue(CcsFileManager.printBytes("new_file", data));
      assertTrue(Files.exists(newFile));
      var read = CcsFileManager.getBytes("new_file");
      assertThat(new String(read, StandardCharsets.UTF_8), equalTo(text));
    } finally {
      Files.delete(newFile);
    }
  }

  @Test
  public void disallowWritingDangerousNames() {
    assertFalse(CcsFileManager.printBytes("../danger", new byte[0]));
    assertFalse(CcsFileManager.printBytes("sub/folder", new byte[0]));
    assertFalse(CcsFileManager.printBytes("sub\\folder", new byte[0]));
  }
}
