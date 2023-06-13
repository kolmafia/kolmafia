package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withFight;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withMoxie;
import static internal.helpers.Player.withMuscle;
import static internal.helpers.Player.withMysticality;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withTurnsPlayed;
import static internal.helpers.Player.withWorkshedItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ColdMedicineCabinetCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("ColdMedicineCabinetCommandTest");
    ChoiceManager.handlingChoice = false;
    FightRequest.currentRound = 0;
  }

  public ColdMedicineCabinetCommandTest() {
    this.command = "cmc";
  }

  @Test
  void doNotCheckWithNoWorkshedItem() {
    var cleanups = new Cleanups(withWorkshedItem(-1));

    try (cleanups) {
      String output = execute("");
      assertThat(output, containsString("You do not have a Cold Medicine Cabinet installed."));
    }
  }

  @Test
  void doNotCheckWithWrongWorkshedItem() {
    var cleanups = new Cleanups(withWorkshedItem(ItemPool.DIABOLIC_PIZZA_CUBE));

    try (cleanups) {
      String output = execute("");
      assertThat(output, containsString("You do not have a Cold Medicine Cabinet installed."));
    }
  }

  @Test
  void errorsWithInvalidParameter() {
    var cleanups =
        new Cleanups(withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET), withContinuationState());

    try (cleanups) {
      var output = execute("beans");
      assertThat(output, containsString("not recognised"));
      assertErrorState();
    }
  }

  @Test
  void handlesAllConsultsUsed() {
    var cleanups =
        new Cleanups(
            withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
            withProperty("_coldMedicineConsults", 5));

    try (cleanups) {
      var output = execute("");

      assertThat(output, containsString("5/5 consults"));
      assertThat(output, not(containsString("turns until next consult")));
      assertThat(output, not(containsString("Consult is ready now")));
    }
  }

  @Nested
  class Guessing {
    @Nested
    class Equipment {
      @BeforeAll
      public static void beforeAll() {
        KoLCharacter.reset("ColdMedicineCabinetCommandTest");
      }

      @ParameterizedTest
      @CsvSource({"0, ice crown", "1, frozen jeans", "2, ice wrap", "3, ice wrap", "4, ice wrap"})
      void showsRightEquipmentForNumberTaken(int taken, String itemName) {
        var cleanups =
            new Cleanups(
                withProperty("_coldMedicineEquipmentTaken", taken),
                withProperty("_nextColdMedicineConsult", 1),
                withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
                withTurnsPlayed(0));
        try (cleanups) {
          String output = execute("");

          assertThat(output, containsString("Your next equipment should be " + itemName));
          assertContinueState();
        }
      }
    }

    @Nested
    class Booze {
      @BeforeAll
      public static void beforeAll() {
        KoLCharacter.reset("ColdMedicineCabinetCommandTest");
      }

      @ParameterizedTest
      @CsvSource({
        "100, 0, 0, Doc's Fortifying Wine",
        "0, 100, 0, Doc's Smartifying Wine",
        "0, 0, 100, Doc's Limbering Wine",
        "100, 0, 100, Doc's Special Reserve Wine",
        "100, 100, 100, Doc's Special Reserve Wine",
        "3, 3, 3, Doc's Medical-Grade Wine",
      })
      void showsRightWineForStatBuff(int mus, int mys, int mox, String itemName) {
        var cleanups =
            new Cleanups(
                withMuscle(1, mus),
                withMysticality(1, mys),
                withMoxie(1, mox),
                withProperty("_nextColdMedicineConsult", 1),
                withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
                withTurnsPlayed(0));

        try (cleanups) {
          String output = execute("");

          assertThat(output, containsString("Your next booze should be " + itemName));
          assertContinueState();
        }
      }

      @Test
      void guessesIfDueButInFight() {
        var cleanups =
            new Cleanups(
                withFight(),
                withProperty("_nextColdMedicineConsult", 0),
                withTurnsPlayed(1),
                withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET));
        try (cleanups) {
          String output = execute("");
          assertThat(output, containsString("Your next equipment should be"));
        }
      }

      @Test
      void guessesIfDueButInChoice() {
        var cleanups =
            new Cleanups(
                withHandlingChoice(),
                withProperty("_nextColdMedicineConsult", 0),
                withTurnsPlayed(1),
                withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET));
        try (cleanups) {
          String output = execute("");
          assertThat(output, containsString("Your next equipment should be"));
        }
      }
    }

    @Nested
    class Pill {
      @BeforeAll
      public static void beforeAll() {
        KoLCharacter.reset("ColdMedicineCabinetCommandTest");
      }

      @ParameterizedTest
      @CsvSource({
        "i, Extrovermectin&trade;",
        "o, Homebodyl&trade;",
        "u, Breathitin&trade;",
        "x, Fleshazole&trade;",
        "?, unknown"
      })
      void showsRightPillForRightMajority(String environment, String pill) {
        var cleanups =
            new Cleanups(
                withProperty("lastCombatEnvironments", environment.repeat(11) + "x".repeat(9)),
                withProperty("_nextColdMedicineConsult", 1),
                withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
                withTurnsPlayed(0));

        try (cleanups) {
          String output = execute("");

          var guess =
              "Your next pill " + (pill.equals("unknown") ? "is unknown" : "should be " + pill);
          assertThat(output, containsString(guess));
          assertContinueState();
        }
      }

      @Test
      void showsFleshazoleForNoOverallMajority() {
        var cleanups =
            new Cleanups(
                withProperty("lastCombatEnvironments", "iiiiiioooooouuuuuuio"),
                withProperty("_nextColdMedicineConsult", 1),
                withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
                withTurnsPlayed(0));

        try (cleanups) {
          String output = execute("");

          assertThat(output, containsString("Your next pill should be Fleshazole&trade;"));
          assertContinueState();
        }
      }
    }
  }

  @Nested
  class Checking {
    @BeforeEach
    public void beforeEach() {
      HttpClientWrapper.setupFakeClient();
    }

    @Test
    void canCheckCabinet() {
      var cleanups =
          new Cleanups(
              withNextResponse(200, html("request/test_choice_cmc_ice_wrap.html")),
              withProperty("_nextColdMedicineConsult", 0),
              withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
              withTurnsPlayed(1));

      try (cleanups) {
        String output = execute("");

        assertThat(output, containsString("Your next equipment is ice wrap\n"));
        assertThat(output, containsString("Your next food is frozen tofu pop\n"));
        assertThat(output, containsString("Your next booze is Doc's Fortifying Wine\n"));
        assertThat(output, containsString("Your next potion is anti-odor cream\n"));
        assertThat(output, containsString("Your next pill is Breathitin&trade;\n"));
      }
    }

    @Test
    void canHandleBogusCabinetResponse() {
      var cleanups =
          new Cleanups(
              withNextResponse(200, "unknown"),
              withProperty("_nextColdMedicineConsult", 0),
              withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
              withTurnsPlayed(1),
              withContinuationState());

      try (cleanups) {
        String output = execute("");

        assertThat(output, containsString("Cold Medicine Cabinet choice could not be parsed.\n"));
        assertErrorState();
      }
    }
  }

  @Nested
  class Collecting {
    @BeforeAll
    public static void beforeAll() {
      KoLCharacter.reset("ColdMedicineCabinetCommandTest");
    }

    @BeforeEach
    public void beforeEach() {
      HttpClientWrapper.setupFakeClient();
    }

    @Test
    void cannotCollectIfNoMoreConsults() {
      var cleanups =
          new Cleanups(
              withProperty("_nextColdMedicineConsult", 0),
              withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
              withTurnsPlayed(1),
              withProperty("_coldMedicineConsults", 5),
              withContinuationState());

      try (cleanups) {
        var output = execute("food");

        assertThat(output, containsString("You do not have any consults"));
        assertErrorState();
      }
    }

    @Test
    void cannotCollectIfNoConsultReady() {
      var cleanups =
          new Cleanups(
              withProperty("_nextColdMedicineConsult", 5),
              withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
              withTurnsPlayed(0),
              withProperty("_coldMedicineConsults", 2),
              withContinuationState());

      try (cleanups) {
        var output = execute("equipment");

        assertThat(output, containsString("You are not due a consult (5 turns to go)."));
        assertErrorState();
      }
    }

    @ParameterizedTest
    @CsvSource({
      "equip, 1",
      "equipment, 1",
      "food, 2",
      "booze, 3",
      "wine, 3",
      "potion, 4",
      "pill, 5"
    })
    void canCollectItem(String command, int decision) {
      var cleanups =
          new Cleanups(
              withProperty("_nextColdMedicineConsult", 0),
              withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
              withTurnsPlayed(5),
              withProperty("_coldMedicineConsults", 2),
              withContinuationState(),
              withHandlingChoice(false));

      try (cleanups) {
        execute(command);

        var requests = getRequests();

        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/campground.php", "action=workshed");
        assertPostRequest(requests.get(1), "/choice.php", "whichchoice=0&option=" + decision);
        assertContinueState();
      }
    }
  }

  @Nested
  class PillsParamTests {
    @BeforeAll
    public static void beforeAll() {
      KoLCharacter.reset("ColdMedicineCabinetCommandTest");
    }

    @Test
    void GuessNextPillsWithNoUnknownOrUnderwaterEnvironments() {
      var cleanups =
          new Cleanups(
              withNextResponse(200, html("request/test_choice_cmc_ice_wrap.html")),
              withProperty("lastCombatEnvironments", "iiiiiioooooouuuuuuio"),
              withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
              withTurnsPlayed(0),
              withContinuationState());

      try (cleanups) {
        String output = execute("plan");
        assertThat(
            output,
            containsString(
                "For Fleshazole&trade;, spend a minimum of 11 combats underwater or until no environment has overall majority\n"));
        assertThat(
            output,
            containsString("For Homebodyl&trade;, spend 4 combats in an outdoor location\n"));
        assertThat(
            output,
            containsString("For Breathitin&trade;, spend 5 combats in an underground location\n"));
        assertThat(
            output,
            containsString("For Extrovermectin&trade;, spend 10 combats in an indoor location"));
        assertContinueState();
      }
    }

    @Test
    void GuessNextPillsWithUnknownEnvironments() {
      var cleanups =
          new Cleanups(
              withNextResponse(200, html("request/test_choice_cmc_ice_wrap.html")),
              withProperty("lastCombatEnvironments", "???????uuuuuuuuooooo"),
              withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
              withTurnsPlayed(0),
              withContinuationState());

      try (cleanups) {
        String output = execute("plan");
        assertThat(
            output,
            containsString(
                "For Fleshazole&trade;, spend a minimum of 11 combats underwater or until no environment has overall majority\n"));
        assertThat(
            output,
            containsString(
                "For Extrovermectin&trade;, spend a minimum of 11 combats in an indoor location\n"));
        assertThat(
            output,
            containsString("For Homebodyl&trade;, spend 6 combats in an outdoor location\n"));
        assertThat(
            output,
            containsString("For Breathitin&trade;, spend 3 combats in an underground location"));
        assertContinueState();
      }
    }

    @Test
    void GuessNextPillsWithAllTypesOfEnvironments() {
      var cleanups =
          new Cleanups(
              withNextResponse(200, html("request/test_choice_cmc_ice_wrap.html")),
              withProperty("lastCombatEnvironments", "xxx???ii?ouuuuooiixx"),
              withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
              withTurnsPlayed(0),
              withContinuationState());

      try (cleanups) {
        String output = execute("plan");
        assertThat(
            output,
            containsString("For Breathitin&trade;, spend 7 combats in an underground location\n"));
        assertThat(
            output,
            containsString("For Homebodyl&trade;, spend 8 combats in an outdoor location\n"));
        assertThat(
            output,
            containsString("For Extrovermectin&trade;, spend 9 combats in an indoor location\n"));
        assertThat(
            output,
            containsString(
                "For Fleshazole&trade;, spend 9 combats underwater or until no environment has overall majority"));
        assertContinueState();
      }
    }

    @Test
    void GuessNextPillsWithExistingMajority() {
      var cleanups =
          new Cleanups(
              withNextResponse(200, html("request/test_choice_cmc_ice_wrap.html")),
              withProperty("lastCombatEnvironments", "uuuuuuuuuuuiioxiiuo?"),
              withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
              withTurnsPlayed(0),
              withContinuationState());

      try (cleanups) {
        String output = execute("plan");
        assertThat(
            output,
            containsString("For Breathitin&trade;, spend 0 combats in an underground location\n"));
        assertThat(
            output,
            containsString("For Homebodyl&trade;, spend 9 combats in an outdoor location\n"));
        assertThat(
            output,
            containsString("For Extrovermectin&trade;, spend 7 combats in an indoor location\n"));
        assertThat(
            output,
            containsString(
                "For Fleshazole&trade;, spend 10 combats underwater or until no environment has overall majority"));
        assertContinueState();
      }
    }
  }
}
