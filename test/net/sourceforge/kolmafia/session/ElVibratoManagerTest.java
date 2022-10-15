package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withCampgroundItem;
import static internal.helpers.Player.withEmptyCampground;
import static internal.helpers.Player.withFight;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLastLocation;
import static internal.helpers.Player.withNextMonster;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSkill;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ElVibratoManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("ElVibratoManager");
    Preferences.reset("ElVibratoManager");
  }

  @Nested
  class ShimmeringPortalEnergy {
    @Test
    public void canUseTrapezoid() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.TRAPEZOID),
              withEmptyCampground(),
              withProperty("currentPortalEnergy", 0));
      try (cleanups) {
        builder.client.addResponse(200, html("request/test_use_el_vibrato_trapezoid.html"));
        builder.client.addResponse(200, ""); // api.php

        String urlString = "inv_use.php?whichitem=3198&ajax=1";
        var request = new GenericRequest(urlString);
        request.run();

        int index = KoLConstants.campground.indexOf(ItemPool.get(ItemPool.TRAPEZOID));
        assertNotEquals(-1, index);
        AdventureResult portal = KoLConstants.campground.get(index);
        assertNotNull(portal);
        assertEquals(20, Preferences.getInteger("currentPortalEnergy"));
        assertEquals(20, portal.getCount());

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=3198&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @ParameterizedTest
    @CsvSource({
      "visit_el_vibrato_campground, 0, 20",
      "visit_el_vibrato_campground, 10, 10",
      "visit_deactivated_el_vibrato_campground, 10, 0"
    })
    public void canRecognizeActivePortalAndEnergy(
        final String fixture, final int startingEnergy, final int estimatedEnergy) {
      String html = html("request/test_" + fixture + ".html");
      var cleanups =
          new Cleanups(withEmptyCampground(), withProperty("currentPortalEnergy", startingEnergy));
      try (cleanups) {
        CampgroundRequest.parseResponse("campground.php", html);
        int index = KoLConstants.campground.indexOf(ItemPool.get(ItemPool.TRAPEZOID));
        assertNotEquals(-1, index);
        AdventureResult portal = KoLConstants.campground.get(index);
        assertNotNull(portal);
        assertEquals(estimatedEnergy, Preferences.getInteger("currentPortalEnergy"));
        assertEquals(estimatedEnergy, portal.getCount());
      }
    }

    @Test
    public void canReopenWithPowerSphere() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.POWER_SPHERE),
              withCampgroundItem(ItemPool.TRAPEZOID, 0),
              withProperty("currentPortalEnergy", 0));
      try (cleanups) {
        builder.client.addResponse(200, html("request/test_use_el_vibrato_power_sphere.html"));
        builder.client.addResponse(200, ""); // api.php

        String urlString = "campground.php?action=powerelvibratoportal";
        var request = new GenericRequest(urlString);
        request.run();

        // We used up the El Vibrato power sphere
        assertEquals(0, InventoryManager.getCount(ItemPool.POWER_SPHERE));

        int index = KoLConstants.campground.indexOf(ItemPool.get(ItemPool.TRAPEZOID));
        assertNotEquals(-1, index);
        AdventureResult portal = KoLConstants.campground.get(index);
        assertNotNull(portal);
        assertEquals(5, Preferences.getInteger("currentPortalEnergy"));
        assertEquals(5, portal.getCount());

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/campground.php", "action=powerelvibratoportal");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canAddEnergyWithPowerSphere() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.POWER_SPHERE),
              withCampgroundItem(ItemPool.TRAPEZOID, 0),
              withProperty("currentPortalEnergy", 10));
      try (cleanups) {
        builder.client.addResponse(200, html("request/test_use_el_vibrato_power_sphere_2.html"));
        builder.client.addResponse(200, ""); // api.php

        String urlString = "campground.php?action=powerelvibratoportal";
        var request = new GenericRequest(urlString);
        request.run();

        // We used up the El Vibrato power sphere
        assertEquals(0, InventoryManager.getCount(ItemPool.POWER_SPHERE));

        int index = KoLConstants.campground.indexOf(ItemPool.get(ItemPool.TRAPEZOID));
        assertNotEquals(-1, index);
        AdventureResult portal = KoLConstants.campground.get(index);
        assertNotNull(portal);
        assertEquals(15, Preferences.getInteger("currentPortalEnergy"));
        assertEquals(15, portal.getCount());

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/campground.php", "action=powerelvibratoportal");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canReopenWithOverchargedPowerSphere() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.OVERCHARGED_POWER_SPHERE),
              withCampgroundItem(ItemPool.TRAPEZOID, 0),
              withProperty("currentPortalEnergy", 0));
      try (cleanups) {
        builder.client.addResponse(
            200, html("request/test_use_el_vibrato_overcharged_power_sphere.html"));
        builder.client.addResponse(200, ""); // api.php

        String urlString = "campground.php?action=overpowerelvibratoportal";
        var request = new GenericRequest(urlString);
        request.run();

        // We used up the overcharged El Vibrato power sphere
        assertEquals(0, InventoryManager.getCount(ItemPool.OVERCHARGED_POWER_SPHERE));

        int index = KoLConstants.campground.indexOf(ItemPool.get(ItemPool.TRAPEZOID));
        assertNotEquals(-1, index);
        AdventureResult portal = KoLConstants.campground.get(index);
        assertNotNull(portal);
        assertEquals(10, Preferences.getInteger("currentPortalEnergy"));
        assertEquals(10, portal.getCount());

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/campground.php", "action=overpowerelvibratoportal");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canAddEnergyWithOverchargedPowerSphere() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.OVERCHARGED_POWER_SPHERE),
              withCampgroundItem(ItemPool.TRAPEZOID, 0),
              withProperty("currentPortalEnergy", 13));
      try (cleanups) {
        builder.client.addResponse(
            200, html("request/test_use_el_vibrato_overcharged_power_sphere_2.html"));
        builder.client.addResponse(200, ""); // api.php

        String urlString = "campground.php?action=overpowerelvibratoportal";
        var request = new GenericRequest(urlString);
        request.run();

        // We used up the overcharged El Vibrato power sphere
        assertEquals(0, InventoryManager.getCount(ItemPool.OVERCHARGED_POWER_SPHERE));

        int index = KoLConstants.campground.indexOf(ItemPool.get(ItemPool.TRAPEZOID));
        assertNotEquals(-1, index);
        AdventureResult portal = KoLConstants.campground.get(index);
        assertNotNull(portal);
        assertEquals(23, Preferences.getInteger("currentPortalEnergy"));
        assertEquals(23, portal.getCount());

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/campground.php", "action=overpowerelvibratoportal");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canDecrementEnergyWithFight() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              // This will clean up FightRequest
              withFight(0),
              withCampgroundItem(ItemPool.TRAPEZOID, 20),
              withProperty("currentPortalEnergy", 20));
      try (cleanups) {
        // adventure.php?snarfblat=164
        builder.client.addResponse(
            302, Map.of("location", List.of("fight.php?ireallymeanit=1663603584")), "");
        // fight.php?ireallymeanit=1663603584
        builder.client.addResponse(200, html("request/test_el_vibrato_fight_1.html"));
        // api.php?what=status&for=KoLmafia
        builder.client.addResponse(200, ""); // api.php
        // fight.php?action=attack
        builder.client.addResponse(200, html("request/test_el_vibrato_fight_2.html"));
        // api.php?what=status&for=KoLmafia
        builder.client.addResponse(200, ""); // api.php

        var adventure = new AdventureRequest("El Vibrato Island", AdventurePool.EL_VIBRATO_ISLAND);
        adventure.run();

        assertEquals(19, Preferences.getInteger("currentPortalEnergy"));

        int index = KoLConstants.campground.indexOf(ItemPool.get(ItemPool.TRAPEZOID));
        assertNotEquals(-1, index);
        AdventureResult portal = KoLConstants.campground.get(index);
        assertNotNull(portal);
        assertEquals(19, portal.getCount());
      }
    }
  }

  @Nested
  class ElVibratoMachine {
    @Test
    public void canTrackMachinationsEncounter() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withCampgroundItem(ItemPool.TRAPEZOID, 20),
              withItem(ItemPool.PUNCHCARD_TARGET),
              withProperty("currentPortalEnergy", 20));
      try (cleanups) {
        // adventure.php?snarfblat=164
        builder.client.addResponse(
            302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        // choice.php?forceoption=0
        builder.client.addResponse(302, Map.of("location", List.of("elvmachine.php")), "");
        // elvmachine.php
        builder.client.addResponse(200, html("request/test_el_vibrato_machine.html"));
        // api.php?what=status&for=KoLmafia
        builder.client.addResponse(200, ""); // api.php
        // elvmachine.php?action=slot&whichcard=3151
        builder.client.addResponse(200, html("request/test_el_vibrato_machine_slot.html"));
        // elvmachine.php?action=button
        builder.client.addResponse(200, html("request/test_el_vibrato_machine_button.html"));

        var adventure = new AdventureRequest("El Vibrato Island", AdventurePool.EL_VIBRATO_ISLAND);
        adventure.run();

        // adventure.php -> choice.php -> evmachine.php
        assertFalse(ChoiceManager.handlingChoice);

        assertEquals(19, Preferences.getInteger("currentPortalEnergy"));

        int index = KoLConstants.campground.indexOf(ItemPool.get(ItemPool.TRAPEZOID));
        assertNotEquals(-1, index);
        AdventureResult portal = KoLConstants.campground.get(index);
        assertNotNull(portal);
        assertEquals(19, portal.getCount());

        // We are now in Machinations
        var slot = new GenericRequest("elvmachine.php?action=slot&whichcard=3151");
        slot.run();

        // We traded TARGET for ATTACK
        assertEquals(0, InventoryManager.getCount(ItemPool.PUNCHCARD_TARGET));
        assertEquals(1, InventoryManager.getCount(ItemPool.PUNCHCARD_ATTACK));

        var button = new GenericRequest("elvmachine.php?action=button");
        button.run();

        // We exited the machine

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(6));
        assertPostRequest(requests.get(0), "/adventure.php", "snarfblat=164");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertGetRequest(requests.get(2), "/elvmachine.php");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(4), "/elvmachine.php", "action=slot&whichcard=3151");
        assertPostRequest(requests.get(5), "/elvmachine.php", "action=button");
      }
    }
  }

  @Nested
  class CombatHelper {
    // Verbs
    private static final int ATTACK = ItemPool.PUNCHCARD_ATTACK;
    private static final int REPAIR = ItemPool.PUNCHCARD_REPAIR;
    private static final int BUFF = ItemPool.PUNCHCARD_BUFF;
    private static final int MODIFY = ItemPool.PUNCHCARD_MODIFY;
    private static final int BUILD = ItemPool.PUNCHCARD_BUILD;
    // Objects
    private static final int TARGET = ItemPool.PUNCHCARD_TARGET;
    private static final int SELF = ItemPool.PUNCHCARD_SELF;
    private static final int FLOOR = ItemPool.PUNCHCARD_FLOOR;
    private static final int DRONE = ItemPool.PUNCHCARD_DRONE;
    private static final int WALL = ItemPool.PUNCHCARD_WALL;
    private static final int SPHERE = ItemPool.PUNCHCARD_SPHERE;

    private static final String html = "<tr><td><center><table></table></center></td></tr>";

    @Test
    public void mustBeOnElVibratoIsland() {
      var cleanups =
          new Cleanups(
              withLastLocation("Haunted Pantry"),
              withFight(1),
              withNextMonster("lonely construct"));
      try (cleanups) {
        String input = html;
        StringBuffer page = new StringBuffer(input);
        ElVibratoManager.decorate(page);
        String result = page.toString();
        // Undecorated
        assertEquals(input, result);
      }
    }

    @Test
    public void mustBeInFight() {
      var cleanups =
          new Cleanups(
              withLastLocation("El Vibrato Island"),
              withFight(0),
              withNextMonster("lonely construct"));
      try (cleanups) {
        String input = html;
        StringBuffer page = new StringBuffer(input);
        ElVibratoManager.decorate(page);
        String result = page.toString();
        // Undecorated
        assertEquals(input, result);
      }
    }

    @Test
    public void mustBeFightingConstruct() {
      var cleanups =
          new Cleanups(
              withLastLocation("El Vibrato Island"),
              withFight(1),
              withNextMonster("migratory pirate"));
      try (cleanups) {
        String input = html;
        StringBuffer page = new StringBuffer(input);
        ElVibratoManager.decorate(page);
        String result = page.toString();
        // Undecorated
        assertEquals(input, result);
      }
    }

    @Test
    public void mustHaveNormalPage() {
      var cleanups =
          new Cleanups(
              withLastLocation("El Vibrato Island"),
              withFight(1),
              withNextMonster("lonely construct"));
      try (cleanups) {
        String input = "hello";
        StringBuffer page = new StringBuffer(input);
        ElVibratoManager.decorate(page);
        String result = page.toString();
        // Undecorated
        assertEquals(input, result);
      }
    }

    private static void checkButton(
        String result, int card1, int card2, boolean disabled, boolean funkslinging) {
      // disabled funkslinging
      //
      // <form method=POST action="fight.php"><td>
      //    <input type=hidden name="action" value="macro">
      //    <input type=hidden name="macrotext" value="use 3146,3153">
      //    <input onclick="return killforms(this);" type="submit"
      //           value="COMMAND!" disabled>
      //    &nbsp;</td>
      // </form>

      // enabled no funkslinging
      //
      // <form method=POST action="fight.php"><td>
      //     <input type=hidden name="action" value="macro">
      //     <input type=hidden name="macrotext" value="use 3148; use 3153">
      //     <input onclick="return killforms(this);" type="submit"
      //            value="COMMAND!">
      //     &nbsp;</td>
      // </form>

      StringBuilder macro = new StringBuilder();
      macro.append("<input type=hidden name=\"macrotext\" value=\"");
      macro.append("use ");
      macro.append(card1);
      macro.append(funkslinging ? "," : "; use ");
      macro.append(card2);
      macro.append("\">");

      assertTrue(result.contains(macro.toString()));

      StringBuilder isEnabled = new StringBuilder();
      isEnabled.append("value=\"COMMAND!\"");
      if (disabled) {
        isEnabled.append(" disabled");
      }
      isEnabled.append(">");

      assertTrue(result.contains(isEnabled.toString()));
    }

    // We'll work with the lonely construct since it can manipulate two
    // kinds of object
    //
    // Command(MODIFY, SPHERE, POWER_SPHERE, "-> overcharged power sphere");
    // Command(REPAIR, DRONE, BROKEN_DRONE, "-> repaired drone");
    // Command(REPAIR, SELF, null, "manipulates construct");
    // Command(REPAIR, TARGET, null, "damages you");

    @ParameterizedTest
    @CsvSource({
      "lonely construct, true",
      "lonely construct, false",
      "lonely construct (translated), true",
      "lonely construct (translated), false",
    })
    public void canMakeNecessaryButtons(final String monster, final boolean funkslinging) {
      var cleanups =
          new Cleanups(
              withLastLocation("El Vibrato Island"),
              withFight(1),
              withNextMonster(monster),
              withItem(MODIFY),
              withItem(SPHERE),
              withItem(REPAIR),
              withItem(DRONE),
              withItem(SELF),
              withItem(ItemPool.POWER_SPHERE),
              withSkill(funkslinging ? "Ambidextrous Funkslinging" : "Sing"));
      try (cleanups) {
        String input = html;
        StringBuffer page = new StringBuffer(input);
        ElVibratoManager.decorate(page);
        String result = page.toString();
        // Decorated
        assertNotEquals(input, result);
        checkButton(result, MODIFY, SPHERE, false, funkslinging);
        checkButton(result, REPAIR, DRONE, true, funkslinging);
        checkButton(result, REPAIR, SELF, false, funkslinging);
        checkButton(result, REPAIR, TARGET, true, funkslinging);
      }
    }
  }
}
