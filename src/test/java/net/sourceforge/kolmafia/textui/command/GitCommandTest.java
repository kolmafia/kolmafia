package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withContinuationState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mockStatic;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.scripts.git.GitManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class GitCommandTest extends AbstractCommandTestBase {
  public GitCommandTest() {
    this.command = "git";
  }

  static MockedStatic<GitManager> mocked;

  @BeforeAll
  static void beforeAll() {
    mocked = mockStatic(GitManager.class, Mockito.RETURNS_DEFAULTS);
  }

  @BeforeEach
  void beforeEach() {
    mocked.clearInvocations();
  }

  @AfterAll
  static void afterAll() {
    mocked.close();
  }

  @Nested
  class Checkout {
    @Test
    void requiresUrlOfSomeSort() {
      var cleanups = new Cleanups(withContinuationState());

      try (cleanups) {
        String output = execute("checkout");
        assertThat(output.trim(), equalTo("git checkout requires a repo url."));
        mocked.verifyNoInteractions();
      }
    }

    @Test
    void canParseNormalUrl() {
      execute("checkout https://github.com/gh-user/gh-repo.git");
      mocked.verify(() -> GitManager.clone("https://github.com/gh-user/gh-repo.git"));
    }

    @Test
    void requiresHttpOrHttps() {
      var cleanups = new Cleanups(withContinuationState());

      try (cleanups) {
        String output = execute("checkout git://github.com/gh-user/gh-repo.git");
        assertThat(output.trim(), equalTo("git checkout works with http(s) URLs only"));
        mocked.verifyNoInteractions();
      }
    }

    @Test
    void canRecreateGithubUrl() {
      execute("checkout gh-user/gh-repo.git release");
      mocked.verify(() -> GitManager.clone("https://github.com/gh-user/gh-repo.git", "release"));
    }

    @Test
    void canRecreateGithubUrlWithGitExtension() {
      execute("checkout gh-user/gh-repo");
      mocked.verify(() -> GitManager.clone("https://github.com/gh-user/gh-repo.git"));
    }
  }
}
