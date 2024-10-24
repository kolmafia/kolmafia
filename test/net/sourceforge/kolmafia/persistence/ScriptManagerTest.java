package net.sourceforge.kolmafia.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ScriptManagerTest {

  @Test
  void canReadSvnRepoJson() {
    // This should read the existing test/root/data/svnrepo.json, which contains two entries.
    ScriptManager.updateRepoScripts(false);

    assertThat(ScriptManager.getInstalledScripts(), empty());

    List<Script> repoScripts = ScriptManager.getRepoScripts();
    assertThat(repoScripts, hasSize(2));
    assertEquals(repoScripts.get(0).getScriptName(), "acquireBuff");
  }
}
