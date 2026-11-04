package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withDataFile;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ScriptManagerTest {

  @Test
  void canReadSvnRepoJson() {
    // This should read the existing test/root/data/svnrepo.json, which contains two entries.
    try (var cleanups =
        new Cleanups(withProperty("_svnRepoFileFetched", "true"), withDataFile("svnrepo.json"))) {
      ScriptManager.updateRepoScripts(false);
    }

    assertThat(ScriptManager.getInstalledScripts(), empty());

    List<Script> repoScripts = ScriptManager.getRepoScripts();
    assertThat(repoScripts, hasSize(2));
    assertEquals("acquireBuff", repoScripts.getFirst().getScriptName());
  }

  @Nested
  class LocalScripts {
    @Test
    void addsPlainFiles(@TempDir Path dir) throws IOException {
      Path script = Files.writeString(dir.resolve("script.ash"), "print(1);");

      assertThat(ScriptManager.shouldAddScript(script.toFile()), is(true));
    }

    @Test
    void skipsEmptyDirectories(@TempDir Path dir) throws IOException {
      Path empty = Files.createDirectories(dir.resolve("empty").resolve("nested"));

      assertThat(ScriptManager.shouldAddScript(empty.toFile()), is(false));
      assertThat(ScriptManager.shouldAddScript(dir.resolve("empty").toFile()), is(false));
    }

    @Test
    void addsDirectoriesThatContainScripts(@TempDir Path dir) throws IOException {
      Path nested = Files.createDirectories(dir.resolve("outer").resolve("inner"));
      Files.writeString(nested.resolve("script.ash"), "print(1);");

      assertThat(ScriptManager.shouldAddScript(dir.resolve("outer").toFile()), is(true));
    }

    @Test
    void listsDirectoriesBeforeFiles() {
      ScriptManager.compileScripts();

      List<File> scripts = new ArrayList<>(KoLConstants.scripts);
      assertThat(scripts, not(empty()));

      int firstFile = 0;
      while (firstFile < scripts.size() && scripts.get(firstFile).isDirectory()) {
        firstFile++;
      }
      for (File script : scripts.subList(firstFile, scripts.size())) {
        assertThat(script.getName(), script.isDirectory(), is(false));
      }
      for (File script : scripts) {
        assertThat(script.getName(), ScriptManager.shouldAddScript(script), is(true));
      }
    }
  }
}
