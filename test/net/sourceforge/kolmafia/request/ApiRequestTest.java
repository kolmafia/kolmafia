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
            withProperty("zootGraftedHeadFamiliar", 25),
            withProperty("zootGraftedShoulderLeftFamiliar", 25),
            withProperty("zootGraftedShoulderRightFamiliar", 25),
            withProperty("zootGraftedHandLeftFamiliar", 25),
            withProperty("zootGraftedHandRightFamiliar", 25),
            withProperty("zootGraftedNippleRightFamiliar", 25),
            withProperty("zootGraftedNippleLeftFamiliar", 25),
            withProperty("zootGraftedButtCheekLeftFamiliar", 25),
            withProperty("zootGraftedButtCheekRightFamiliar", 25),
            withProperty("zootGraftedFootLeftFamiliar", 25),
            withProperty("zootGraftedFootRightFamiliar", 25));

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

      assertThat("zootGraftedHeadFamiliar", isSetTo(175));
      assertThat("zootGraftedShoulderLeftFamiliar", isSetTo(16));
      assertThat("zootGraftedShoulderRightFamiliar", isSetTo(55));
      assertThat("zootGraftedHandLeftFamiliar", isSetTo(0));
      assertThat("zootGraftedHandRightFamiliar", isSetTo(0));
      assertThat("zootGraftedNippleRightFamiliar", isSetTo(20));
      assertThat("zootGraftedNippleLeftFamiliar", isSetTo(71));
      assertThat("zootGraftedButtCheekLeftFamiliar", isSetTo(3));
      assertThat("zootGraftedButtCheekRightFamiliar", isSetTo(142));
      assertThat("zootGraftedFootLeftFamiliar", isSetTo(286));
      assertThat("zootGraftedFootRightFamiliar", isSetTo(0));
    }
  }
}
