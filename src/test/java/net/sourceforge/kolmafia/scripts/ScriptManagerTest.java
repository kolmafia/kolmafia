package net.sourceforge.kolmafia.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ScriptManagerTest {
  @Nested
  class ProjectIdentifier {
    @Test
    void setForSourceForge() throws URISyntaxException {
      URI uri = new URI("https://svn.code.sf.net/p/zlib/code");
      var ident = ScriptManager.getProjectIdentifier(uri.getHost(), uri.getPath());
      assertEquals("zlib", ident);
    }

    @Test
    void setForGitHub() throws URISyntaxException {
      URI uri = new URI("https://github.com/soolar/CONSUME.ash");
      var ident = ScriptManager.getProjectIdentifier(uri.getHost(), uri.getPath());
      assertEquals("soolar-CONSUME.ash", ident);
    }

    @Test
    void setForFallback() throws URISyntaxException {
      URI uri = new URI("https://git-codecommit.us-east-2.amazonaws.com/v1/repos/MyDemoRepo");
      var ident = ScriptManager.getProjectIdentifier(uri.getHost(), uri.getPath());
      assertEquals("v1-repos-MyDemoRepo", ident);
    }
  }
}
