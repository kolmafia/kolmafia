package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.alibaba.fastjson2.JSONObject;
import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiRequestTest {
  @BeforeEach
  public void setUp() {
    Preferences.reset("CharSheetRequestTest");
    KoLCharacter.reset(true);
  }

  @Test
  void parseZootomistGrafts() {
    var cleanups =
        new Cleanups(
            withPath(AscensionPath.Path.Z_IS_FOR_ZOOTOMIST),
            withProperty("zootGraftHead", 25),
            withProperty("zootGraftShoulderLeft", 25),
            withProperty("zootGraftShoulderRight", 25),
            withProperty("zootGraftHandLeft", 25),
            withProperty("zootGraftHandRight", 25),
            withProperty("zootGraftNippleRight", 25),
            withProperty("zootGraftNippleLeft", 25),
            withProperty("zootGraftButtCheekLeft", 25),
            withProperty("zootGraftButtCheekRight", 25),
            withProperty("zootGraftFootLeft", 25),
            withProperty("zootGraftFootRight", 25));

    try (cleanups) {
      var json =
          JSONObject.parseObject(
              """
        {
          "basemuscle": "70",
          "basemysticality": "70",
          "basemoxie": "70",
          "level": "13",
          "grafts":{"1":"175","2":"16","3":"55","6":"20","7":"71","8":"3","9":"142","10":"286"}
        }
      """);
      ApiRequest.parseZootomistGrafts(json);

      assertThat("zootGraftHead", isSetTo(175));
      assertThat("zootGraftShoulderLeft", isSetTo(16));
      assertThat("zootGraftShoulderRight", isSetTo(55));
      assertThat("zootGraftHandLeft", isSetTo(0));
      assertThat("zootGraftHandRight", isSetTo(0));
      assertThat("zootGraftNippleRight", isSetTo(20));
      assertThat("zootGraftNippleLeft", isSetTo(71));
      assertThat("zootGraftButtCheekLeft", isSetTo(3));
      assertThat("zootGraftButtCheekRight", isSetTo(142));
      assertThat("zootGraftFootLeft", isSetTo(286));
      assertThat("zootGraftFootRight", isSetTo(0));
    }
  }
}
