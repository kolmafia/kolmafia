package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacter.Gender;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class RumpleManagerTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("rumplestiltskin");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("rumplestiltskin");
  }

  @Nested
  class Workshop {

    public Cleanups withEmptyWorkshop() {
      // Set up this test to have all quests appropriately started
      return new Cleanups(
          withProperty("craftingClay"),
          withProperty("craftingGlass"),
          withProperty("craftingLeather"),
          withItem(ItemPool.STRAW, 0),
          withItem(ItemPool.LEATHER, 0),
          withItem(ItemPool.CLAY, 0),
          withItem(ItemPool.FILLING, 0),
          withItem(ItemPool.PARCHMENT, 0),
          withItem(ItemPool.GLASS, 0));
    }

    @Test
    public void canDetectMaterialsFromVisitingWorkshop() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withEmptyWorkshop(),
              withPasswordHash("gnome"),
              // If you have a password hash, KoL looks at your vinyl boots
              withGender(Gender.FEMALE),
              withHandlingChoice(845));
      try (cleanups) {
        var html = html("request/test_visit_workshop_1.html");
        client.addResponse(200, html);

        var URL = "choice.php?pwd=gnome&whichchoice=845&option=1";
        var request = new GenericRequest(URL);
        request.run();

        // We are still in a choice
        assertTrue(ChoiceManager.handlingChoice);

        // We have detected materials
        assertEquals(2, InventoryManager.getCount(ItemPool.STRAW));
        assertEquals(6, InventoryManager.getCount(ItemPool.LEATHER));
        assertEquals(4, InventoryManager.getCount(ItemPool.CLAY));
        assertEquals(0, InventoryManager.getCount(ItemPool.FILLING));
        assertEquals(0, InventoryManager.getCount(ItemPool.PARCHMENT));
        assertEquals(0, InventoryManager.getCount(ItemPool.GLASS));

        var requests = client.getRequests();
        assertThat(requests, hasSize(1));

        assertPostRequest(requests.get(0), "/choice.php", "whichchoice=845&option=1&pwd=gnome");
      }
    }

    @Test
    public void canDetectMasteryFromVisitingWorkshop() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withEmptyWorkshop(),
              withPasswordHash("gnome"),
              // If you have a password hash, KoL looks at your vinyl boots
              withGender(Gender.FEMALE),
              withHandlingChoice(845));
      try (cleanups) {
        var html = html("request/test_visit_workshop_2.html");
        client.addResponse(200, html);
        client.addResponse(200, ""); // api.php

        var URL = "choice.php?pwd=gnome&whichchoice=845&option=1";
        var request = new GenericRequest(URL);
        request.run();

        // We are still in a choice
        assertTrue(ChoiceManager.handlingChoice);

        // We have detected mastery of leather crafting
        assertEquals(Preferences.getInteger("craftingClay"), -1);
        assertEquals(Preferences.getInteger("craftingLeather"), 0);
        assertEquals(Preferences.getInteger("craftingStraw"), -1);

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(requests.get(0), "/choice.php", "whichchoice=845&option=1&pwd=gnome");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Nested
    class CraftingMastery {

      @ParameterizedTest
      @CsvSource({"2, 4", "1, 6", "0, 3"})
      public void canLearnMasteryFromPractice(int triesLeft, int initialLeather) {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withEmptyWorkshop(),
                withItem(ItemPool.LEATHER, initialLeather),
                withPasswordHash("gnome"),
                // If you have a password hash, KoL looks at your vinyl boots
                withGender(Gender.FEMALE),
                withHandlingChoice(849));
        try (cleanups) {
          var html = html("request/test_practice_crafting_" + triesLeft + ".html");
          client.addResponse(200, html);
          client.addResponse(200, ""); // api.php

          // We have unknown mastery
          assertEquals(Preferences.getInteger("craftingLeather"), -1);

          var URL = "choice.php?pwd=gnome&whichchoice=849&option=2";
          var request = new GenericRequest(URL);
          request.run();

          // We are still in a choice
          assertTrue(ChoiceManager.handlingChoice);

          // We have triesLeft more attempts to achieve mastery
          assertEquals(Preferences.getInteger("craftingLeather"), triesLeft);

          // Practicing consumes 3 leather
          int expectedLeather = initialLeather - 3;
          assertEquals(expectedLeather, InventoryManager.getCount(ItemPool.LEATHER));

          // Gaining mastery gives us 1 parchment
          int expectedParchment = triesLeft == 0 ? 1 : 0;
          assertEquals(expectedParchment, InventoryManager.getCount(ItemPool.PARCHMENT));

          var requests = client.getRequests();
          assertThat(requests, hasSize(2));

          assertPostRequest(requests.get(0), "/choice.php", "whichchoice=849&option=2&pwd=gnome");
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }
    }
  }
}
