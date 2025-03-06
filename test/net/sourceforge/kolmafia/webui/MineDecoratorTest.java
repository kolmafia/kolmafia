package net.sourceforge.kolmafia.webui;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MineDecoratorTest {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("MineDecoratorTest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("MineDecoratorTest");
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "volcano_got_gold|mining.php?mine=6&which=50&pwd|#50<img src=\"https://d2uyhvukfffg5a.cloudfront.net/itemimages/goldnugget.gif\" alt=\"1,970 carat gold\" title=\"1,970 carat gold\" class=hand onClick='descitem(372371940)' >",
        "volcano_deeply_explored|mining.php?mine=6&which=9&pwd|#9<img src=\"https://d2uyhvukfffg5a.cloudfront.net/itemimages/hp.gif\" height=30 width=30>",
        "volcano_stats|mining.php?mine=6&which=51&pwd|''",
      },
      delimiter = '|')
  void canParseMineResult(final String file, final String url, final String expected) {
    var cleanups = new Cleanups(withProperty("mineLayout6", ""), withProperty("mineState6", ""));

    try (cleanups) {
      MineDecorator.parseResponse(url, html("request/test_mining_" + file + ".html"));
      assertThat("mineLayout6", isSetTo(expected));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "volcano_mixed_results,XXXXXXXXXXXXXXXXXXXX**XXXXoo*XXXXoXX",
    "volcano_deeply_explored,ooo*XX*oo**X*oo*o*oooooXooooooXooooX",
  })
  void canParseMineState(final String file, final String expected) {
    var cleanups = new Cleanups(withProperty("mineLayout6", ""), withProperty("mineState6", ""));

    try (cleanups) {
      MineDecorator.parseResponse(
          "mining.php?mine=6", html("request/test_mining_" + file + ".html"));
      assertThat("mineState6", isSetTo(expected));
    }
  }
}
