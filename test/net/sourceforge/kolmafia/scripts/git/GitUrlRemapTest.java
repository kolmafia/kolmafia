package net.sourceforge.kolmafia.scripts.git;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class GitUrlRemapTest {

  private void withRemaps(String value, Runnable fn) {
    Preferences.setString("gitUrlRemaps", value);
    GitUrlRemap.reload();
    try {
      fn.run();
    } finally {
      Preferences.setString("gitUrlRemaps", "");
      GitUrlRemap.reload();
    }
  }

  @Nested
  class BasicRemap {
    @Test
    void shouldReturnNullForNullInput() {
      assertEquals(null, GitUrlRemap.remapUrl(null));
    }

    @Test
    void shouldReturnBlankForBlankInput() {
      assertEquals("", GitUrlRemap.remapUrl(""));
    }

    @Test
    void shouldReturnUnchangedWhenNoRemaps() {
      assertEquals(
          "https://github.com/org/repo.git",
          GitUrlRemap.remapUrl("https://github.com/org/repo.git"));
    }

    @Test
    void shouldApplyExactPrefixMatch() {
      withRemaps(
          "https://github.com/org|https://github.com/fork",
          () -> {
            assertEquals(
                "https://github.com/fork/repo.git",
                GitUrlRemap.remapUrl("https://github.com/org/repo.git"));
          });
    }

    @Test
    void shouldApplyFullUrlReplacement() {
      withRemaps(
          "https://github.com/original/repo.git|https://github.com/fork/alt-repo.git",
          () -> {
            assertEquals(
                "https://github.com/fork/alt-repo.git",
                GitUrlRemap.remapUrl("https://github.com/original/repo.git"));
          });
    }

    @Test
    void shouldNotMatchPartialPrefix() {
      withRemaps(
          "https://github.com/org|https://github.com/fork",
          () -> {
            // Should NOT match because org is followed by '2', not '/'
            assertEquals(
                "https://github.com/org2/repo.git",
                GitUrlRemap.remapUrl("https://github.com/org2/repo.git"));
          });
    }

    @Test
    void shouldMatchExactUrlWithNoSuffix() {
      withRemaps(
          "https://github.com/org|https://github.com/fork",
          () -> {
            // Should match when the URL is exactly the prefix (edge case)
            assertEquals("https://github.com/fork", GitUrlRemap.remapUrl("https://github.com/org"));
          });
    }
  }

  @Nested
  class MultipleRemaps {
    @Test
    void shouldApplyFirstMatchingRemap() {
      withRemaps(
          "https://github.com/org1|https://github.com/fork1;https://github.com/org2|https://github.com/fork2",
          () -> {
            assertEquals(
                "https://github.com/fork1/repo.git",
                GitUrlRemap.remapUrl("https://github.com/org1/repo.git"));
            assertEquals(
                "https://github.com/fork2/other.git",
                GitUrlRemap.remapUrl("https://github.com/org2/other.git"));
          });
    }

    @Test
    void shouldUseInsertionOrder() {
      withRemaps(
          "https://github.com/org/repo.git|https://github.com/fork/alt.git;https://github.com/org|https://github.com/fork",
          () -> {
            // First-defined remap takes precedence
            assertEquals(
                "https://github.com/fork/alt.git",
                GitUrlRemap.remapUrl("https://github.com/org/repo.git"));
            // Generic remap still works for other repos
            assertEquals(
                "https://github.com/fork/other.git",
                GitUrlRemap.remapUrl("https://github.com/org/other.git"));
          });
    }
  }

  @Nested
  class EdgeCases {
    @Test
    void shouldIgnoreMalformedRemap() {
      withRemaps(
          "no-pipe-here;https://github.com/org|https://github.com/fork",
          () -> {
            // Only the valid remap should be applied
            assertEquals(
                "https://github.com/fork/repo.git",
                GitUrlRemap.remapUrl("https://github.com/org/repo.git"));
          });
    }

    @Test
    void shouldIgnoreEmptyRemap() {
      withRemaps(
          ";;https://github.com/org|https://github.com/fork;;",
          () -> {
            assertEquals(
                "https://github.com/fork/repo.git",
                GitUrlRemap.remapUrl("https://github.com/org/repo.git"));
          });
    }

    @Test
    void shouldHandleSpaces() {
      withRemaps(
          "  https://github.com/org | https://github.com/fork  ",
          () -> {
            assertEquals(
                "https://github.com/fork/repo.git",
                GitUrlRemap.remapUrl("https://github.com/org/repo.git"));
          });
    }
  }

  @Nested
  class CaseInsensitivity {
    @Test
    void shouldMatchCaseInsensitive() {
      withRemaps(
          "https://github.com/ORG|https://github.com/fork",
          () -> {
            assertEquals(
                "https://github.com/fork/repo.git",
                GitUrlRemap.remapUrl("https://github.com/org/repo.git"));
          });
    }

    @Test
    void shouldPreserveOriginalCase() {
      withRemaps(
          "https://github.com/org|https://github.com/fork",
          () -> {
            assertEquals(
                "https://github.com/fork/Repo.git",
                GitUrlRemap.remapUrl("https://github.com/org/Repo.git"));
          });
    }
  }
}
