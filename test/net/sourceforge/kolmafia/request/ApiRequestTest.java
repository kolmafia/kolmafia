package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Networking.json;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withMeatInCloset;
import static internal.helpers.Player.withMeatInStorage;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withPullsRemaining;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.fastjson2.JSONObject;
import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ApiRequest.What;
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

  @Test
  void ascendingKeyPutsUsInValhalla() {
    var cleanups = new Cleanups(() -> CharPaneRequest.setInValhalla(false));

    try (cleanups) {
      ApiRequest.parseStatus(json(html("request/test_api_status_valhalla.json")));

      assertTrue(CharPaneRequest.inValhalla());
      // Set at the very end of parseStatus, so we know we parsed the whole thing
      assertThat(KoLCharacter.getRollover(), equalTo(1789875002L));
    }
  }

  @Test
  void absentAscendingKeyTakesUsOutOfValhalla() {
    var cleanups = new Cleanups(() -> CharPaneRequest.setInValhalla(false));

    try (cleanups) {
      CharPaneRequest.setInValhalla(true);
      ApiRequest.parseStatus(json(html("request/test_crimbo_ghost_api.json")));

      assertFalse(CharPaneRequest.inValhalla());
    }
  }

  @Test
  void parsesEachWhatWhenSeveralAreRequested() {
    ApiRequest.parseResponse(
        "api.php?what=inventory,closet&for=KoLmafia",
        """
        {"inventory":{"1":"3"},"closet":{"2":"5"}}
        """);

    assertThat(KoLConstants.inventory, contains(ItemPool.get(ItemPool.SEAL_CLUB, 3)));
    assertThat(KoLConstants.closet, contains(ItemPool.get(ItemPool.SEAL_TOOTH, 5)));
  }

  @Test
  void parsesRootObjectWhenOneWhatIsRequested() {
    ApiRequest.parseResponse("api.php?what=inventory&for=KoLmafia", "{\"1\":\"3\"}");

    assertThat(KoLConstants.inventory, contains(ItemPool.get(ItemPool.SEAL_CLUB, 3)));
  }

  @Test
  void refreshesSeveralThingsInOneRequest() {
    var builder = new FakeHttpClientBuilder();

    try (var cleanups = new Cleanups(withHttpClientBuilder(builder))) {
      ApiRequest.refresh(What.INVENTORY, What.CLOSET);

      var requests = builder.client.getRequests();
      assertThat(requests, hasSize(1));
      assertPostRequest(requests.get(0), "/api.php", "what=inventory,closet&for=KoLmafia");
    }
  }

  @Test
  void parsesClosetAndStorageFromStatus() {
    try (var cleanups =
        new Cleanups(withMeatInCloset(0), withMeatInStorage(0), withPullsRemaining(0))) {
      ApiRequest.parseStatus(json(html("request/test_status2.json")));

      assertThat(KoLCharacter.getClosetMeat(), is(54321L));
      assertThat(KoLCharacter.getStorageMeat(), is(12345L));
      assertThat(ConcoctionDatabase.getPullsRemaining(), is(1));
    }
  }
}
