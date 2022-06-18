package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.setupFakeResponse;
import static internal.helpers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClanLoungeRequestTest {
  @BeforeEach
  public void init() {
    Preferences.saveSettingsToFile = false;
    KoLCharacter.reset("ClanLoungeRequestTest");
    Preferences.reset("ClanLoungeRequestTest");
    // don't try to visit the fireworks shop
    Preferences.setBoolean("_fireworksShop", true);
  }

  static String html(String path) {
    try {
      return Files.readString(Paths.get(path)).trim();
    } catch (IOException e) {
      Assertions.fail("Failed to load HTML file: " + path);
    }

    return null;
  }

  @Test
  void floundryRequestParsesLocations() {
    assertThat("_floundryCarpLocation", isSetTo(""));

    ClanLoungeRequest req = new ClanLoungeRequest(ClanLoungeRequest.FLOUNDRY);
    var cleanups = setupFakeResponse(200, html("request/test_clan_floundry.html"));
    try (cleanups) {
      req.run();
    }

    assertThat("_floundryCarpLocation", isSetTo("Pirates of the Garbage Barges"));
    assertThat("_floundryCodLocation", isSetTo("Thugnderdome"));
    assertThat("_floundryTroutLocation", isSetTo("The Haunted Conservatory"));
    assertThat("_floundryBassLocation", isSetTo("Guano Junction"));
    assertThat("_floundryHatchetfishLocation", isSetTo("The Skeleton Store"));
    assertThat("_floundryTunaLocation", isSetTo("The Oasis"));
  }
}
