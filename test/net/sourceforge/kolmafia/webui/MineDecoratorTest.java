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
import org.junit.jupiter.api.Test;

class MineDecoratorTest {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("MineDecoratorTest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("MineDecoratorTest");
  }

  @Test
  void canParseMineResult() {
    var cleanups = new Cleanups(withProperty("mineLayout6", ""), withProperty("mineState6", ""));

    try (cleanups) {
      MineDecorator.parseResponse(
          "mining.php?mine=6&which=50&pwd", html("request/test_mining_volcano_got_gold.html"));
      assertThat(
          "mineLayout6",
          isSetTo(
              "#50<img src=\"https://d2uyhvukfffg5a.cloudfront.net/itemimages/goldnugget.gif\" alt=\"1,970 carat gold\" title=\"1,970 carat gold\" class=hand onClick='descitem(372371940)' >"));
    }
  }

  @Test
  void canParseMineState() {
    var cleanups = new Cleanups(withProperty("mineLayout6", ""), withProperty("mineState6", ""));

    try (cleanups) {
      MineDecorator.parseResponse(
          "mining.php?mine=6", html("request/test_mining_volcano_mixed_results.html"));
      assertThat("mineState6", isSetTo("XXXXXXXXXXXXXXXXXXXX**XXXXoo*XXXXoXX"));
    }
  }
}
